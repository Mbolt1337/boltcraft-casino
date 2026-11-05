package com.mbolt.casino.test;

import com.mbolt.casino.block.decor.NeonGlyph;
import com.mbolt.casino.block.decor.NeonPanelBlock;
import com.mbolt.casino.client.screen.GuideBookScreen;
import com.mbolt.casino.client.screen.NeonGlyphScreen;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.registry.ModCreativeTabs;
import com.mbolt.casino.registry.ModDecor;
import com.mbolt.casino.test.ArtPreview.Shot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * The decor showroom for {@code gradlew runClient -PartPreview -PpreviewScene=decor}: a lounge wall with the
 * "BOLTCRAFT CASINO" sign in neon letters framed by a tube, the boards in marquee lights, icon signs, a bar with
 * both kinds of corner, a sofa corner, a card table, ropes, chandeliers, columns, a curtain and every floor.
 *
 * <p>The wall runs along x at z = 7 and is seen from the north, so the viewer's left is +x: text and boards are
 * laid out from x = {@link #LEFT} towards smaller x.
 */
final class DecorPreview {
    private static final int FLOOR = -61;
    private static final int Y = FLOOR + 1;
    private static final int WALL_Z = 7;
    private static final int ON_WALL = WALL_Z - 1;
    private static final int LEFT = 8;
    private static final String SIGN_TEXT = "BOLTCRAFT CASINO";

    private static final Vec3 OVERVIEW_EYE = new Vec3(0.5, -54.5, -13.5);
    private static final Vec3 OVERVIEW_TARGET = new Vec3(0.5, -56.5, 4.5);
    private static final Vec3 FACADE_EYE = new Vec3(0.5, -55.0, -4.5);
    private static final Vec3 FACADE_TARGET = new Vec3(0.5, -55.0, 6.5);
    private static final Vec3 BAR_EYE = new Vec3(5.5, -57.6, -1.5);
    private static final Vec3 BAR_TARGET = new Vec3(-0.5, -60.0, 3.5);
    private static final Vec3 LOUNGE_EYE = new Vec3(12.5, -57.6, -1.0);
    private static final Vec3 LOUNGE_TARGET = new Vec3(10.5, -59.8, 3.5);

    static final List<Shot> SHOTS = List.of(
            new Shot("decor_overview", OVERVIEW_EYE, OVERVIEW_TARGET, false),
            new Shot("decor_facade", FACADE_EYE, FACADE_TARGET, false),
            new Shot("decor_letters_angle", new Vec3(-5.5, -56.0, 0.5), new Vec3(2.5, -56.0, 6.5), false),
            new Shot("decor_panels", new Vec3(2.5, -53.0, 0.0), new Vec3(2.5, -53.0, 6.5), false),
            new Shot("decor_signs", new Vec3(2.5, -58.6, 1.5), new Vec3(2.5, -58.8, 6.5), false),
            new Shot("decor_bar", BAR_EYE, BAR_TARGET, false),
            new Shot("decor_bar_back", new Vec3(0.5, -58.4, 5.8), new Vec3(0.5, -60.2, 3.0), false),
            new Shot("decor_bar_corner", new Vec3(-6.0, -57.8, 1.0), new Vec3(-2.5, -59.8, 4.0), false),
            new Shot("decor_lounge", LOUNGE_EYE, LOUNGE_TARGET, false),
            new Shot("decor_card_table", new Vec3(6.5, -58.0, -0.6), new Vec3(6.5, -60.0, 2.5), false),
            new Shot("decor_ropes", new Vec3(-8.5, -57.6, -2.0), new Vec3(-8.5, -58.6, 2.5), false),
            new Shot("decor_columns", new Vec3(16.5, -57.4, -0.5), new Vec3(16.5, -58.0, 6.0), false),
            new Shot("decor_floors", new Vec3(2.5, -55.5, -11.5), new Vec3(2.5, -61.0, -3.0), false),
            new Shot("decor_hotbar", OVERVIEW_EYE, OVERVIEW_TARGET, true),
            new Shot("decor_night_overview", OVERVIEW_EYE, OVERVIEW_TARGET, 40,
                    ArtPreview.onServer(server -> DevClient.command(server, "time set midnight"))),
            new Shot("decor_night_facade", FACADE_EYE, FACADE_TARGET, false),
            new Shot("decor_night_bar", BAR_EYE, BAR_TARGET, false),
            new Shot("decor_night_lounge", LOUNGE_EYE, LOUNGE_TARGET, false),
            // The picker closes beyond eight blocks, so this one is opened standing at the sign.
            new Shot("gui_neon_picker", new Vec3(0.5, -55.4, 1.0), new Vec3(0.5, -55.5, 6.5), true, -1, 20,
                    mc -> mc.setScreen(new NeonGlyphScreen(new BlockPos(LEFT - 8, -56, ON_WALL)))),
            new Shot("gui_decor_tab", FACADE_EYE, FACADE_TARGET, true, -1, 20, DecorPreview::openDecorTab),
            // The end of the tab, where the newest blocks are.
            new Shot("gui_decor_tab_end", FACADE_EYE, FACADE_TARGET, true, -1, 10, mc -> {
                if (mc.screen != null) {
                    mc.screen.mouseScrolled(0, 0, 0, -100);
                }
            }),
            new Shot("gui_guide_1", FACADE_EYE, FACADE_TARGET, true, -1, 20,
                    mc -> mc.setScreen(new GuideBookScreen())),
            guidePage("gui_guide_2"), guidePage("gui_guide_3"), guidePage("gui_guide_4"),
            guidePage("gui_guide_5"), guidePage("gui_guide_6"), guidePage("gui_guide_7"),
            guidePage("gui_guide_8"), guidePage("gui_guide_9"), guidePage("gui_guide_10"),
            guidePage("gui_guide_11"), guidePage("gui_guide_12"));

    /** The next spread of the open guide book. */
    private static Shot guidePage(String name) {
        return new Shot(name, FACADE_EYE, FACADE_TARGET, true, -1, 10, mc -> {
            if (mc.screen instanceof GuideBookScreen book) {
                book.turn(1);
            }
        });
    }

    private DecorPreview() {
    }

    static void build(MinecraftServer server) {
        ServerLevel level = server.overworld();
        // A stand-in link so the support page shows; the real one is Maxim's to give.
        CasinoServerConfig.SUPPORT_URL.set("https://example.com");
        DevClient.command(server, "gamerule doDaylightCycle false");
        DevClient.command(server, "gamerule doWeatherCycle false");
        DevClient.command(server, "time set noon");
        DevClient.command(server, "weather clear");
        fill(server, -14, FLOOR, -11, 22, FLOOR, WALL_Z, "minecraft:dark_oak_planks");
        fill(server, -14, Y, WALL_Z, 22, Y + 1, WALL_Z, "boltcraft_casino:wood_wall_panel");
        fill(server, -14, Y + 2, WALL_Z, 22, Y + 10, WALL_Z, "minecraft:polished_blackstone_bricks");
        fill(server, -14, Y + 2, WALL_Z, -10, Y + 10, WALL_Z, "boltcraft_casino:velvet_wall_panel");
        fill(server, 18, Y + 2, WALL_Z, 22, Y + 10, WALL_Z, "boltcraft_casino:velvet_wall_panel");
        fill(server, -9, Y, ON_WALL, 12, Y, ON_WALL, "boltcraft_casino:gold_skirting[facing=north]");

        // The sign: letters framed by a yellow tube, boards in marquee lights above, icon signs below.
        for (int i = 0; i < SIGN_TEXT.length(); i++) {
            char c = SIGN_TEXT.charAt(i);
            if (c != ' ') {
                String color = i < 9 ? "pink" : "cyan";
                set(server, LEFT - i, -56, ON_WALL, "neon_letter[facing=north,glyph=" + glyph(c) + ",color="
                        + color + "]");
            }
        }
        for (int x = LEFT + 1; x >= LEFT - SIGN_TEXT.length(); x--) {
            set(server, x, -57, ON_WALL, "neon_tube[facing=north,color=yellow]");
            set(server, x, -55, ON_WALL, "neon_tube[facing=north,color=yellow]");
        }
        set(server, LEFT + 1, -56, ON_WALL, "neon_tube[facing=north,color=yellow]");
        set(server, LEFT - SIGN_TEXT.length(), -56, ON_WALL, "neon_tube[facing=north,color=yellow]");
        panel(level, ModDecor.NEON_PANEL_BOLTCRAFT.get(), LEFT, -52);
        panel(level, ModDecor.NEON_PANEL_JACKPOT.get(), LEFT - 6, -52);
        panel(level, ModDecor.NEON_PANEL_BAR.get(), LEFT - 11, -52);
        panel(level, ModDecor.NEON_PANEL_VIP.get(), LEFT - 14, -52);
        for (int x = LEFT + 1; x >= LEFT - 5; x--) {
            set(server, x, -53, ON_WALL, "marquee_lights[facing=north]");
            set(server, x, -51, ON_WALL, "marquee_lights[facing=north]");
        }
        set(server, LEFT + 1, -52, ON_WALL, "marquee_lights[facing=north]");
        set(server, LEFT - 5, -52, ON_WALL, "marquee_lights[facing=north]");
        for (int i = 0; i < ModDecor.NEON_SIGNS.size(); i++) {
            set(server, LEFT - 2 * i, -59, ON_WALL, "neon_sign_" + ModDecor.NEON_SIGNS.get(i) + "[facing=north]");
        }
        set(server, 12, -58, ON_WALL, "wall_sconce[facing=north]");
        set(server, -10, -58, ON_WALL, "wall_sconce[facing=north]");

        // A curtained doorway between two columns.
        for (int y = Y; y <= Y + 3; y++) {
            for (int x = 15; x <= 17; x++) {
                set(server, x, y, ON_WALL, "velvet_curtain[facing=north]");
            }
        }
        for (int y = Y; y <= Y + 4; y++) {
            set(server, 14, y, 5, "casino_column");
            set(server, 18, y, 5, "casino_column");
        }

        // The bar: an arm facing east, an inner corner, a straight run, an outer corner and an arm facing west.
        set(server, 3, Y, 1, "bar_counter[facing=east]");
        set(server, 3, Y, 2, "bar_counter[facing=east]");
        for (int x = 3; x >= -3; x--) {
            set(server, x, Y, 3, "bar_counter[facing=north]");
        }
        set(server, -3, Y, 4, "bar_counter[facing=west]");
        set(server, -3, Y, 5, "bar_counter[facing=west]");
        for (int x : new int[]{1, -1}) {
            set(server, x, Y, 2, "bar_stool");
        }
        set(server, 4, Y, 1, "bar_stool");
        set(server, -4, Y, 4, "bar_stool");

        // The lounge and the card table.
        for (int x = 10; x <= 12; x++) {
            set(server, x, Y, 4, "velvet_sofa[facing=north]");
        }
        set(server, 11, Y, 2, "coffee_table");
        set(server, 13, Y, 4, "gold_planter");
        set(server, 9, Y, 4, "gold_planter");
        set(server, 11, -56, 3, "crystal_chandelier");
        set(server, 6, Y, 2, "card_table[facing=north]");
        set(server, 5, Y, 2, "bar_stool");
        set(server, 7, Y, 2, "bar_stool");
        set(server, 6, Y, 1, "bar_stool");

        // Ropes of every color round a VIP corner, under the grand chandelier.
        String[] ropes = {"red", "red", "blue", "blue", "green", "black"};
        for (int i = 0; i < ropes.length; i++) {
            set(server, -6 - i, Y, 1, "velvet_rope_" + ropes[i]);
        }
        set(server, -11, Y, 2, "velvet_rope_black");
        set(server, -11, Y, 3, "velvet_rope_black");
        set(server, -8, -55, 3, "grand_chandelier");

        // Floors: four carpets, then marble and checkered tiles with their stairs and slabs.
        String[] carpets = {"burgundy", "navy", "felt", "diamond"};
        for (int i = 0; i < carpets.length; i++) {
            int from = 14 - 7 * i;
            fill(server, from - 6, Y, -3, from, Y, -1, "boltcraft_casino:casino_carpet_" + carpets[i]);
            set(server, 16 + i, Y, -2, "casino_carpet_" + carpets[i] + "_block");
        }
        fill(server, 4, FLOOR, -9, 14, FLOOR, -5, "boltcraft_casino:marble_tiles");
        fill(server, -8, FLOOR, -9, 3, FLOOR, -5, "boltcraft_casino:checkered_tiles");
        set(server, -10, Y, -6, "marble_tile_stairs[facing=south]");
        set(server, -11, Y, -6, "marble_tile_slab");
        set(server, -12, Y, -6, "checkered_tile_stairs[facing=south]");
        set(server, -13, Y, -6, "checkered_tile_slab");

        settle(level, new BlockPos(-14, Y, -11), new BlockPos(22, Y + 10, WALL_Z));
        hotbar(server.getPlayerList().getPlayers().getFirst());
    }

    private static int glyph(char c) {
        for (NeonGlyph glyph : NeonGlyph.values()) {
            if (glyph.character() == c) {
                return glyph.ordinal();
            }
        }
        throw new IllegalArgumentException("No neon glyph for " + c);
    }

    /** A board whose left end, as seen from the front, is at x. */
    private static void panel(ServerLevel level, NeonPanelBlock block, int x, int y) {
        BlockState state = block.defaultBlockState().setValue(NeonPanelBlock.FACING, Direction.NORTH);
        for (int part = 0; part < block.width(); part++) {
            level.setBlock(new BlockPos(x - part, y, ON_WALL), state.setValue(NeonPanelBlock.PART, part),
                    Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Commands place blocks without asking them how they join their neighbours, so every block in the room is
     * then updated from its neighbours, as if it had been placed by hand.
     */
    private static void settle(ServerLevel level, BlockPos from, BlockPos to) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                BlockState settled = Block.updateFromNeighbourShapes(state, level, pos);
                if (settled != state) {
                    level.setBlock(pos, settled, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void hotbar(ServerPlayer player) {
        player.getInventory().clearContent();
        List<ItemStack> items = List.of(new ItemStack(ModDecor.NEON_LETTER.get()), new ItemStack(ModDecor.NEON_TUBE.get()),
                new ItemStack(ModDecor.NEON_SIGN_BLOCKS.get(1).get()), new ItemStack(ModDecor.NEON_PANEL_BOLTCRAFT.get()),
                new ItemStack(ModDecor.MARQUEE_LIGHTS.get()), new ItemStack(ModDecor.VELVET_ROPES.get(0).get()),
                new ItemStack(ModDecor.VELVET_SOFA.get()), new ItemStack(ModDecor.CRYSTAL_CHANDELIER.get()),
                new ItemStack(ModDecor.GRAND_CHANDELIER.get()));
        for (int slot = 0; slot < items.size(); slot++) {
            player.getInventory().setItem(slot, items.get(slot));
        }
    }

    static void openDecorTab(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        var screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
        mc.setScreen(screen);
        try {
            Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            select.setAccessible(true);
            select.invoke(screen, ModCreativeTabs.DECOR.get());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot open the decor tab", e);
        }
    }

    private static void set(MinecraftServer server, int x, int y, int z, String block) {
        String id = block.contains(":") ? block : "boltcraft_casino:" + block;
        DevClient.command(server, String.format(Locale.ROOT, "setblock %d %d %d %s", x, y, z, id));
    }

    private static void fill(MinecraftServer server, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        DevClient.command(server, String.format(Locale.ROOT, "fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2,
                block));
    }
}
