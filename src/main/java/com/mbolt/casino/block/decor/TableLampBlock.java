package com.mbolt.casino.block.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class TableLampBlock extends ShapedDecorBlock {
    public static final MapCodec<TableLampBlock> CODEC = simpleCodec(TableLampBlock::new);

    public TableLampBlock(Properties properties) {
        super(properties, Block.box(3, 0, 3, 13, 14, 13));
        registerDefaultState(defaultBlockState().setValue(TabletopSupport.LOWERING, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TabletopSupport.LOWERING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int lowering = TabletopSupport.lowering(context.getLevel(), context.getClickedPos());
        return lowering < 0 ? null : defaultBlockState().setValue(TabletopSupport.LOWERING, lowering);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction != Direction.DOWN) {
            return state;
        }
        int lowering = TabletopSupport.lowering(level, pos);
        return lowering < 0 ? Blocks.AIR.defaultBlockState() : state.setValue(TabletopSupport.LOWERING, lowering);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return super.getShape(state, level, pos, context).move(0, TabletopSupport.offset(state), 0);
    }
}
