package com.mbolt.casino.menu;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;

import java.util.UUID;

/** Player names for UUIDs stored in the journal, also for players who are offline. */
public final class PlayerNames {
    private static final int SHORT_UUID_LENGTH = 8;

    private PlayerNames() {
    }

    public static String of(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        GameProfileCache cache = server.getProfileCache();
        if (cache != null) {
            var profile = cache.get(id).map(GameProfile::getName);
            if (profile.isPresent()) {
                return profile.get();
            }
        }
        return id.toString().substring(0, SHORT_UUID_LENGTH);
    }
}
