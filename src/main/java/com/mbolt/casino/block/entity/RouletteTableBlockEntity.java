package com.mbolt.casino.block.entity;

import com.mbolt.casino.block.CasinoBlock;
import com.mbolt.casino.entity.CroupierEntity;
import com.mbolt.casino.game.RouletteRoundNbt;
import com.mbolt.casino.game.RouletteRounds;
import com.mbolt.casino.game.roulette.RouletteRound;
import com.mbolt.casino.menu.RouletteMenu;
import com.mbolt.casino.registry.ModBlockEntities;
import com.mbolt.casino.registry.ModEntities;
import com.mbolt.casino.client.ClientRouletteEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/** Public round snapshot drives the renderer and the croupier; the world's journal owns settlement. */
public final class RouletteTableBlockEntity extends LinkedBlockEntity implements GeoBlockEntity {
    /** From the table centre to the croupier's feet in blocks: the table's half depth plus half his width. */
    private static final double CROUPIER_DISTANCE = 20.25 / 16;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable private RouletteRound round;
    @Nullable private UUID croupierId;
    private int missingCroupierTicks;
    @Nullable private UUID soundingRound;
    private boolean ballDropped;

    public RouletteTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROULETTE_TABLE.get(), pos, state);
    }

    @Nullable public UUID roundId() { return round == null ? null : round.snapshot().id(); }
    @Nullable public RouletteRound round() { return round; }
    public boolean isCroupier(UUID id) { return id.equals(croupierId); }

    public CompoundTag roundTag() {
        return round == null ? new CompoundTag() : RouletteRoundNbt.save(round);
    }

    public void applyRound(RouletteRound value) {
        round = RouletteRound.restore(value.snapshot());
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void clearRound() {
        round = null;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void openMenu(ServerPlayer player) {
        var provider = new SimpleMenuProvider((id, inventory, viewer) -> new RouletteMenu(id, inventory, worldPosition),
                getBlockState().getBlock().getName());
        player.openMenu(provider, buf -> buf.writeBlockPos(worldPosition));
    }

    @Override
    public boolean canRelink() {
        return noRoundRunning();
    }

    private boolean noRoundRunning() {
        return round == null || round.snapshot().phase() == RouletteRound.Phase.COMPLETE
                || round.snapshot().phase() == RouletteRound.Phase.CANCELLED;
    }

    public Vec3 tableCenter() {
        var facing = getBlockState().getValue(CasinoBlock.FACING);
        var right = facing.getClockWise();
        var back = facing.getOpposite();
        return new Vec3(worldPosition.getX() + .5 + (right.getStepX() + back.getStepX()) * .5,
                worldPosition.getY(), worldPosition.getZ() + .5 + (right.getStepZ() + back.getStepZ()) * .5);
    }

    /** Behind the wheel, just clear of the table's back edge, facing the players. */
    public Vec3 croupierPosition() {
        var back = getBlockState().getValue(CasinoBlock.FACING).getOpposite();
        return tableCenter().add(back.getStepX() * CROUPIER_DISTANCE, 0, back.getStepZ() * CROUPIER_DISTANCE);
    }

    public void removedFromWorld() {
        if (level instanceof ServerLevel server) {
            RouletteRounds.get(server.getServer()).cancel(roundId());
            if (croupierId != null && server.getEntity(croupierId) instanceof CroupierEntity croupier) {
                croupier.discard();
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RouletteTableBlockEntity table) {
        var server = (ServerLevel) level;
        var current = RouletteRounds.get(server.getServer()).find(table.roundId());
        if (current != null && (table.round == null || !current.snapshot().equals(table.round.snapshot()))) {
            table.applyRound(current);
        } else if (current == null && !table.noRoundRunning()) {
            // An unfinished round the world no longer knows ended while this table was unloaded: its stakes were
            // settled or refunded already. A finished round stays, so the last result keeps showing.
            table.clearRound();
        }
        if (!table.hasOwner() || !table.isLinked() || level.getGameTime() % 40 != 0) {
            return;
        }
        if (table.croupierId != null) {
            if (server.getEntity(table.croupierId) != null) {
                table.missingCroupierTicks = 0;
                return;
            }
            // Wait for entity loading. An old orphan that loads later discards itself on the identity check.
            BlockPos dealerPos = BlockPos.containing(table.croupierPosition());
            if (server.getChunkSource().getChunkNow(dealerPos.getX() >> 4, dealerPos.getZ() >> 4) == null
                    || ++table.missingCroupierTicks < 5) {
                return;
            }
        }
        var croupier = ModEntities.CROUPIER.get().create(server);
        if (croupier != null) {
            croupier.bind(table);
            if (server.noCollision(croupier)) {
                server.addFreshEntity(croupier);
                table.croupierId = croupier.getUUID();
                table.missingCroupierTicks = 0;
                table.setChanged();
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, RouletteTableBlockEntity table) {
        if (table.round == null || table.round.snapshot().phase() != RouletteRound.Phase.SPINNING) {
            return;
        }
        if (!table.roundId().equals(table.soundingRound)) {
            table.soundingRound = table.roundId();
            table.ballDropped = false;
            ClientRouletteEffects.start(table);
        }
        if (!table.ballDropped && level.getGameTime() >= table.round.snapshot().deadline() - 10) {
            table.ballDropped = true;
            ClientRouletteEffects.drop(table);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Round", roundTag());
        if (croupierId != null) { tag.putUUID("Croupier", croupierId); }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readClientState(tag);
        croupierId = tag.hasUUID("Croupier") ? tag.getUUID("Croupier") : null;
    }

    @Override
    protected void writeClientState(CompoundTag tag) { tag.put("Round", roundTag()); }

    @Override
    protected void readClientState(CompoundTag tag) {
        var saved = tag.getCompound("Round");
        round = saved.isEmpty() ? null : RouletteRoundNbt.load(saved);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
