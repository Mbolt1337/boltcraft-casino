package com.mbolt.casino;

import com.mbolt.casino.block.Protection;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.datagen.CasinoData;
import com.mbolt.casino.game.GameEvents;
import com.mbolt.casino.game.GuideDeliveries;
import com.mbolt.casino.network.CasinoNetwork;
import com.mbolt.casino.registry.ModBlockEntities;
import com.mbolt.casino.registry.ModBlocks;
import com.mbolt.casino.registry.ModCreativeTabs;
import com.mbolt.casino.registry.ModDataComponents;
import com.mbolt.casino.registry.ModDecor;
import com.mbolt.casino.registry.ModItems;
import com.mbolt.casino.registry.ModEntities;
import com.mbolt.casino.registry.ModMenus;
import com.mbolt.casino.registry.ModParticles;
import com.mbolt.casino.registry.ModSounds;
import com.mbolt.casino.registry.ModStructures;
import com.mbolt.casino.registry.ModTriggers;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(CasinoMod.MODID)
public final class CasinoMod {
    public static final String MODID = "boltcraft_casino";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CasinoMod(IEventBus modBus, ModContainer container) {
        ModBlocks.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModDecor.BLOCKS.register(modBus);
        ModDecor.ITEMS.register(modBus);
        ModEntities.REGISTER.register(modBus);
        modBus.addListener(ModEntities::attributes);
        ModBlockEntities.REGISTER.register(modBus);
        ModMenus.REGISTER.register(modBus);
        ModDataComponents.REGISTER.register(modBus);
        ModSounds.REGISTER.register(modBus);
        ModTriggers.REGISTER.register(modBus);
        ModStructures.TYPES.register(modBus);
        ModStructures.PIECES.register(modBus);
        ModParticles.REGISTER.register(modBus);
        ModCreativeTabs.REGISTER.register(modBus);

        container.registerConfig(ModConfig.Type.SERVER, CasinoServerConfig.SPEC);
        modBus.addListener(CasinoServerConfig::onLoad);
        modBus.addListener(CasinoServerConfig::onReload);
        modBus.addListener(CasinoNetwork::register);
        modBus.addListener(CasinoData::gather);

        NeoForge.EVENT_BUS.register(Protection.class);
        NeoForge.EVENT_BUS.register(GameEvents.class);
        NeoForge.EVENT_BUS.register(GuideDeliveries.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
