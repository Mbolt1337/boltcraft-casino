package com.mbolt.casino.block;

import com.mbolt.casino.block.entity.CasinoPartBlockEntity;
import com.mbolt.casino.block.entity.OwnedBlockEntity;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.game.SlotRounds;
import com.mbolt.casino.menu.SlotMachineMenu;
import com.mbolt.casino.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/** A two-block-tall slot machine. The lower half holds the block entity and renders the whole machine. */
public final class SlotMachineBlock extends CasinoBlock {
    public static final MapCodec<SlotMachineBlock> CODEC = simpleCodec(SlotMachineBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public SlotMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    /** For any block: the slot machine part holding the block entity, or the position itself. */
    public static BlockPos basePos(BlockState state, BlockPos pos) {
        boolean upperHalf = state.getBlock() instanceof SlotMachineBlock
                && state.getValue(HALF) == DoubleBlockHalf.UPPER;
        return upperHalf ? pos.below() : pos;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    @Override
    protected BlockPos blockEntityPos(BlockState state, BlockPos pos) {
        return basePos(state, pos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        Level level = context.getLevel();
        if (above.getY() >= level.getMaxBuildHeight() || !level.getBlockState(above).canBeReplaced(context)) {
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new SlotMachineBlockEntity(pos, state) : new CasinoPartBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void openMenu(ServerPlayer player, BlockPos pos, OwnedBlockEntity blockEntity) {
        var provider = new SimpleMenuProvider((id, inventory, viewer) -> new SlotMachineMenu(id, inventory, pos),
                getName());
        player.openMenu(provider, buf -> buf.writeBlockPos(pos));
    }

    /** Breaking the upper half in creative removes the lower half without a drop, like a door. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        boolean upper = state.getValue(HALF) == DoubleBlockHalf.UPPER;
        boolean noDrop = player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos);
        if (!level.isClientSide && upper && noDrop && level.getBlockState(pos.below()).is(this)) {
            level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Each half disappears when its partner is gone. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        Direction partner = state.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        if (direction == partner) {
            boolean paired = neighbor.is(this) && neighbor.getValue(HALF) != state.getValue(HALF);
            if (!paired) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** A machine removed mid-spin refunds the bet; the round itself lives on in the world data. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof SlotMachineBlockEntity slot && slot.spinning()) {
            SlotRounds.get(server.getServer()).cancel(slot.roundId());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type, ModBlockEntities.SLOT_MACHINE.get(), SlotMachineBlockEntity::clientTick);
        }
        return createTickerHelper(type, ModBlockEntities.SLOT_MACHINE.get(), SlotMachineBlockEntity::serverTick);
    }
}
