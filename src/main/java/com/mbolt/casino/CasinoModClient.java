package com.mbolt.casino;

import com.mbolt.casino.block.decor.Neon;
import com.mbolt.casino.block.decor.NeonColor;
import com.mbolt.casino.client.ClientSoundSettings;
import com.mbolt.casino.client.particle.ChipParticle;
import com.mbolt.casino.client.particle.SparkleParticle;
import com.mbolt.casino.client.render.CroupierRenderer;
import com.mbolt.casino.client.render.RouletteTableRenderer;
import com.mbolt.casino.client.render.ServingTrayRenderer;
import com.mbolt.casino.client.render.SlotMachineRenderer;
import com.mbolt.casino.client.render.VaultRenderer;
import com.mbolt.casino.client.screen.CashierScreen;
import com.mbolt.casino.client.screen.GuideBookScreen;
import com.mbolt.casino.client.screen.RouletteScreen;
import com.mbolt.casino.client.screen.SlotMachineScreen;
import com.mbolt.casino.client.screen.VaultScreen;
import com.mbolt.casino.config.CasinoClientConfig;
import com.mbolt.casino.item.GuideBookItem;
import com.mbolt.casino.registry.ModBlockEntities;
import com.mbolt.casino.registry.ModDecor;
import com.mbolt.casino.registry.ModEntities;
import com.mbolt.casino.registry.ModMenus;
import com.mbolt.casino.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CasinoMod.MODID, dist = Dist.CLIENT)
public final class CasinoModClient {
    public CasinoModClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CasinoClientConfig.SPEC);
        GuideBookItem.setOpener(() -> Minecraft.getInstance().setScreen(new GuideBookScreen()));
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.register(ClientSoundSettings.class);
        modBus.addListener(CasinoModClient::registerScreens);
        modBus.addListener(CasinoModClient::registerRenderers);
        modBus.addListener(CasinoModClient::registerParticles);
        modBus.addListener(CasinoModClient::registerBlockColors);
        modBus.addListener(CasinoModClient::registerItemColors);
    }

    /** Neon letters and tubes are drawn white and take their color from the block state. */
    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> 0xFF000000 | state.getValue(Neon.COLOR).rgb(),
                ModDecor.NEON_LETTER.get(), ModDecor.NEON_TUBE.get());
    }

    /** In the inventory they show the color they are placed in. */
    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? 0xFF000000 | NeonColor.PINK.rgb() : -1,
                ModDecor.NEON_LETTER.get(), ModDecor.NEON_TUBE.get());
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ROULETTE_TABLE.get(), RouletteScreen::new);
        event.register(ModMenus.VAULT.get(), VaultScreen::new);
        event.register(ModMenus.CASHIER.get(), CashierScreen::new);
        event.register(ModMenus.SLOT_MACHINE.get(), SlotMachineScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEAT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SERVING_TRAY.get(), ServingTrayRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ROULETTE_TABLE.get(), RouletteTableRenderer::new);
        event.registerEntityRenderer(ModEntities.CROUPIER.get(), CroupierRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.VAULT.get(), VaultRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SLOT_MACHINE.get(), SlotMachineRenderer::new);
    }

    private static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.CHIP.get(), ChipParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPARKLE.get(), SparkleParticle.Provider::new);
    }
}
