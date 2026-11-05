package com.mbolt.casino.client.sound;

import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.game.roulette.RouletteRound;
import com.mbolt.casino.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** A local loop stops with the authoritative round. Global casino volume is applied by ClientSoundSettings. */
public final class RouletteSpinSound extends AbstractTickableSoundInstance {
    private final RouletteTableBlockEntity table;

    public RouletteSpinSound(RouletteTableBlockEntity table) {
        super(ModSounds.ROULETTE_SPIN.get(), SoundSource.BLOCKS, RandomSource.create());
        this.table = table;
        var center = table.tableCenter();
        x = center.x;
        y = center.y + 1;
        z = center.z;
        looping = true;
        delay = 4;
        volume = .4f;
    }

    @Override
    public void tick() {
        if (table.isRemoved() || table.round() == null
                || table.round().snapshot().phase() != RouletteRound.Phase.SPINNING) {
            stop();
        }
    }
}
