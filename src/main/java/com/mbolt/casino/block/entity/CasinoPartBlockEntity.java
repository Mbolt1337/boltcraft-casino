package com.mbolt.casino.block.entity;

import com.mbolt.casino.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Inert companion part. Only the main part owns a bank link, renderer, ticker or round. */
public final class CasinoPartBlockEntity extends BlockEntity {
    public CasinoPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CASINO_PART.get(), pos, state);
    }
}
