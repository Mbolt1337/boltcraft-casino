package com.mbolt.casino.test;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.UUID;

/** Fresh fake players for game tests; every call gets a new UUID so parallel tests never share state. */
final class TestPlayers {
    private TestPlayers() {
    }

    static ServerPlayer at(GameTestHelper helper, String name, BlockPos relative) {
        var profile = new GameProfile(UUID.randomUUID(), name);
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.setPos(helper.absolutePos(relative).getCenter());
        player.getInventory().clearContent();
        return player;
    }
}
