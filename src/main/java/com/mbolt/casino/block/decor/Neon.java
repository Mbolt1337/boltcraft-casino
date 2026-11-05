package com.mbolt.casino.block.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** What neon blocks share: a color property that a dye changes. */
public final class Neon {
    public static final EnumProperty<NeonColor> COLOR = EnumProperty.create("color", NeonColor.class);

    private Neon() {
    }

    /** Recolors the block with the dye in hand, using up one dye outside creative mode. */
    public static ItemInteractionResult dye(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                            Player player) {
        if (!(stack.getItem() instanceof DyeItem dye)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        NeonColor color = NeonColor.fromDye(dye.getDyeColor());
        if (color == null || state.getValue(COLOR) == color) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(COLOR, color), 3);
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1, 1);
            stack.consume(1, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
