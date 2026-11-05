package com.mbolt.casino.client;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.client.sound.CasinoVolumeSound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Local volume applies equally to sounds sent by the server and to client-side reel and chip effects. */
public final class ClientSoundSettings {
    private ClientSoundSettings() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound != null && sound.getLocation().getNamespace().equals(CasinoMod.MODID)) {
            event.setSound(CasinoVolumeSound.wrap(sound));
        }
    }
}
