package com.mbolt.casino.item;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.CasinoText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A casino chip of a fixed value. Chips are only issued by cashiers and are never crafted. */
public final class ChipItem extends Item {
    private final int value;

    public ChipItem(int value, Properties properties) {
        super(properties);
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "item." + CasinoMod.MODID + ".chip.value";
        tooltip.add(Component.translatable(key, CasinoText.chips(value)).withStyle(ChatFormatting.GOLD));
        if (stack.getCount() > 1) {
            long total = (long) value * stack.getCount();
            tooltip.add(Component.translatable(key + "_stack", CasinoText.chips(total))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
