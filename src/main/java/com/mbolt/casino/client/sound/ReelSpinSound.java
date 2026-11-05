package com.mbolt.casino.client.sound;

import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** The looping reel whir; it fades out as the last reel comes to rest. */
public final class ReelSpinSound extends AbstractTickableSoundInstance {
    private static final float VOLUME = 0.55f;
    private static final double MACHINE_CENTER_HEIGHT = 1.25;

    private final SlotMachineBlockEntity machine;
    private final long spinStartedAt;

    public ReelSpinSound(SlotMachineBlockEntity machine) {
        super(ModSounds.REEL_SPIN.get(), SoundSource.BLOCKS, RandomSource.create());
        this.machine = machine;
        this.spinStartedAt = machine.startedAt();
        this.looping = true;
        this.delay = 0;
        this.volume = VOLUME;
        this.x = machine.getBlockPos().getX() + 0.5;
        this.y = machine.getBlockPos().getY() + MACHINE_CENTER_HEIGHT;
        this.z = machine.getBlockPos().getZ() + 0.5;
    }

    @Override
    public void tick() {
        double lastStop = SlotMachineBlockEntity.FIRST_REEL_STOP
                + (SlotMachineBlockEntity.REELS - 1) * SlotMachineBlockEntity.REEL_STOP_STEP;
        boolean finished = machine.isRemoved() || !machine.spinning() || machine.startedAt() != spinStartedAt
                || machine.spinProgress(0) >= lastStop;
        if (finished) {
            stop();
        }
    }
}
