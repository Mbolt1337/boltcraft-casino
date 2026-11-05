package com.mbolt.casino.block.decor;

import com.mbolt.casino.block.HorizontalShapes;
import com.mbolt.casino.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A velvet sofa seat. Seats side by side with the same facing join into one long sofa: only the ends keep their
 * arms. Left and right are the sitter's, who faces the way the sofa faces.
 */
public final class VelvetSofaBlock extends FacingDecorBlock implements Seat {
    public static final BooleanProperty ARM_LEFT = BooleanProperty.create("arm_left");
    public static final BooleanProperty ARM_RIGHT = BooleanProperty.create("arm_right");
    private static final HorizontalShapes SHAPES = new HorizontalShapes(
            new double[]{0, 0, 1, 16, 8, 16}, new double[]{0, 8, 13, 16, 15, 16});

    public VelvetSofaBlock(Properties properties) {
        super(properties, SHAPES);
        registerDefaultState(defaultBlockState().setValue(ARM_LEFT, true).setValue(ARM_RIGHT, true));
    }

    @Override
    public double seatHeight() {
        return .5;
    }

    @Override
    public double seatForwardOffset() {
        return 3.5 / 16;
    }

    @Override
    public Direction seatFacing(BlockState state, Player player) {
        return state.getValue(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        return SeatEntity.sit(level, pos, state, player);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ARM_LEFT, ARM_RIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return arms(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return arms(state, level, pos);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(ARM_LEFT, state.getValue(ARM_RIGHT))
                .setValue(ARM_RIGHT, state.getValue(ARM_LEFT));
    }

    private BlockState arms(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(ARM_LEFT, !joins(state, level, pos.relative(facing.getCounterClockWise())))
                .setValue(ARM_RIGHT, !joins(state, level, pos.relative(facing.getClockWise())));
    }

    private boolean joins(BlockState state, BlockGetter level, BlockPos neighborPos) {
        BlockState neighbor = level.getBlockState(neighborPos);
        return neighbor.is(this) && neighbor.getValue(FACING) == state.getValue(FACING);
    }
}
