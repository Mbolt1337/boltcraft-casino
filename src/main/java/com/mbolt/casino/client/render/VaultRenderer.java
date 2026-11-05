package com.mbolt.casino.client.render;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class VaultRenderer extends GeoBlockRenderer<VaultBlockEntity> {
    public VaultRenderer(BlockEntityRendererProvider.Context context) {
        super(new DefaultedBlockGeoModel<>(CasinoMod.id("vault")));
    }
}
