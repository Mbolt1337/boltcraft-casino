package com.mbolt.casino.block.entity;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.advancement.CasinoEvent;
import com.mbolt.casino.client.ClientSlotEffects;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.SlotRounds;
import com.mbolt.casino.game.slot.SlotMachineLogic;
import com.mbolt.casino.game.slot.SlotSymbol;
import com.mbolt.casino.inventory.ChipWallet;
import com.mbolt.casino.network.SlotSpinResult;
import com.mbolt.casino.registry.ModBlockEntities;
import com.mbolt.casino.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * One slot machine. The server picks the result before anything moves; clients spin the reels so that they stop
 * on exactly those symbols.
 */
public final class SlotMachineBlockEntity extends LinkedBlockEntity implements GeoBlockEntity {
    public static final int REELS = 3;
    /** Share of the spin after which the first reel stops; each next reel stops {@link #REEL_STOP_STEP} later. */
    public static final double FIRST_REEL_STOP = 0.65;
    public static final double REEL_STOP_STEP = 0.15;
    private static final int MINIMUM_TURNS = 4;
    private static final double SYNC_RADIUS = 32;
    private static final int TICKS_PER_SECOND = 20;

    private static final String CONTROLLER = "machine";
    private static final RawAnimation LEVER = RawAnimation.begin().thenPlay("animation.slot_machine.spin");
    private static final RawAnimation WIN = RawAnimation.begin().thenPlay("animation.slot_machine.win");
    private static final RawAnimation JACKPOT = RawAnimation.begin().thenPlay("animation.slot_machine.jackpot");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final int[] symbols = new int[REELS];
    @Nullable
    private UUID roundId;
    private boolean spinning;
    private long payout;
    private long startedAt;
    private long endsAt;
    private long cooldownUntil;

    /** Client only: the spin whose sounds have already been started, and how many reels have clicked to a stop. */
    private long soundedSpin = Long.MIN_VALUE;
    private int stoppedReels;
    /** Client only: the spin whose winnings have been poured out, and ticks into the pour (-1 when none runs). */
    private long pouredSpin = Long.MIN_VALUE;
    private int pourAge = -1;

