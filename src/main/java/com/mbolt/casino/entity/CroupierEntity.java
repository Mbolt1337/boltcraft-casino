package com.mbolt.casino.entity;

import com.mbolt.casino.block.CasinoBlock;
import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.game.RouletteRounds;
import com.mbolt.casino.game.roulette.RouletteLogic;
import com.mbolt.casino.game.roulette.RouletteRound;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Locale;

/**
 * A table attendant with the appearance of a villager. No trades, breeding, loot or wandering AI. He calls the round
 * like a real croupier: the server picks his line and every client shows it in a speech bubble over his head.
 */
public final class CroupierEntity extends PathfinderMob implements GeoEntity {
    /** What the croupier is saying, as a line id (see {@link Line}); empty when silent. */
    private static final EntityDataAccessor<String> LINE =
            SynchedEntityData.defineId(CroupierEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Ticks into the spin at which the croupier waves "no more bets", while the ball still runs. */
    private static final int NO_MORE_BETS_TICKS = 40;
    /** Ticks before betting closes at which he calls the last bets, when the betting time is long enough. */
    private static final int LAST_BETS_TICKS = 100;
    /** A player coming this close to an idle table is greeted, once until they leave again. */
    private static final double GREETING_DISTANCE = 5;
    private static final int GREETING_CHECK_TICKS = 20;

    /** The croupier's lines. {@code result} carries the winning number after a colon. */
    public enum Line {
        WELCOME, BETS_OPEN, LAST_BETS, NO_MORE_BETS, RESULT;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private BlockPos tablePos = BlockPos.ZERO;
    private RouletteRound.Phase lastPhase = RouletteRound.Phase.IDLE;
    private boolean waved;
    private boolean calledLastBets;
    private boolean playerNear;
    /** Client only: the tick at which the current line arrived, so the bubble can fade after a while. */
    private int lineStartedAt;

    public CroupierEntity(EntityType<? extends CroupierEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LINE, "");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (LINE.equals(key)) {
            lineStartedAt = tickCount;
        }
    }

    /** The line being said, for the bubble: its id and, for a result, the number. */
    public String line() {
        return entityData.get(LINE);
    }

    public int lineAge() {
        return tickCount - lineStartedAt;
    }

    private void say(Line line, String argument) {
        String value = argument.isEmpty() ? line.id() : line.id() + ":" + argument;
        // The same line again (a second round's "bets open") must still show, so it is cleared first.
        entityData.set(LINE, "");
        entityData.set(LINE, value);
        playSound(SoundEvents.VILLAGER_AMBIENT, 0.6f, 0.9f + random.nextFloat() * 0.2f);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0);
    }

    public void bind(RouletteTableBlockEntity table) {
        tablePos = table.getBlockPos();
        var pos = table.croupierPosition();
        moveTo(pos.x, pos.y, pos.z, table.getBlockState().getValue(CasinoBlock.FACING).toYRot(), 0);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)
                || server.getChunkSource().getChunkNow(tablePos.getX() >> 4, tablePos.getZ() >> 4) == null) {
            return;
        }
        if (!(server.getBlockEntity(tablePos) instanceof RouletteTableBlockEntity table)
                || !table.isCroupier(getUUID())) {
            discard();
            return;
        }
        bind(table);
        RouletteRound.Phase phase = table.round() == null ? RouletteRound.Phase.IDLE
                : table.round().snapshot().phase();
        // A spin starts with the croupier turning the wheel and throwing the ball, as the ball leaves his side.
        long now = server.getServer().overworld().getGameTime();
        if (phase != lastPhase) {
            if (phase == RouletteRound.Phase.BETTING) {
                say(Line.BETS_OPEN, "");
                calledLastBets = false;
            } else if (phase == RouletteRound.Phase.SPINNING) {
                triggerAnim("gestures", "launch_ball");
                waved = false;
            } else if (phase == RouletteRound.Phase.PAYOUT) {
                triggerAnim("gestures", "announce_result");
                say(Line.RESULT, Integer.toString(table.round().snapshot().number()));
            }
            lastPhase = phase;
        }
        if (phase == RouletteRound.Phase.BETTING && !calledLastBets && table.round() != null) {
            long left = table.round().snapshot().deadline() - now;
            if (left <= LAST_BETS_TICKS && left > LAST_BETS_TICKS / 2) {
                say(Line.LAST_BETS, "");
                calledLastBets = true;
            }
        }
        if (phase == RouletteRound.Phase.SPINNING && !waved && table.round() != null) {
            long started = table.round().snapshot().deadline() - RouletteRounds.SPIN_TICKS;
            if (now - started >= NO_MORE_BETS_TICKS) {
                triggerAnim("gestures", "close_bets");
                say(Line.NO_MORE_BETS, "");
                waved = true;
            }
        }
        if (tickCount % GREETING_CHECK_TICKS == 0) {
            greet(phase);
        }
    }

    /** Welcomes a player who walks up to a table where nothing is going on. */
    private void greet(RouletteRound.Phase phase) {
        boolean near = level().getNearestPlayer(this, GREETING_DISTANCE) != null;
        boolean quiet = phase == RouletteRound.Phase.IDLE || phase == RouletteRound.Phase.COMPLETE
                || phase == RouletteRound.Phase.CANCELLED;
        if (near && !playerNear && quiet) {
            say(Line.WELCOME, "");
        }
        playerNear = near;
    }

    /** Red, black or zero, for the result line. */
    public static String colour(int number) {
        return number == 0 ? "zero" : RouletteLogic.isRed(number) ? "red" : "black";
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && player.distanceToSqr(this) <= 6 * 6
                && level().getBlockEntity(tablePos) instanceof RouletteTableBlockEntity table) {
            table.openMenu(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("CasinoTable", tablePos.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tablePos = BlockPos.of(tag.getLong("CasinoTable"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 10,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.croupier.idle"))));
        controllers.add(new AnimationController<>(this, "gestures", 4, state -> PlayState.STOP)
                .triggerableAnim("close_bets", RawAnimation.begin().thenPlay("animation.croupier.close_bets"))
                .triggerableAnim("launch_ball", RawAnimation.begin().thenPlay("animation.croupier.launch_ball"))
                .triggerableAnim("announce_result",
                        RawAnimation.begin().thenPlay("animation.croupier.announce_result")));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
