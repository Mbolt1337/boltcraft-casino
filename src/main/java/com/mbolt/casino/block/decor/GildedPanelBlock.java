package com.mbolt.casino.block.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Red lacquer cladding framed in gold. Panels side by side join into one field, so the gold frame only runs
 * round the outside of the whole field, whatever its size: each property says a panel continues that way.
 */
public final class GildedPanelBlock extends Block {
    public static final MapCodec<GildedPanelBlock> CODEC = simpleCodec(GildedPanelBlock::new);

    public GildedPanelBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : PipeBlock.PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        BlockGetter level = context.getLevel();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                    level.getBlockState(context.getClickedPos().relative(direction)).is(this));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), neighbor.is(this));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            rotated = rotated.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(rotation.rotate(direction)),
                    state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction)));
        }
        return rotated;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            mirrored = mirrored.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(mirror.mirror(direction)),
                    state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction)));
        }
        return mirrored;
    }
}
