package com.mbolt.casino.game;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server-wide game bookkeeping that is not tied to any loaded block. */
public final class GameEvents {
    private GameEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        SlotRounds.get(server).tick(server);
        RouletteRounds.get(server).tick(server);
        PendingPayouts.get(server).deliverOnline(server);
    }
}
