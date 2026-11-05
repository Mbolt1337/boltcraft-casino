package com.mbolt.casino.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.stream.Stream;

/** Counting, taking and giving items across the main inventory and the off hand. Armor slots are ignored. */
public final class PlayerItems {
    private PlayerItems() {
    }

    public static long count(Player player, Item item) {
        long count = 0;
        for (ItemStack stack : carriedStacks(player)) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Removes exactly {@code amount}; the caller must check {@link #count} first. */
    public static void remove(Player player, Item item, long amount) {
        if (amount < 0 || count(player, item) < amount) {
            throw new IllegalArgumentException("Player does not carry enough " + item);
        }
        long remaining = amount;
        for (ItemStack stack : carriedStacks(player)) {
            if (remaining == 0) {
                break;
            }
            if (stack.is(item)) {
                int taken = (int) Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;
            }
        }
        inventoryChanged(player);
    }

    /** Adds items to the inventory; whatever does not fit is dropped for the player and can never be lost. */
    public static void give(ServerPlayer player, Item item, long amount) {
        long remaining = amount;
        while (remaining > 0) {
            int size = (int) Math.min(remaining, item.getDefaultMaxStackSize());
            ItemStack stack = new ItemStack(item, size);
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                dropFor(player, stack);
            }
            remaining -= size;
        }
        inventoryChanged(player);
    }

    /** Drops resources at a block, split into normal stacks. */
    public static void dropAt(Level level, BlockPos pos, Item item, long amount) {
        long remaining = amount;
        while (remaining > 0) {
            int size = (int) Math.min(remaining, item.getDefaultMaxStackSize());
            Block.popResource(level, pos, new ItemStack(item, size));
            remaining -= size;
        }
    }

    private static void dropFor(ServerPlayer player, ItemStack stack) {
        ItemEntity dropped = player.drop(stack, false);
        if (dropped != null) {
            dropped.setNoPickUpDelay();
            dropped.setTarget(player.getUUID());
        }
    }

    private static List<ItemStack> carriedStacks(Player player) {
        Inventory inventory = player.getInventory();
        return Stream.concat(inventory.items.stream(), inventory.offhand.stream()).toList();
    }

    private static void inventoryChanged(Player player) {
        player.getInventory().setChanged();
        // Keeps the client inventory current while a casino screen, which has no slots, is open.
        player.inventoryMenu.broadcastChanges();
    }
}
