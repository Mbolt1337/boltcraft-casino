package com.mbolt.casino.test;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.game.SlotRounds;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Pictures for the public project page, from the real game and nothing else: {@code -PpreviewScene=promo} films the
 * games in a dressed studio, {@code promo_world} the two casinos at dusk. Bursts of frames two ticks apart become
 * the page's animations ({@code art/promo.py}). Development tool, never shipped.
 */
final class PromoPreview {
    private static final int FLOOR_Y = -61;
    /** Two game ticks between frames: ten frames a second, the speed the game itself runs at. */
    private static final int FRAME_TICKS = 2;

    private static final Vec3 JACKPOT_EYE = new Vec3(6.0, -58.45, -1.9);
    private static final Vec3 JACKPOT_TARGET = new Vec3(4.4, -59.25, 0.25);
    private static final Vec3 TABLE_EYE = new Vec3(14.9, -58.05, -3.1);
    private static final Vec3 TABLE_TARGET = new Vec3(12.9, -59.45, -0.5);
    private static final Vec3 SCREEN_EYE = new Vec3(1.0, -58.4, -2.0);
    private static final Vec3 SCREEN_TARGET = new Vec3(1.0, -59.5, 0.5);

    /*
     * The jackpot: reels stop 39, 48 and 57 ticks after the pull and the chips pour out for 60 ticks after that.
     * The roulette: betting closes 100 ticks after the first bet, the wheel turns for 160 ticks, the croupier calls
     * the number at the end.
     */
    static final List<ArtPreview.Shot> SHOTS = games();

    static final List<ArtPreview.Shot> WORLD_SHOTS = world();

    private PromoPreview() {
    }

    private static List<ArtPreview.Shot> games() {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        shots.add(new ArtPreview.Shot("promo_studio", JACKPOT_EYE, JACKPOT_TARGET, 30));
        burst(shots, "promo_jackpot", JACKPOT_EYE, JACKPOT_TARGET, 64,
                ArtPreview.onServer(server -> ArtPreview.spin(server, SlotRounds::isJackpot)));
        shots.add(new ArtPreview.Shot("promo_roulette_bets", TABLE_EYE, TABLE_TARGET, 96,
                ArtPreview.onServer(ArtPreview::placeRouletteBets)));
        burst(shots, "promo_roulette", TABLE_EYE, TABLE_TARGET, 92, mc -> {});
        // The slot machine screen with its reels turning, after the round above has settled.
        shots.add(ArtPreview.Shot.screen("promo_gui_slot", 40, tidy().andThen(
                ArtPreview.onServer(server -> ArtPreview.open(server, ArtPreview.SLOT_MACHINE)))));
        List<ArtPreview.Shot> spin = new ArrayList<>();
        burst(spin, "promo_gui_spin", SCREEN_EYE, SCREEN_TARGET, 34,
                mc -> DevClient.press(mc, CasinoText.gui("spin"), 1));
        spin.replaceAll(shot -> new ArtPreview.Shot(shot.name(), shot.eye(), shot.target(), true, -1, shot.ticks(),
                shot.action()));
        shots.addAll(spin);
        return List.copyOf(shots);
    }

