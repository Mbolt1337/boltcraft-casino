package com.mbolt.casino.block.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * A gold stanchion that hangs a velvet rope to every rope stanchion next to it, of any color. Like a fence it
 * cannot be jumped over, so it closes off a VIP corner.
 */
public final class VelvetRopeBlock extends CrossCollisionBlock {
    public static final MapCodec<VelvetRopeBlock> CODEC = simpleCodec(VelvetRopeBlock::new);

    public VelvetRopeBlock(Properties properties) {
        super(4, 2, 16, 13, 24, properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends CrossCollisionBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState()
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction),
                    level.getBlockState(pos.relative(direction)).getBlock() instanceof VelvetRopeBlock);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction.getAxis().isHorizontal()) {
            return state.setValue(PROPERTY_BY_DIRECTION.get(direction), neighbor.getBlock() instanceof VelvetRopeBlock);
        }
        return state;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
