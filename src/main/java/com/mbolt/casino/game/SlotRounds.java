package com.mbolt.casino.game;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.CasinoText;
import com.mbolt.casino.advancement.CasinoEvent;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.block.entity.VaultLink;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.slot.SlotMachineLogic;
import com.mbolt.casino.game.slot.SlotSymbol;
import net.minecraft.core.BlockPos;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Slot rounds owned by the world rather than by the machine, so that a round chosen by the server is settled
 * exactly once even if the player logs out, the machine is broken or its chunk unloads.
 */
public final class SlotRounds extends SavedData {
    private static final String FILE = CasinoMod.MODID + "_slot_rounds";
    private static final Factory<SlotRounds> FACTORY = new Factory<>(SlotRounds::new, SlotRounds::load);
    private static final double BIG_WIN_RADIUS_SQUARED = 64 * 64;

    private final Map<UUID, Round> rounds = new LinkedHashMap<>();

    public static SlotRounds get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE);
    }

    public SlotMachineLogic.Result choose(long bet, SlotMachineLogic logic) {
        return logic.spin(bet, CasinoRandom::nextInt);
    }

    public void start(Round round) {
        if (rounds.putIfAbsent(round.id(), round) != null) {
            throw new IllegalStateException("Duplicate slot round " + round.id());
        }
        setDirty();
    }

    public boolean isActive(UUID round) {
        Round found = rounds.get(round);
        return found != null && found.state == State.SPINNING;
    }

    /** The machine was broken: the bet goes back to the player at the next settlement. */
    public void cancel(@Nullable UUID round) {
        Round found = round == null ? null : rounds.get(round);
        if (found != null && found.state == State.SPINNING) {
            found.state = State.CANCELLED;
            setDirty();
        }
    }

    /** The vault itself is going away: refund and settle all its rounds right now, while it still exists. */
    public void vaultRemoved(MinecraftServer server, VaultBlockEntity vault) {
        for (Round round : new ArrayList<>(rounds.values())) {
            if (round.vault().vaultId().equals(vault.identity())) {
                round.state = State.CANCELLED;
                settle(server, round, vault);
            }
        }
    }

    /** Called every server tick: settles finished rounds whose vault chunk is loaded. */
    public void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Round round : new ArrayList<>(rounds.values())) {
            if (round.state == State.SPINNING && now >= round.endsAt()) {
                round.state = State.FINISHED;
                setDirty();
            }
            if (round.state == State.SPINNING) {
                continue;
            }
            ServerLevel level = server.getLevel(round.vault().pos().dimension());
            if (level == null || !isChunkLoaded(level, round.vault().pos().pos())) {
                continue;
            }
            settle(server, round, round.vault().resolve(level).orElse(null));
        }
    }

    /** Pays the result or refunds the bet; a missing vault can only refund. */
    private void settle(MinecraftServer server, Round round, @Nullable VaultBlockEntity vault) {
        long time = System.currentTimeMillis();
        boolean refund = round.state == State.CANCELLED || vault == null;
        long chips;
        if (refund) {
            if (vault != null) {
                vault.ledger().cancelSlot(round.id(), round.player(), round.bet(), time);
            }
            chips = round.bet();
        } else {
            vault.ledger().finishSlot(round.id(), round.player(), time);
            chips = round.result().payout();
        }
        if (vault != null) {
            vault.ledgerChanged();
        }
        rounds.remove(round.id());
        setDirty();
        PendingPayouts.get(server).add(round.id(), round.player(), chips);

        SlotMachineBlockEntity machine = loadedMachine(server, round.machine());
        if (machine != null) {
            machine.roundFinished(round.id(), refund);
        }
        if (!refund) {
            announce(server, round);
            ServerPlayer winner = server.getPlayerList().getPlayer(round.player());
            if (isJackpot(round.result())) {
                CasinoEvent.JACKPOT.award(winner);
            }
            if (chips >= CasinoEvent.HIGH_ROLLER_CHIPS) {
                CasinoEvent.HIGH_ROLLER.award(winner);
            }
        }
    }

    private static void announce(MinecraftServer server, Round round) {
        long payout = round.result().payout();
        boolean jackpot = isJackpot(round.result());
        if (!jackpot && payout < CasinoServerConfig.ANNOUNCE_BIG_WIN.get()) {
            return;
        }
        ServerPlayer winner = server.getPlayerList().getPlayer(round.player());
        Component name = winner != null ? winner.getDisplayName() : Component.literal(round.playerName());
        Component text = CasinoText.message(jackpot ? "jackpot" : "big_win", name, CasinoText.chips(payout));
        BlockPos machinePos = round.machine().pos();
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            boolean near = viewer.level().dimension().equals(round.machine().dimension())
                    && viewer.distanceToSqr(machinePos.getCenter()) <= BIG_WIN_RADIUS_SQUARED;
            if (jackpot || near) {
                viewer.sendSystemMessage(text);
            }
        }
    }

    public static boolean isJackpot(SlotMachineLogic.Result result) {
        return isJackpot(result.first(), result.second(), result.third());
    }

    public static boolean isJackpot(int first, int second, int third) {
        int seven = SlotSymbol.SEVEN.ordinal();
        return first == seven && second == seven && third == seven;
    }

    @Nullable
    private static SlotMachineBlockEntity loadedMachine(MinecraftServer server, GlobalPos pos) {
        ServerLevel level = server.getLevel(pos.dimension());
        if (level == null || !isChunkLoaded(level, pos.pos())) {
            return null;
        }
        return level.getBlockEntity(pos.pos()) instanceof SlotMachineBlockEntity machine ? machine : null;
    }

    private static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag rows = new ListTag();
        for (Round round : rounds.values()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("Id", round.id());
            row.putUUID("Player", round.player());
            row.putString("PlayerName", round.playerName());
            row.put("Machine", GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, round.machine()).getOrThrow());
            row.put("Vault", round.vault().save());
            row.putLong("Bet", round.bet());
            row.putLong("EndsAt", round.endsAt());
            SlotMachineLogic.Result result = round.result();
            row.putIntArray("Symbols", new int[]{result.first(), result.second(), result.third()});
            row.putLong("Payout", result.payout());
            row.putString("State", round.state.name());
            rows.add(row);
        }
        tag.put("Rounds", rows);
        return tag;
    }

    private static SlotRounds load(CompoundTag tag, HolderLookup.Provider registries) {
        SlotRounds data = new SlotRounds();
        for (Tag value : tag.getList("Rounds", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value;
            int[] symbols = row.getIntArray("Symbols");
            var machine = GlobalPos.CODEC.parse(NbtOps.INSTANCE, row.get("Machine")).result();
            var vault = VaultLink.load(row.getCompound("Vault"));
            if (symbols.length != 3 || machine.isEmpty() || vault.isEmpty()) {
                CasinoMod.LOGGER.error("Skipping unreadable slot round {}", row);
                continue;
            }
            var result = new SlotMachineLogic.Result(symbols[0], symbols[1], symbols[2], row.getLong("Payout"));
            Round round = new Round(row.getUUID("Id"), row.getUUID("Player"), row.getString("PlayerName"),
                    machine.get(), vault.get(), row.getLong("Bet"), row.getLong("EndsAt"), result);
            round.state = State.valueOf(row.getString("State"));
            data.rounds.put(round.id(), round);
        }
        return data;
    }

    private enum State {
        SPINNING,
        FINISHED,
        CANCELLED
    }

    public static final class Round {
        private final UUID id;
        private final UUID player;
        private final String playerName;
        private final GlobalPos machine;
        private final VaultLink vault;
        private final long bet;
        private final long endsAt;
        private final SlotMachineLogic.Result result;
        private State state = State.SPINNING;

        public Round(UUID id, UUID player, String playerName, GlobalPos machine, VaultLink vault, long bet,
                     long endsAt, SlotMachineLogic.Result result) {
            this.id = id;
            this.player = player;
            this.playerName = playerName;
            this.machine = machine;
            this.vault = vault;
            this.bet = bet;
            this.endsAt = endsAt;
            this.result = result;
        }

        public UUID id() {
            return id;
        }

        public UUID player() {
            return player;
        }

        public String playerName() {
            return playerName;
        }

        public GlobalPos machine() {
            return machine;
        }

        public VaultLink vault() {
            return vault;
        }

        public long bet() {
            return bet;
        }

        public long endsAt() {
            return endsAt;
        }

        public SlotMachineLogic.Result result() {
            return result;
        }
    }
}
