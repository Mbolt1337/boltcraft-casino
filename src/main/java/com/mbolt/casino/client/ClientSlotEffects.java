package com.mbolt.casino.client;

import com.mbolt.casino.block.CasinoBlock;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.client.sound.ReelSpinSound;
import com.mbolt.casino.config.CasinoClientConfig;
import com.mbolt.casino.registry.ModParticles;
import com.mbolt.casino.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Client-only sounds and particles of a slot machine. */
public final class ClientSlotEffects {
    private static final float STOP_VOLUME = 0.6f;
    private static final float STOP_PITCH_BASE = 1.0f;
    private static final float STOP_PITCH_STEP = 0.1f;

    private static final int WIN_POUR_TICKS = 20;
    private static final int JACKPOT_POUR_TICKS = 60;
    private static final int MIN_WIN_CHIPS = 8;
    private static final int MAX_WIN_CHIPS = 32;
    private static final int JACKPOT_TRAY_CHIPS_PER_TICK = 4;
    private static final int JACKPOT_SPARKLES_PER_TICK = 2;
    private static final int CLINK_EVERY_TICKS = 3;
    private static final float CLINK_VOLUME = 0.5f;

    // Places on the machine in blocks, measured from the bottom centre of its lower half.
    private static final double TRAY_REACH = 0.52;
    private static final double TRAY_HEIGHT = 0.22;
    private static final double TRAY_WIDTH = 0.4;
    private static final double TOPPER_HEIGHT = 2.05;
    private static final double TOPPER_WIDTH = 0.7;
    private static final double SPARKLE_REACH = 0.6;
    private static final double SPARKLE_WIDTH = 1.2;
    private static final double SPARKLE_LOW = 0.9;
    private static final double SPARKLE_HIGH = 2.3;

    private ClientSlotEffects() {
    }

    public static void startReelSound(SlotMachineBlockEntity machine) {
        Minecraft.getInstance().getSoundManager().play(new ReelSpinSound(machine));
    }

    /** Each reel clicks a little higher than the one before. */
    public static void reelStopped(SlotMachineBlockEntity machine, int stoppedReels) {
        var level = machine.getLevel();
        if (level != null) {
            float pitch = STOP_PITCH_BASE + stoppedReels * STOP_PITCH_STEP;
            level.playLocalSound(machine.getBlockPos(), ModSounds.REEL_STOP.get(), SoundSource.BLOCKS,
                    STOP_VOLUME, pitch, false);
        }
    }

    /**
     * One tick of a payout: chips pour out of the tray, one for every chip won within limits. A jackpot gushes for
     * three seconds while chips burst from the topper among gold sparkles. Returns false once the pour is over.
     */
    public static boolean pourWinnings(SlotMachineBlockEntity machine, int age, boolean jackpot) {
        Level level = machine.getLevel();
        int duration = jackpot ? JACKPOT_POUR_TICKS : WIN_POUR_TICKS;
        if (level == null || age >= duration) {
            return false;
        }
        if (CasinoClientConfig.PARTICLES.get()) {
            Pour pour = new Pour(level, machine);
            int trayChips = jackpot ? JACKPOT_TRAY_CHIPS_PER_TICK : winChipsThisTick(machine.payout(), age);
            for (int chip = 0; chip < trayChips; chip++) {
                pour.fromTray();
            }
            if (jackpot) {
                pour.fromTopper();
                for (int sparkle = 0; sparkle < JACKPOT_SPARKLES_PER_TICK; sparkle++) {
                    pour.sparkle();
                }
            }
        }
        if (age % CLINK_EVERY_TICKS == 0) {
            float pitch = 0.9f + level.random.nextFloat() * 0.4f;
            level.playLocalSound(machine.getBlockPos(), ModSounds.CHIP_CLINK.get(), SoundSource.BLOCKS,
                    CLINK_VOLUME, pitch, false);
        }
        return true;
    }

    /** Spreads a win's chips evenly over the pour. */
    private static int winChipsThisTick(long payout, int age) {
        int total = Math.clamp(payout, MIN_WIN_CHIPS, MAX_WIN_CHIPS);
        return total * (age + 1) / WIN_POUR_TICKS - total * age / WIN_POUR_TICKS;
    }

    /** Spawn points and throws relative to the way the machine faces. */
    private static final class Pour {
        private final Level level;
        private final RandomSource random;
        private final Vec3 base;
        private final Vec3 out;
        private final Vec3 side;

        Pour(Level level, SlotMachineBlockEntity machine) {
            this.level = level;
            random = level.random;
            base = Vec3.atBottomCenterOf(machine.getBlockPos());
            Direction front = machine.getBlockState().getValue(CasinoBlock.FACING);
            out = Vec3.atLowerCornerOf(front.getNormal());
            side = Vec3.atLowerCornerOf(front.getClockWise().getNormal());
        }

        /** Out of the tray mouth, forwards and a little up, so the chips land in front of the player. */
        void fromTray() {
            Vec3 at = base.add(out.scale(TRAY_REACH)).add(side.scale(spread(TRAY_WIDTH))).add(0, TRAY_HEIGHT, 0);
            Vec3 throwing = out.scale(0.06 + random.nextDouble() * 0.08).add(side.scale(spread(0.08)))
                    .add(0, 0.05 + random.nextDouble() * 0.08, 0);
            chip(at, throwing);
        }

        /** Straight up out of the topper, falling all around the machine. */
        void fromTopper() {
            Vec3 at = base.add(side.scale(spread(TOPPER_WIDTH))).add(0, TOPPER_HEIGHT, 0);
            Vec3 throwing = out.scale(spread(0.24) + 0.04).add(side.scale(spread(0.24)))
                    .add(0, 0.25 + random.nextDouble() * 0.15, 0);
            chip(at, throwing);
        }

        /** Anywhere in front of the cabinet, from the button deck to above the topper. */
        void sparkle() {
            double height = SPARKLE_LOW + random.nextDouble() * (SPARKLE_HIGH - SPARKLE_LOW);
            Vec3 at = base.add(out.scale(SPARKLE_REACH)).add(side.scale(spread(SPARKLE_WIDTH))).add(0, height, 0);
            level.addParticle(ModParticles.SPARKLE.get(), at.x, at.y, at.z, spread(0.02), 0.01, spread(0.02));
        }

        private void chip(Vec3 at, Vec3 throwing) {
            level.addParticle(ModParticles.CHIP.get(), at.x, at.y, at.z, throwing.x, throwing.y, throwing.z);
        }

        /** A random offset between -width / 2 and width / 2. */
        private double spread(double width) {
            return (random.nextDouble() - 0.5) * width;
        }
    }
}