    public SlotMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLOT_MACHINE.get(), pos, state);
    }

    public boolean spinning() {
        return spinning;
    }

    @Nullable
    public UUID roundId() {
        return roundId;
    }

    public long payout() {
        return payout;
    }

    public int symbol(int reel) {
        return symbols[reel];
    }

    public long startedAt() {
        return startedAt;
    }

    public long endsAt() {
        return endsAt;
    }

    @Override
    public boolean canRelink() {
        return !spinning;
    }

    /** Starts a spin for the player, or returns why it cannot start. Every check precedes every change. */
    @Nullable
    public Component spin(ServerPlayer player, int bet) {
        if (!CasinoServerConfig.ENABLED.get()) {
            return CasinoText.message("games_disabled");
        }
        if (!CasinoServerConfig.slotBets().contains(bet)) {
            return CasinoText.message("invalid_bet");
        }
        if (!hasOwner()) {
            return CasinoText.message("game_unclaimed");
        }
        if (spinning) {
            return CasinoText.message("slot_busy");
        }
        long now = level.getGameTime();
        if (now < cooldownUntil) {
            long seconds = (cooldownUntil - now + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
            return CasinoText.message("slot_cooldown", seconds);
        }
        var vault = linkedVault();
        if (vault.isEmpty()) {
            return CasinoText.message(isLinked() ? "vault_unavailable" : "not_linked");
        }
        long balance = ChipWallet.balance(player);
        if (balance < bet) {
            return CasinoText.message("not_enough_chips", CasinoText.chips(bet - balance));
        }
        SlotMachineLogic logic = CasinoServerConfig.slotLogic();
        var rates = CasinoServerConfig.rates();
        long maximumPayout = Math.multiplyExact((long) bet, logic.maximumMultiplier());
        if (vault.get().ledger().freeReserve(rates) < maximumPayout) {
            return CasinoText.message("insufficient_reserve", CasinoText.chips(maximumPayout));
        }

        SlotRounds rounds = SlotRounds.get(player.server);
        SlotMachineLogic.Result result = rounds.choose(bet, logic);
        UUID id = UUID.randomUUID();
        vault.get().ledger().bookSlot(id, bet, maximumPayout, result.payout(), rates, player.getUUID(),
                System.currentTimeMillis());
        vault.get().ledgerChanged();
        ChipWallet.take(player, bet);

        int duration = CasinoServerConfig.SLOT_SPIN_DURATION_TICKS.get();
        roundId = id;
        symbols[0] = result.first();
        symbols[1] = result.second();
        symbols[2] = result.third();
        payout = result.payout();
        startedAt = now;
        endsAt = now + duration;
        cooldownUntil = now + Math.max(duration, CasinoServerConfig.SLOT_COOLDOWN_TICKS.get());
        spinning = true;
        rounds.start(new SlotRounds.Round(id, player.getUUID(), player.getGameProfile().getName(),
                GlobalPos.of(level.dimension(), worldPosition), VaultLink.of(vault.get()), bet,
                player.server.overworld().getGameTime() + duration, result));

        syncToClients();
        triggerAnim(CONTROLLER, "lever");
        level.playSound(null, worldPosition, ModSounds.LEVER_PULL.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
        var center = worldPosition.getCenter();
        PacketDistributor.sendToPlayersNear((ServerLevel) level, null, center.x, center.y, center.z, SYNC_RADIUS,
                new SlotSpinResult(worldPosition, symbols[0], symbols[1], symbols[2], payout, startedAt, endsAt));
        CasinoEvent.FIRST_SPIN.award(player);
        return null;
    }

    /** Called by {@link SlotRounds} once the round is settled; plays the win effects. */
    public void roundFinished(UUID round, boolean refunded) {
        if (!round.equals(roundId)) {
            return;
        }
        spinning = false;
        if (refunded) {
            payout = 0;
        }
        syncToClients();
        if (payout <= 0) {
            return;
        }
        boolean jackpot = SlotRounds.isJackpot(symbols[0], symbols[1], symbols[2]);
        triggerAnim(CONTROLLER, jackpot ? "jackpot" : "win");
        SoundEvent sound;
        if (jackpot) {
            sound = ModSounds.JACKPOT.get();
        } else if (payout >= CasinoServerConfig.ANNOUNCE_BIG_WIN.get()) {
            sound = ModSounds.WIN_BIG.get();
        } else {
            sound = ModSounds.WIN_SMALL.get();
        }
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    /** Client: applies a spin announced by the server. */
    public void applySpin(SlotSpinResult result) {
        int[] chosen = result.symbols();
        System.arraycopy(chosen, 0, symbols, 0, REELS);
        payout = result.payout();
        startedAt = result.startedAt();
        endsAt = result.endsAt();
        spinning = true;
    }

    /**
     * Reel rotation in radians: several fast turns that ease out exactly onto the chosen symbol. Reels stop one
     * after another; a stopped reel shows its symbol facing the player.
     */
    public double reelAngle(int reel, float partialTick) {
        double target = symbols[reel] * (2 * Math.PI / SlotSymbol.values().length);
        if (!spinning || level == null) {
            return target;
        }
        double elapsed = level.getGameTime() + partialTick - startedAt;
        double stopAt = Math.max(1, endsAt - startedAt) * (FIRST_REEL_STOP + reel * REEL_STOP_STEP);
        if (elapsed >= stopAt) {
            return target;
        }
        double progress = Math.clamp(elapsed / stopAt, 0, 1);
        double eased = 1 - Math.pow(1 - progress, 3);
        double turns = (MINIMUM_TURNS + reel) * 2 * Math.PI;
        return (turns + target) * eased;
    }

    /** Share of the spin that has passed, 0 to 1; 1 when idle. */
    public double spinProgress(float partialTick) {
        if (!spinning || level == null) {
            return 1;
        }
        double elapsed = level.getGameTime() + partialTick - startedAt;
        return Math.clamp(elapsed / Math.max(1, endsAt - startedAt), 0, 1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SlotMachineBlockEntity machine) {
        // Fallback for a round settled while this machine was unloaded: stop showing the spin.
        boolean stale = machine.spinning && machine.roundId != null && level.getGameTime() >= machine.endsAt
                && !SlotRounds.get(((ServerLevel) level).getServer()).isActive(machine.roundId);
        if (stale) {
            machine.spinning = false;
            machine.syncToClients();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SlotMachineBlockEntity machine) {
        if (machine.spinning) {
            machine.tickSpinEffects();
        } else if (machine.payout > 0 && machine.soundedSpin == machine.startedAt
                && machine.pouredSpin != machine.startedAt) {
            // A spin this client watched has just been settled with a win.
            machine.pouredSpin = machine.startedAt;
            machine.pourAge = 0;
        }
        if (machine.pourAge >= 0) {
            boolean jackpot = SlotRounds.isJackpot(machine.symbols[0], machine.symbols[1], machine.symbols[2]);
            boolean pouring = ClientSlotEffects.pourWinnings(machine, machine.pourAge++, jackpot);
            if (!pouring) {
                machine.pourAge = -1;
            }
        }
    }

    private void tickSpinEffects() {
        if (soundedSpin != startedAt) {
            soundedSpin = startedAt;
            stoppedReels = 0;
            ClientSlotEffects.startReelSound(this);
        }
        double progress = spinProgress(0);
        while (stoppedReels < REELS && progress >= FIRST_REEL_STOP + stoppedReels * REEL_STOP_STEP) {
            stoppedReels++;
            ClientSlotEffects.reelStopped(this, stoppedReels);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim("lever", LEVER)
                .triggerableAnim("win", WIN)
                .triggerableAnim("jackpot", JACKPOT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void writeClientState(CompoundTag tag) {
        tag.putBoolean("Spinning", spinning);
        tag.putIntArray("Symbols", symbols.clone());
        tag.putLong("Payout", payout);
        tag.putLong("StartedAt", startedAt);
        tag.putLong("EndsAt", endsAt);
    }

    @Override
    protected void readClientState(CompoundTag tag) {
        spinning = tag.getBoolean("Spinning");
        int[] saved = tag.getIntArray("Symbols");
        if (saved.length == REELS) {
            System.arraycopy(saved, 0, symbols, 0, REELS);
        }
        payout = tag.getLong("Payout");
        startedAt = tag.getLong("StartedAt");
        endsAt = tag.getLong("EndsAt");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeClientState(tag);
        tag.putLong("CooldownUntil", cooldownUntil);
        if (roundId != null) {
            tag.putUUID("Round", roundId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readClientState(tag);
        cooldownUntil = tag.getLong("CooldownUntil");
        roundId = tag.hasUUID("Round") ? tag.getUUID("Round") : null;
    }
}
