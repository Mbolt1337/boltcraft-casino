package com.mbolt.casino.block.decor;

import net.minecraft.world.level.block.state.BlockState;

/** A strip of chasing bulbs round a sign or along a facade. Runs straight on past its last neighbour. */
public final class MarqueeLightsBlock extends WallConnectingBlock {
    public MarqueeLightsBlock(Properties properties) {
        super(properties, flat(3));
    }

    @Override
    protected BlockState arms(BlockState state, boolean up, boolean down, boolean left, boolean right) {
        boolean vertical = up || down;
        boolean horizontal = left || right;
        return withArms(state,
                up || down && !horizontal,
                down || up && !horizontal,
                left || right && !vertical || !vertical && !horizontal,
                right || left && !vertical || !vertical && !horizontal);
    }
}
