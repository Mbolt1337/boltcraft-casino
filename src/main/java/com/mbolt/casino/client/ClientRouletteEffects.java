package com.mbolt.casino.client;

import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.client.sound.RouletteSpinSound;
import com.mbolt.casino.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;

public final class ClientRouletteEffects {
    private ClientRouletteEffects() { }

    public static void start(RouletteTableBlockEntity table) {
        Minecraft.getInstance().getSoundManager().play(new RouletteSpinSound(table));
    }

    public static void drop(RouletteTableBlockEntity table) {
        var center = table.tableCenter();
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(ModSounds.BALL_DROP.get(),
                SoundSource.BLOCKS, .6f, 1, table.getLevel().random, center.x, center.y + 1, center.z));
    }
}
