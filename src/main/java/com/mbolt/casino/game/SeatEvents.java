package com.mbolt.casino.game;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.entity.SeatEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Dismount before vanilla saves the player, since furniture anchors deliberately are not saved. */
@EventBusSubscriber(modid = CasinoMod.MODID)
public final class SeatEvents {
    private SeatEvents() {
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        release(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        event.getServer().getPlayerList().getPlayers().forEach(SeatEvents::release);
    }

    private static void release(Player player) {
        if (!player.level().isClientSide && player.getVehicle() instanceof SeatEntity seat) {
            player.stopRiding();
            seat.discard();
        }
    }
}
