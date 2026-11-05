package com.mbolt.casino.inventory;

import com.mbolt.casino.economy.ChipMath;
import com.mbolt.casino.item.ChipItem;
import com.mbolt.casino.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/** The chips a player carries, treated as one balance. */
public final class ChipWallet {
    private ChipWallet() {
    }

    public static long balance(Player player) {
        long total = 0;
        for (ChipItem chip : ModItems.chipsByValue()) {
            total = Math.addExact(total, Math.multiplyExact(PlayerItems.count(player, chip), chip.value()));
        }
        return total;
    }

    /** Takes {@code cost} chips and returns the change in the best denominations. */
    public static void take(ServerPlayer player, long cost) {
        Map<Integer, Long> available = new LinkedHashMap<>();
        for (ChipItem chip : ModItems.chipsByValue()) {
            available.put(chip.value(), PlayerItems.count(player, chip));
        }
        ChipMath.Payment payment = ChipMath.payment(available, cost);
        payment.taken().forEach((value, count) -> PlayerItems.remove(player, ModItems.chip(value), count));
        give(player, payment.change());
    }

    /** Gives {@code amount} chips split greedily into the largest denominations. */
    public static void give(ServerPlayer player, long amount) {
        ChipMath.split(amount).forEach((value, count) -> PlayerItems.give(player, ModItems.chip(value), count));
    }
}