    private static List<ArtPreview.Shot> world() {
        List<ArtPreview.Shot> shots = new ArrayList<>();
        // The grand hall stands at x 40..80 with its steps facing south at z 45; the lounge to its west.
        Vec3 hall = new Vec3(60.5, FLOOR_Y + 14, 26);
        shots.add(new ArtPreview.Shot("promo_dusk", new Vec3(60.5, FLOOR_Y + 13, 78), hall, 40,
                ArtPreview.onServer(server -> DevClient.command(server, "time set 12800"))));
        int frames = 70;
        for (int i = 0; i < frames; i++) {
            // Round the front of the hall from the south-west to the south-east, slowly sinking.
            double angle = Math.toRadians(-52 + 104.0 * i / (frames - 1));
            Vec3 eye = new Vec3(hall.x + Math.sin(angle) * 48, FLOOR_Y + 16 - 4.0 * i / (frames - 1),
                    hall.z + Math.cos(angle) * 48);
            shots.add(new ArtPreview.Shot(frame("promo_flyover", i), eye, hall, i == 0 ? 20 : FRAME_TICKS));
        }
        shots.add(new ArtPreview.Shot("promo_night", new Vec3(60.5, FLOOR_Y + 13, 78), hall, 40,
                ArtPreview.onServer(server -> DevClient.command(server, "time set midnight"))));
        shots.add(new ArtPreview.Shot("promo_night_lounge", new Vec3(12.5, FLOOR_Y + 7, 46),
                new Vec3(12.5, FLOOR_Y + 9, 18), 20));
        return List.copyOf(shots);
    }

    /** Frames two ticks apart from a fixed camera; the action runs as the first frame starts. */
    private static void burst(List<ArtPreview.Shot> shots, String name, Vec3 eye, Vec3 target, int frames,
                              Consumer<Minecraft> action) {
        for (int i = 0; i < frames; i++) {
            Consumer<Minecraft> first = i == 0 ? action : mc -> {};
            shots.add(new ArtPreview.Shot(frame(name, i), eye, target, FRAME_TICKS, tidy().andThen(first)));
        }
    }

    /** Advancement toasts and chat lines are the test player's, not part of the picture. */
    private static Consumer<Minecraft> tidy() {
        return mc -> {
            mc.getToasts().clear();
            mc.gui.getChat().clearMessages(false);
        };
    }

    private static String frame(String name, int index) {
        return String.format(Locale.ROOT, "%s_%03d", name, index);
    }

    /**
     * The games showroom dressed as a corner of a casino: carpet, a panelled wall with gold trim, the JACKPOT
     * board over the slot machine. The vanilla blocks kept beside the games for scale make way for planters.
     */
    static void build(MinecraftServer server) {
        ArtPreview.buildShowroom(server);
        int y = FLOOR_Y;
        command(server, "fill -9 %d -8 17 %d 4 boltcraft_casino:casino_carpet_burgundy_block", y, y);
        command(server, "fill -9 %d 3 17 %d 3 boltcraft_casino:wood_wall_panel", y + 1, y + 2);
        command(server, "fill -9 %d 3 17 %d 3 boltcraft_casino:gilded_panel", y + 3, y + 5);
        command(server, "fill -9 %d 3 17 %d 3 boltcraft_casino:gilded_cornice", y + 6, y + 6);
        for (int x = -8; x <= 16; x += 6) {
            command(server, "fill %d %d 3 %d %d 3 boltcraft_casino:onyx_column", x, y + 1, x, y + 5);
        }
        // Facing north, part 0 is the viewer's left, to the east.
        for (int part = 0; part < 4; part++) {
            command(server, "setblock %d %d 2 boltcraft_casino:neon_panel_jackpot[facing=north,part=%d]", 6 - part,
                    y + 3, part);
        }
        command(server, "setblock -6 %d 0 boltcraft_casino:gold_planter", y + 1);
        command(server, "setblock -4 %d 0 minecraft:air", y + 1);
        command(server, "setblock 6 %d 0 minecraft:air", y + 1);
        for (int x : new int[]{-3, 9, 15}) {
            command(server, "setblock %d %d 2 boltcraft_casino:wall_sconce[facing=north]", x, y + 3);
        }
        command(server, "setblock 10 %d 2 boltcraft_casino:neon_sign_chip[facing=north]", y + 3);
        command(server, "fill 10 %d -1 11 %d -1 boltcraft_casino:velvet_rope_red", y + 1, y + 1);
    }

    private static void command(MinecraftServer server, String format, Object... args) {
        DevClient.command(server, String.format(Locale.ROOT, format, args));
    }
}
