package com.mbolt.casino.block.decor;

import com.mbolt.casino.block.HorizontalShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A strip on the wall that runs on into the same block next to it on the same wall: neon tube, marquee lights.
 * The four properties are the arms the model draws, left and right as seen from the front.
 */
public abstract class WallConnectingBlock extends WallDecorBlock {
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    protected WallConnectingBlock(Properties properties, HorizontalShapes shapes) {
        super(properties, shapes);
        registerDefaultState(defaultBlockState().setValue(UP, false).setValue(DOWN, false)
                .setValue(LEFT, true).setValue(RIGHT, true));
    }

    /** Which arms to draw for the neighbours this block connects to. */
    protected abstract BlockState arms(BlockState state, boolean up, boolean down, boolean left, boolean right);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(UP, DOWN, LEFT, RIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return connect(state, level, pos);
    }

    private BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        // Seen from the front the viewer looks against the facing, so their left is the facing turned clockwise.
        return arms(state, connects(state, level, pos, Direction.UP), connects(state, level, pos, Direction.DOWN),
                connects(state, level, pos, facing.getClockWise()),
                connects(state, level, pos, facing.getCounterClockWise()));
    }

    private boolean connects(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        BlockState other = level.getBlockState(pos.relative(direction));
        return other.is(this) && other.getValue(FACING) == state.getValue(FACING);
    }

    protected static BlockState withArms(BlockState state, boolean up, boolean down, boolean left, boolean right) {
        return state.setValue(UP, up).setValue(DOWN, down).setValue(LEFT, left).setValue(RIGHT, right);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT))
                .setValue(RIGHT, state.getValue(LEFT));
    }
}
