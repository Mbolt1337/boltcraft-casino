package com.mbolt.casino.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The guide book: right click opens its own screen on the client. The client registers how to open it, so this
 * class never touches client code and a dedicated server can load it.
 */
public final class GuideBookItem extends Item {
    private static Runnable opener = () -> {
    };

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    public static void setOpener(Runnable open) {
        opener = open;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            opener.run();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
