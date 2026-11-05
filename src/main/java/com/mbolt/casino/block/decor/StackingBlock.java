package com.mbolt.casino.block.decor;

import com.mbolt.casino.block.HorizontalShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Decor that knows where it stands in a stack of itself: a column gets its plinth at the bottom and its capital
 * at the top, a curtain its rod above and its fringe below. Curtains only stack on the same wall; a column's
 * facing means nothing, so columns stack whichever way they were placed.
 */
public class StackingBlock extends WallDecorBlock {
    public static final EnumProperty<StackPart> PART = EnumProperty.create("part", StackPart.class);

    private final boolean onWall;

    public StackingBlock(Properties properties, HorizontalShapes shapes, boolean onWall) {
        super(properties, shapes);
        this.onWall = onWall;
        registerDefaultState(defaultBlockState().setValue(PART, StackPart.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return stack(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis().isVertical() ? stack(state, level, pos) : state;
    }

    private BlockState stack(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(PART, StackPart.of(continues(state, level, pos.below()),
                continues(state, level, pos.above())));
    }

    private boolean continues(BlockState state, BlockGetter level, BlockPos neighborPos) {
        BlockState neighbor = level.getBlockState(neighborPos);
        return neighbor.is(this) && (!onWall || neighbor.getValue(FACING) == state.getValue(FACING));
    }
}
