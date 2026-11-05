package com.mbolt.casino.block;

import com.mbolt.casino.block.entity.CasinoPartBlockEntity;
import com.mbolt.casino.block.entity.OwnedBlockEntity;
import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Four horizontal parts, one bank link, one renderer, one item drop. */
public final class RouletteTableBlock extends CasinoBlock {
    public static final MapCodec<RouletteTableBlock> CODEC = simpleCodec(RouletteTableBlock::new);
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public RouletteTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }

    public static BlockPos partPos(BlockPos anchor, Direction facing, int part) {
        return anchor.relative(facing.getClockWise(), part & 1).relative(facing.getOpposite(), part >> 1);
    }

    public static BlockPos basePos(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof RouletteTableBlock)) {
            return pos;
        }
        int part = state.getValue(PART);
        return pos.relative(state.getValue(FACING).getClockWise(), -(part & 1))
                .relative(state.getValue(FACING).getOpposite(), -(part >> 1));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Override
    protected BlockPos blockEntityPos(BlockState state, BlockPos pos) { return basePos(state, pos); }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        for (int part = 1; part < 4; part++) {
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
        for (int part = 1; part < 4; part++) {
            level.setBlock(partPos(pos, state.getValue(FACING), part), state.setValue(PART, part), Block.UPDATE_CLIENTS);
        }
        super.setPlacedBy(level, pos, state, placer, stack);
        for (int part = 0; part < 4; part++) {
            level.updateNeighborsAt(partPos(pos, state.getValue(FACING), part), this);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0
                ? new RouletteTableBlockEntity(pos, state) : new CasinoPartBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void openMenu(ServerPlayer player, BlockPos pos, OwnedBlockEntity blockEntity) {
        ((RouletteTableBlockEntity) blockEntity).openMenu(player);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        BlockPos anchor = basePos(state, pos);
        for (int part = 0; part < 4; part++) {
            BlockPos member = partPos(anchor, state.getValue(FACING), part);
            if (member.equals(neighborPos) && (!neighbor.is(this) || neighbor.getValue(PART) != part
                    || neighbor.getValue(FACING) != state.getValue(FACING))) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && state.getValue(PART) != 0
                && (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            level.setBlock(basePos(state, pos), Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof RouletteTableBlockEntity table) {
            table.removedFromWorld();
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ROULETTE_TABLE.get(), level.isClientSide
                ? RouletteTableBlockEntity::clientTick : RouletteTableBlockEntity::serverTick);
    }
}
