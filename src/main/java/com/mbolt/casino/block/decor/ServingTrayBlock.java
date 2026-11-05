package com.mbolt.casino.block.decor;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.block.entity.ServingTrayBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Put a serving down with it in hand, or take the clicked serving with an empty hand. No menu or currency. */
public final class ServingTrayBlock extends BaseEntityBlock {
    public static final MapCodec<ServingTrayBlock> CODEC = simpleCodec(ServingTrayBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 2, 16, 3, 14);

    public ServingTrayBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TabletopSupport.LOWERING, 0));
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
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ServingTrayBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.move(0, TabletopSupport.offset(state), 0);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()
                || !ServingTrayBlockEntity.isServing(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ServingTrayBlockEntity tray
                && tray.stillValid(player) && !tray.serve(player, slot(hit))) {
            player.displayClientMessage(CasinoText.message("tray_full"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player.distanceToSqr(pos.getCenter()) <= 6 * 6) {
                level.destroyBlock(pos, !player.hasInfiniteMaterials());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ServingTrayBlockEntity tray
                && tray.stillValid(player) && !tray.take(player, slot(hit))) {
            player.displayClientMessage(CasinoText.message("tray_empty_slot"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static int slot(BlockHitResult hit) {
        double x = hit.getLocation().x - hit.getBlockPos().getX();
        double z = hit.getLocation().z - hit.getBlockPos().getZ();
        return (x < .5 ? 0 : 1) + (z < .5 ? 0 : 2);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ServingTrayBlockEntity tray
                ? AbstractContainerMenu.getRedstoneSignalFromContainer(tray) : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof ServingTrayBlockEntity tray) {
            Containers.dropContents(level, pos, tray);
            tray.clearContent();
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
