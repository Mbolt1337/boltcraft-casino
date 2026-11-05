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
 * A board on a wall several blocks wide and tall: the CASINO marquee signs, the giant chips, the banner. Part 0
 * is the bottom left as seen from the front, where the player clicked; parts count to the right, then up.
 * Breaking any part takes the whole board down and drops it once, from part 0.
 */
public final class BoardBlock extends WallDecorBlock {
    public static final int MAX_PARTS = 64;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, MAX_PARTS - 1);

    private final int width;
    private final int height;
    private final double depth;

    public BoardBlock(Properties properties, int width, int height, double depth) {
        super(properties, flat(depth));
        if (width * height > MAX_PARTS) {
            throw new IllegalArgumentException("A board has at most " + MAX_PARTS + " parts");
        }
        this.width = width;
        this.height = height;
        this.depth = depth;
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int parts() {
        return width * height;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(properties -> new BoardBlock(properties, width, height, depth));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    /** Columns run to the viewer's right, the facing turned counter-clockwise; rows run up. */
    public static BlockPos partPos(BlockPos anchor, Direction facing, int width, int part) {
        return anchor.relative(facing.getCounterClockWise(), part % width).above(part / width);
    }

    private BlockPos anchor(BlockState state, BlockPos pos) {
        int part = state.getValue(PART);
        return pos.relative(state.getValue(FACING).getClockWise(), part % width).below(part / width);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        for (int part = 1; part < parts(); part++) {
            BlockPos pos = partPos(context.getClickedPos(), state.getValue(FACING), width, part);
            if (!context.getLevel().isInWorldBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context)) {
                return null;
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        for (int part = 1; part < parts(); part++) {
            level.setBlock(partPos(pos, state.getValue(FACING), width, part), state.setValue(PART, part),
                    Block.UPDATE_ALL);
        }
    }

    /** A part whose neighbouring part went missing goes too, so the whole board comes down together. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        int part = state.getValue(PART);
        int column = part % width;
        int row = part / width;
        int expected = -1;
        if (direction == facing.getCounterClockWise() && column < width - 1) {
            expected = part + 1;
        } else if (direction == facing.getClockWise() && column > 0) {
            expected = part - 1;
        } else if (direction == Direction.UP && row < height - 1) {
            expected = part + width;
        } else if (direction == Direction.DOWN && row > 0) {
            expected = part - width;
        }
        if (expected >= 0 && (!neighbor.is(this) || neighbor.getValue(FACING) != facing
                || neighbor.getValue(PART) != expected)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /** In creative, or without drops, breaking another part must not make part 0 drop the board. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && state.getValue(PART) != 0
                && (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            level.setBlock(anchor(state, pos), Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        if (mirror == Mirror.NONE) {
            return result;
        }
        int part = state.getValue(PART);
        return result.setValue(PART, part / width * width + width - 1 - part % width);
    }
}
