package com.mbolt.casino.block.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A neon board several blocks wide. Part 0 is the left end as seen from the front, where the player clicked; the
 * other parts follow to the right. Breaking any part takes the whole board down and drops it once, from part 0.
 */
public final class NeonPanelBlock extends WallDecorBlock {
    public static final int MAX_WIDTH = 5;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, MAX_WIDTH - 1);

    private final int width;

    public NeonPanelBlock(Properties properties, int width) {
        super(properties, flat(3));
        this.width = width;
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }

    public int width() {
        return width;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(PART, width - 1 - state.getValue(PART));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(properties -> new NeonPanelBlock(properties, width));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    /** Parts run to the viewer's right, which is the facing turned counter-clockwise. */
    private static BlockPos partPos(BlockPos anchor, Direction facing, int part) {
        return anchor.relative(facing.getCounterClockWise(), part);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        for (int part = 1; part < width; part++) {
            BlockPos pos = partPos(context.getClickedPos(), state.getValue(FACING), part);
            if (!context.getLevel().isInWorldBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context)) {
                return null;
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        for (int part = 1; part < width; part++) {
            level.setBlock(partPos(pos, state.getValue(FACING), part), state.setValue(PART, part), Block.UPDATE_ALL);
        }
    }

    /** A part whose neighbouring part went missing goes too, so the whole board comes down together. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        int part = state.getValue(PART);
        int expected = direction == facing.getCounterClockWise() ? part + 1
                : direction == facing.getClockWise() ? part - 1 : -1;
        if (expected >= 0 && expected < width && (!neighbor.is(this) || neighbor.getValue(FACING) != facing
                || neighbor.getValue(PART) != expected)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /** In creative, or without drops, breaking another part must not make part 0 drop the board. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        int part = state.getValue(PART);
        if (!level.isClientSide && part != 0 && (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            BlockPos anchor = pos.relative(state.getValue(FACING).getClockWise(), part);
            level.setBlock(anchor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
