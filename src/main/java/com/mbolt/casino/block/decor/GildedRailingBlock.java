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
 * A gold balustrade for balconies, galleries and stairs: square posts, a top rail and balusters. It runs on
 * into the railing beside it and into any solid wall, and like a fence it cannot be climbed over.
 */
public final class GildedRailingBlock extends CrossCollisionBlock {
    public static final MapCodec<GildedRailingBlock> CODEC = simpleCodec(GildedRailingBlock::new);

    public GildedRailingBlock(Properties properties) {
        super(4, 2, 16, 16, 24, properties);
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

    private static boolean connects(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighbor = level.getBlockState(neighborPos);
        return neighbor.getBlock() instanceof GildedRailingBlock
                || neighbor.isFaceSturdy(level, neighborPos, direction.getOpposite());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState()
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connects(level, pos, direction));
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
            return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connects(level, pos, direction));
        }
        return state;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
