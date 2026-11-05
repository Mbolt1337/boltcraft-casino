package com.mbolt.casino.block.decor;

import com.mbolt.casino.block.HorizontalShapes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decor that hangs on a wall: signs, neon, skirting, sconces, curtains. Its model faces north with its back on the
 * wall to the south. Clicking a wall puts it on that wall; clicking a floor or ceiling turns it to the player.
 */
public class WallDecorBlock extends HorizontalDirectionalBlock {
    private final HorizontalShapes shapes;

    public WallDecorBlock(Properties properties, HorizontalShapes shapes) {
        super(properties);
        this.shapes = shapes;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** A flat piece against the wall, {@code depth} pixels thick. */
    public static HorizontalShapes flat(double depth) {
        return new HorizontalShapes(new double[]{0, 0, 16 - depth, 16, 16, 16});
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(properties -> new WallDecorBlock(properties, shapes));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }
}
