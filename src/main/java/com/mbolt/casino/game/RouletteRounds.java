package com.mbolt.casino.game;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.CasinoText;
import com.mbolt.casino.advancement.CasinoEvent;
import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.block.entity.VaultLink;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.roulette.RouletteBet;
import com.mbolt.casino.game.roulette.RouletteLogic;
import com.mbolt.casino.game.roulette.RouletteRound;
import com.mbolt.casino.inventory.ChipWallet;
import com.mbolt.casino.menu.PlayerNames;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.util.Objects;

/** Authoritative rounds outlive players, table chunks and table removal. All calls run on the server thread. */
public final class RouletteRounds extends SavedData {
    public static final int SPIN_TICKS = 160;
    public static final int PAYOUT_TICKS = 60;
    private static final Factory<RouletteRounds> FACTORY = new Factory<>(RouletteRounds::new, RouletteRounds::load);
    private final Map<UUID, Entry> rounds = new LinkedHashMap<>();
    private final List<CompoundTag> quarantined = new ArrayList<>();

    public static RouletteRounds get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, CasinoMod.MODID + "_roulette_rounds");
    }

    @Nullable
    public Component place(RouletteTableBlockEntity table, ServerPlayer player, RouletteBet bet) {
        if (!CasinoServerConfig.ENABLED.get()) {
            return CasinoText.message("games_disabled");
        }
        if (!table.hasOwner()) {
            return CasinoText.message("game_unclaimed");
        }
        var bank = table.linkedVault();
        if (bank.isEmpty()) {
            return CasinoText.message(table.isLinked() ? "vault_unavailable" : "not_linked");
        }
        MinecraftServer server = player.server;
        Entry entry = table.roundId() == null ? null : rounds.get(table.roundId());
        if (entry != null && (entry.round().snapshot().phase() == RouletteRound.Phase.COMPLETE
                || entry.round().snapshot().phase() == RouletteRound.Phase.CANCELLED)) {
            entry = null;
        }
        if (entry == null) {
            var round = new RouletteRound(UUID.randomUUID(), CasinoServerConfig.ROULETTE_BETTING_SECONDS.get() * 20,
                    SPIN_TICKS, PAYOUT_TICKS);
            entry = new Entry(GlobalPos.of(table.getLevel().dimension(), table.getBlockPos()),
                    VaultLink.of(bank.get()), round);
        }
        var round = entry.round();
        var placed = new RouletteRound.PlacedBet(UUID.randomUUID(), player.getUUID(), bet);
        long now = server.overworld().getGameTime();
        var ledger = bank.get().ledger();
        var rates = CasinoServerConfig.rates();
        long balance = ChipWallet.balance(player);
        long freeReserve = ledger.freeReserve(rates);
        int maxBet = CasinoServerConfig.ROULETTE_MAX_BET.get();
        int maxPayout = CasinoServerConfig.ROULETTE_MAX_ROUND_PAYOUT.get();
        var admission = round.preview(placed, now, balance, freeReserve, maxBet, maxPayout);
        if (!admission.accepted()) {
            return CasinoText.message("roulette." + admission.refusal().name().toLowerCase(Locale.ROOT));
        }
        // Admission is pure; no mutation precedes it. These operations cannot race on the server thread.
        ledger.bookRoulette(round.snapshot().id(), bet.chips(), admission.exposure(), rates, player.getUUID(),
                System.currentTimeMillis());
        ChipWallet.take(player, bet.chips());
        round.place(placed, now, balance, freeReserve, maxBet, maxPayout);
        bank.get().ledgerChanged();
        rounds.put(round.snapshot().id(), entry);
        setDirty();
        table.applyRound(round);
        CasinoEvent.ROULETTE_BET.award(player);
        return null;
    }

    @Nullable
    public RouletteRound find(@Nullable UUID id) {
        Entry entry = id == null ? null : rounds.get(id);
        return entry == null ? null : entry.round();
    }

    public void cancel(@Nullable UUID id) {
        var round = find(id);
        if (round != null && !round.snapshot().settled()) {
            // Leave the bets intact until their vault can be reached and the refunds booked.
            rounds.get(id).cancelled = true;
            setDirty();
        }
    }

    public void vaultRemoved(MinecraftServer server, VaultBlockEntity vault) {
        for (Entry entry : List.copyOf(rounds.values())) {
            if (entry.vault().vaultId().equals(vault.identity()) && !entry.round().snapshot().settled()) {
                entry.cancelled = true;
                settle(server, entry, vault);
            }
        }
    }

    public void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Entry entry : List.copyOf(rounds.values())) {
            var round = entry.round();
            boolean changed = !entry.cancelled && round.advance(now, CasinoRandom::nextInt);
            if (changed) {
                setDirty();
            }
            var level = server.getLevel(entry.vault().pos().dimension());
            if (level != null && loaded(level, entry.vault().pos())) {
                var vault = entry.vault().resolve(level).orElse(null);
                if (vault == null) {
                    entry.cancelled = true;
                }
                if (entry.cancelled || round.snapshot().phase() == RouletteRound.Phase.PAYOUT) {
                    settle(server, entry, vault);
                }
            }
            RouletteTableBlockEntity table = table(server, entry.table());
            if (table != null && (changed || entry.cancelled)
                    && Objects.equals(table.roundId(), round.snapshot().id())) {
                table.applyRound(round);
            }
            var phase = round.snapshot().phase();
            if (phase == RouletteRound.Phase.COMPLETE || phase == RouletteRound.Phase.CANCELLED) {
                if (table != null && Objects.equals(table.roundId(), round.snapshot().id())) {
                    table.applyRound(round);
                }
                rounds.remove(round.snapshot().id());
                setDirty();
            }
        }
    }

    private void settle(MinecraftServer server, Entry entry, @Nullable VaultBlockEntity vault) {
        var round = entry.round();
        if (round.snapshot().settled() || round.snapshot().phase() == RouletteRound.Phase.CANCELLED) {
            return;
        }
        Map<UUID, Long> chips = entry.cancelled ? round.refunds() : round.payouts();
        boolean booked = vault == null || (entry.cancelled
                ? vault.ledger().cancelRoulette(round.snapshot().id(), chips, System.currentTimeMillis())
                : vault.ledger().finishRoulette(round.snapshot().id(), chips, System.currentTimeMillis()));
        if (!booked) {
            return;
        }
        if (vault != null) {
            vault.ledgerChanged();
        }
        chips.forEach((player, amount) -> {
            String key = round.snapshot().id() + ":" + player;
            UUID delivery = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
            PendingPayouts.get(server).add(delivery, player, amount);
            if (!entry.cancelled && amount >= CasinoServerConfig.ANNOUNCE_BIG_WIN.get()) {
                announce(server, entry.table(), player, amount);
            }
        });
        if (entry.cancelled) {
            round.cancel();
        } else {
            awardWins(server, round.snapshot(), chips);
            round.settle();
        }
        setDirty();
        var table = table(server, entry.table());
        if (table != null && Objects.equals(table.roundId(), round.snapshot().id())) {
            table.applyRound(round);
        }
    }

    /** Straight-up and zero wins, and big payouts, for the advancements. */
    private static void awardWins(MinecraftServer server, RouletteRound.Snapshot round, Map<UUID, Long> payouts) {
        for (RouletteRound.PlacedBet placed : round.bets()) {
            RouletteBet bet = placed.bet();
            if (bet.type() == RouletteBet.Type.NUMBER && bet.selection() == round.number()) {
                ServerPlayer winner = server.getPlayerList().getPlayer(placed.player());
                CasinoEvent.STRAIGHT_UP.award(winner);
                if (round.number() == 0) {
                    CasinoEvent.ZERO.award(winner);
                }
            }
        }
        payouts.forEach((player, amount) -> {
            if (amount >= CasinoEvent.HIGH_ROLLER_CHIPS) {
                CasinoEvent.HIGH_ROLLER.award(server.getPlayerList().getPlayer(player));
            }
        });
    }

    private static void announce(MinecraftServer server, GlobalPos table, UUID player, long amount) {
        var winner = server.getPlayerList().getPlayer(player);
        Component name = winner == null ? Component.literal(PlayerNames.of(server, player)) : winner.getDisplayName();
        Component text = CasinoText.message("roulette.big_win", name, CasinoText.chips(amount));
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer.level().dimension().equals(table.dimension())
                    && viewer.distanceToSqr(table.pos().getCenter()) <= 64 * 64) {
                viewer.sendSystemMessage(text);
            }
        }
    }

    private static boolean loaded(ServerLevel level, GlobalPos pos) {
        return level.getChunkSource().getChunkNow(pos.pos().getX() >> 4, pos.pos().getZ() >> 4) != null;
    }

    @Nullable
    private static RouletteTableBlockEntity table(MinecraftServer server, GlobalPos pos) {
        var level = server.getLevel(pos.dimension());
        return level != null && loaded(level, pos)
                && level.getBlockEntity(pos.pos()) instanceof RouletteTableBlockEntity table ? table : null;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var rows = new ListTag();
        for (Entry entry : rounds.values()) {
            var row = new CompoundTag();
            row.put("Table", GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, entry.table()).getOrThrow());
            row.put("Vault", entry.vault().save());
            row.put("Round", RouletteRoundNbt.save(entry.round()));
            row.putBoolean("Cancelled", entry.cancelled);
            rows.add(row);
        }
        quarantined.forEach(rows::add);
        tag.put("Rounds", rows);
        return tag;
    }

    public static RouletteRounds load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new RouletteRounds();
        for (Tag value : tag.getList("Rounds", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            try {
                var table = GlobalPos.CODEC.parse(NbtOps.INSTANCE, row.get("Table")).getOrThrow();
                var vault = VaultLink.load(row.getCompound("Vault")).orElseThrow();
                var round = RouletteRoundNbt.load(row.getCompound("Round"));
                var entry = new Entry(table, vault, round);
                entry.cancelled = row.getBoolean("Cancelled");
                data.rounds.put(round.snapshot().id(), entry);
            } catch (RuntimeException error) {
                data.quarantined.add(row.copy());
                CasinoMod.LOGGER.error("Quarantined an unreadable roulette round; its original data is retained",
                        error);
            }
        }
        return data;
    }

    private static final class Entry {
        private final GlobalPos table;
        private final VaultLink vault;
        private final RouletteRound round;
        private boolean cancelled;

        Entry(GlobalPos table, VaultLink vault, RouletteRound round) {
            this.table = table;
            this.vault = vault;
            this.round = round;
        }

        GlobalPos table() { return table; }
        VaultLink vault() { return vault; }
        RouletteRound round() { return round; }
    }
}
