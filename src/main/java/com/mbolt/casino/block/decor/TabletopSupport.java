package com.mbolt.casino.block.decor;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.entity.ServingTrayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Keeps small props on the actual table surface, including half-height coffee tables and slabs. */
@EventBusSubscriber(modid = CasinoMod.MODID)
public final class TabletopSupport {
    public static final IntegerProperty LOWERING = IntegerProperty.create("lowering", 0, 8);

    private TabletopSupport() {
    }

    public static int lowering(BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        var shape = level.getBlockState(below).getCollisionShape(level, below);
        double top = shape.toAabbs().stream()
                .filter(box -> box.minX <= .5 && box.maxX >= .5 && box.minZ <= .5 && box.maxZ >= .5)
                .mapToDouble(box -> box.maxY).max().orElse(0);
        return top < .5 ? -1 : Math.clamp((int) Math.round((1 - top) * 16), 0, 8);
    }

    public static double offset(BlockState state) {
        return -state.getValue(LOWERING) / 16.0;
    }

    /** A lowered tray also remains usable on vanilla slabs and tables supplied by other mods. */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockState above = event.getLevel().getBlockState(event.getPos().above());
        if (!(above.getBlock() instanceof ServingTrayBlock) || above.getValue(LOWERING) == 0) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            use(event.getLevel().getBlockState(event.getPos()), event.getLevel(), event.getPos(), player,
                    event.getHitVec());
        } else if (!player.isShiftKeyDown() && ServingTrayBlockEntity.isServing(held)) {
            useItem(held, event.getLevel(), event.getPos(), player, event.getHand(), event.getHitVec());
        } else {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }

    /** A low tray shares the table's space visually, so clicking the table also reaches its servings. */
    public static InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                         BlockHitResult hit) {
        BlockState above = level.getBlockState(pos.above());
        if (above.getBlock() instanceof ServingTrayBlock tray && above.getValue(LOWERING) > 0) {
            return tray.useWithoutItem(above, level, pos.above(), player, upperHit(pos, hit));
        }
        return InteractionResult.PASS;
    }

    public static ItemInteractionResult useItem(ItemStack stack, Level level, BlockPos pos, Player player,
                                                InteractionHand hand, BlockHitResult hit) {
        BlockState above = level.getBlockState(pos.above());
        if (above.getBlock() instanceof ServingTrayBlock tray && above.getValue(LOWERING) > 0) {
            return tray.useItemOn(stack, above, level, pos.above(), player, hand, upperHit(pos, hit));
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static BlockHitResult upperHit(BlockPos pos, BlockHitResult hit) {
        return new BlockHitResult(hit.getLocation(), Direction.UP, pos.above(), hit.isInside());
    }
}
