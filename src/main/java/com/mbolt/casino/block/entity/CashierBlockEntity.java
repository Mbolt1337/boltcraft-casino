package com.mbolt.casino.block.entity;

import com.mbolt.casino.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The cashier only knows its owner and its vault; every exchange runs in {@code CashierMenu}. */
public final class CashierBlockEntity extends LinkedBlockEntity {
    public CashierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CASHIER.get(), pos, state);
    }
}
