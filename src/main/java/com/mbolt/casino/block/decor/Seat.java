package com.mbolt.casino.block.decor;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** A furniture surface that holds one seated player. Heights are measured from the block's bottom. */
public interface Seat {
    double seatHeight();

    default double seatForwardOffset() {
        return 0;
    }

    default Direction seatFacing(BlockState state, Player player) {
        return player.getDirection();
    }
}
