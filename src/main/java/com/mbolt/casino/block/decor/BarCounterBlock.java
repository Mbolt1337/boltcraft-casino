package com.mbolt.casino.block.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A bar counter, its padded front to the customers on the side it faces. Counters in a row join up, and where
 * two rows meet at a right angle the corner piece wraps round (outer corner) or turns in (inner corner), the way
 * stairs do.
 */
public final class BarCounterBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<CounterShape> SHAPE = EnumProperty.create("shape", CounterShape.class);
    public static final MapCodec<BarCounterBlock> CODEC = simpleCodec(BarCounterBlock::new);

    public BarCounterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(SHAPE, CounterShape.STRAIGHT));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return state.setValue(SHAPE, shape(state, context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis().isHorizontal() ? state.setValue(SHAPE, shape(state, level, pos)) : state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        if (mirror == Mirror.NONE) {
            return result;
        }
        CounterShape mirrored = switch (state.getValue(SHAPE)) {
            case INNER_LEFT -> CounterShape.INNER_RIGHT;
            case INNER_RIGHT -> CounterShape.INNER_LEFT;
            case OUTER_LEFT -> CounterShape.OUTER_RIGHT;
            case OUTER_RIGHT -> CounterShape.OUTER_LEFT;
            case STRAIGHT -> CounterShape.STRAIGHT;
        };
        return result.setValue(SHAPE, mirrored);
    }

    /**
     * A counter behind this one turned across it makes this an outer corner, its customers on both outer sides.
     * One in front turned across makes an inner corner, with a notch where the two fronts meet.
     */
    static CounterShape shape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockState behind = level.getBlockState(pos.relative(facing.getOpposite()));
        if (behind.getBlock() instanceof BarCounterBlock && behind.getValue(FACING).getAxis() != facing.getAxis()) {
            return behind.getValue(FACING) == facing.getCounterClockWise()
                    ? CounterShape.OUTER_LEFT : CounterShape.OUTER_RIGHT;
        }
        BlockState front = level.getBlockState(pos.relative(facing));
        if (front.getBlock() instanceof BarCounterBlock && front.getValue(FACING).getAxis() != facing.getAxis()) {
            return front.getValue(FACING) == facing.getCounterClockWise()
                    ? CounterShape.INNER_LEFT : CounterShape.INNER_RIGHT;
        }
        return CounterShape.STRAIGHT;
    }
}
