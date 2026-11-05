package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.CasinoText;
import com.mbolt.casino.block.RouletteTableBlock;
import com.mbolt.casino.block.entity.CashierBlockEntity;
import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.CasinoRandom;
import com.mbolt.casino.game.RouletteRounds;
import com.mbolt.casino.game.SlotRounds;
import com.mbolt.casino.game.roulette.RouletteBet;
import com.mbolt.casino.game.slot.SlotMachineLogic;
import com.mbolt.casino.game.slot.SlotSymbol;
import com.mbolt.casino.registry.ModBlocks;
import com.mbolt.casino.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Development tool, never shipped: {@code gradlew runClient -PartPreview} builds a small showroom with every casino
 * block next to vanilla blocks, takes screenshots from fixed viewpoints into {@code run-preview/screenshots} and
 * closes the game. Used to review models and screens in the real renderer before showing them.
 */
@EventBusSubscriber(modid = CasinoMod.MODID, value = Dist.CLIENT)
public final class ArtPreview {
    private static final String WORLD = "BoltCraftPreview";
    /**
     * Russian texts are the longest, so screens that fit them fit every language; the public page is filmed in
     * English ({@code -PpreviewLanguage=en_us}).
     */
    private static final String LANGUAGE = System.getProperty("boltcraft.preview_language", "ru_ru");
    private static final int FLOOR_Y = -61;
    private static final int SETTLE_TICKS = 60;
    private static final int SHOT_TICKS = 30;
    private static final int NIGHT_TICKS = 40;
    static final BlockPos SLOT_MACHINE = new BlockPos(4, FLOOR_Y + 1, 0);
    private static final BlockPos SLOT_VAULT = new BlockPos(-2, FLOOR_Y + 1, 0);
    private static final BlockPos CASHIER = new BlockPos(2, FLOOR_Y + 1, 0);
    /** Front right block of the roulette table as seen by a player; the table covers x 12-13, z -2..-1. */
    private static final BlockPos ROULETTE = new BlockPos(12, FLOOR_Y + 1, -2);
    /** The number the preview spin lands on: red, next to the 0 neither way round, so a wrong turn shows. */
    private static final int ROULETTE_RESULT = 19;
    private static final int BACKING_DIAMONDS = 250;

    /** Lever animation peaks between 4 and 6 ticks after the pull. */
    private static final int LEVER_TICKS = 5;

    /**
     * Each shot: file name, camera position, point looked at, HUD shown, the hotbar slot held (-1 keeps it), ticks
     * until the picture and what happens when the camera arrives.
     */
    record Shot(String name, Vec3 eye, Vec3 target, boolean hud, int slot, int ticks,
                        Consumer<Minecraft> action) {
        Shot(String name, Vec3 eye, Vec3 target, boolean hud) {
            this(name, eye, target, hud, -1, SHOT_TICKS, mc -> {});
        }

        Shot(String name, Vec3 eye, Vec3 target, boolean hud, int slot) {
            this(name, eye, target, hud, slot, SHOT_TICKS, mc -> {});
        }

        Shot(String name, Vec3 eye, Vec3 target, int ticks) {
            this(name, eye, target, false, -1, ticks, mc -> {});
        }

        Shot(String name, Vec3 eye, Vec3 target, int ticks, Consumer<Minecraft> action) {
            this(name, eye, target, false, -1, ticks, action);
        }

        /** A shot of a screen, taken from where every casino block is close enough to use. */
        static Shot screen(String name, int ticks, Consumer<Minecraft> action) {
            return new Shot(name, SCREEN_EYE, SCREEN_TARGET, true, -1, ticks, action);
        }
    }

    private static final Vec3 OVERVIEW_EYE = new Vec3(0.5, -58.6, -6.5);
    private static final Vec3 OVERVIEW_TARGET = new Vec3(0.5, -59.6, 0.5);
    private static final Vec3 VAULT_EYE = new Vec3(-4.2, -58.9, -2.2);
    private static final Vec3 VAULT_TARGET = new Vec3(-1.5, -59.6, 0.5);
    private static final Vec3 CASHIER_EYE = new Vec3(4.9, -58.8, -2.0);
    private static final Vec3 CASHIER_TARGET = new Vec3(2.5, -59.6, 0.5);
    private static final Vec3 SLOT_EYE = new Vec3(6.6, -58.0, -2.0);
    private static final Vec3 SLOT_TARGET = new Vec3(4.5, -59.0, 0.5);
    private static final Vec3 LEVER_EYE = new Vec3(2.4, -58.6, -1.2);
    private static final Vec3 LEVER_TARGET = new Vec3(4.3, -59.0, 0.5);
    private static final Vec3 REELS_EYE = new Vec3(4.5, -58.69, -0.9);
    private static final Vec3 REELS_TARGET = new Vec3(4.5, -58.69, 0.5);
    private static final Vec3 POUR_EYE = new Vec3(6.4, -58.9, -2.6);
    private static final Vec3 POUR_TARGET = new Vec3(4.5, -59.6, -0.4);
    private static final Vec3 TABLE_EYE = new Vec3(15.4, -58.2, -3.6);
    private static final Vec3 TABLE_TARGET = new Vec3(13.0, -59.5, -0.6);
    private static final Vec3 TABLE_FRONT_EYE = new Vec3(13.0, -58.4, -3.4);
    private static final Vec3 TABLE_FRONT_TARGET = new Vec3(13.0, -59.3, -0.8);
    private static final Vec3 WHEEL_EYE = new Vec3(13.0, -57.7, -1.25);
    private static final Vec3 WHEEL_TARGET = new Vec3(13.0, -59.15, -0.5);
    private static final Vec3 LAYOUT_EYE = new Vec3(13.0, -58.3, -2.7);
    private static final Vec3 LAYOUT_TARGET = new Vec3(13.0, -59.2, -1.45);
    private static final Vec3 CROUPIER_EYE = new Vec3(14.2, -58.3, -2.9);
    private static final Vec3 CROUPIER_TARGET = new Vec3(13.0, -58.9, 0.0);
    private static final Vec3 SCREEN_EYE = new Vec3(1.0, -58.4, -2.0);
    private static final Vec3 SCREEN_TARGET = new Vec3(1.0, -59.5, 0.5);

    /*
     * Slot machine timing with the default 60-tick spin: the reels stop 39, 48 and 57 ticks after the pull, the
     * round settles at 60 and the next pull is allowed at 60 as well. Winnings then pour out for 20 ticks, or for
     * 60 on a jackpot.
     */
    private static final List<Shot> SHOTS = List.of(
            new Shot("overview", OVERVIEW_EYE, OVERVIEW_TARGET, false),
            new Shot("vault_closed", VAULT_EYE, VAULT_TARGET, false),
            new Shot("vault_open", new Vec3(2.8, -58.9, -2.4), new Vec3(0.5, -59.6, 0.5), false),
            new Shot("vault_front", new Vec3(-1.5, -59.5, -2.0), new Vec3(-1.5, -59.5, 0.5), false),
            new Shot("cashier", CASHIER_EYE, CASHIER_TARGET, false),
            new Shot("cashier_front", new Vec3(2.5, -59.2, -1.6), new Vec3(2.5, -59.5, 0.5), false),
            new Shot("cashier_teller_side", new Vec3(0.6, -58.7, 2.6), new Vec3(2.5, -59.6, 0.5), false),
            new Shot("hotbar", OVERVIEW_EYE, OVERVIEW_TARGET, true),
            new Shot("hand_key", OVERVIEW_EYE, OVERVIEW_TARGET, true, 3),
            new Shot("hand_chips", OVERVIEW_EYE, OVERVIEW_TARGET, true, 6),
            new Shot("hand_slot_machine", OVERVIEW_EYE, OVERVIEW_TARGET, true, 2, SHOT_TICKS,
                    onServer(ArtPreview::holdSlotMachineInOffhand)),
            new Shot("slot", SLOT_EYE, SLOT_TARGET, false),
            new Shot("slot_front", new Vec3(4.5, -59.0, -1.9), new Vec3(4.5, -59.0, 0.5), false),
            new Shot("slot_lever_side", LEVER_EYE, LEVER_TARGET, false),
            // A spin whose reels land on symbols that look different when turned the wrong way.
            new Shot("slot_lever_pulled", LEVER_EYE, LEVER_TARGET, LEVER_TICKS,
                    onServer(server -> spin(server, ArtPreview::revealsReelDirection))),
            new Shot("slot_reels_spinning", REELS_EYE, REELS_TARGET, 30),
            new Shot("slot_reels_stopped", REELS_EYE, REELS_TARGET, 30),
            new Shot("slot_win_pull", POUR_EYE, POUR_TARGET, LEVER_TICKS,
                    onServer(server -> spin(server, ArtPreview::smallWin))),
            new Shot("slot_win_pour", POUR_EYE, POUR_TARGET, 65),
            new Shot("slot_jackpot_pull", SLOT_EYE, SLOT_TARGET, LEVER_TICKS,
                    onServer(server -> spin(server, SlotRounds::isJackpot))),
            new Shot("slot_jackpot_reels", REELS_EYE, REELS_TARGET, 62),
            new Shot("slot_jackpot", SLOT_EYE, SLOT_TARGET, 18),
            new Shot("slot_jackpot_pour", POUR_EYE, POUR_TARGET, 20),
            new Shot("slot_jackpot_pile", POUR_EYE, POUR_TARGET, 25),
            // Roulette, timed from the first bet: betting closes after 100 ticks, the spin lasts 160, the
            // payout 60. The croupier throws the ball as the spin starts and waves at tick 40 of it.
            new Shot("roulette", TABLE_EYE, TABLE_TARGET, false),
            new Shot("roulette_front", TABLE_FRONT_EYE, TABLE_FRONT_TARGET, false),
            new Shot("roulette_wheel_rest", WHEEL_EYE, WHEEL_TARGET, false),
            new Shot("roulette_bets", LAYOUT_EYE, LAYOUT_TARGET, 25, onServer(ArtPreview::placeRouletteBets)),
            new Shot("gui_roulette_betting", TABLE_FRONT_EYE, TABLE_FRONT_TARGET, true, -1, 25,
                    onServer(server -> open(server, ROULETTE))),
            new Shot("roulette_launch", CROUPIER_EYE, CROUPIER_TARGET, 62, ArtPreview::closeScreen),
            new Shot("roulette_spinning", TABLE_EYE, TABLE_TARGET, 33),
            new Shot("roulette_ball_drop", WHEEL_EYE, WHEEL_TARGET, 55),
            new Shot("roulette_result", WHEEL_EYE, WHEEL_TARGET, 65),
            new Shot("roulette_announce", CROUPIER_EYE, CROUPIER_TARGET, 15),
            new Shot("roulette_marker", LAYOUT_EYE, LAYOUT_TARGET, 20),
            new Shot("gui_roulette_result", TABLE_FRONT_EYE, TABLE_FRONT_TARGET, true, -1, 25,
                    onServer(server -> open(server, ROULETTE))),
            new Shot("roulette_after", TABLE_FRONT_EYE, TABLE_FRONT_TARGET, 20, ArtPreview::closeScreen),
            // The same blocks at midnight: glowing parts must stand out, the rest must not turn black.
            new Shot("night_overview", OVERVIEW_EYE, OVERVIEW_TARGET, NIGHT_TICKS,
                    onServer(server -> DevClient.command(server, "time set midnight"))),
            new Shot("night_slot", SLOT_EYE, SLOT_TARGET, false),
            new Shot("night_vault", VAULT_EYE, VAULT_TARGET, false),
            new Shot("night_cashier", CASHIER_EYE, CASHIER_TARGET, false),
            new Shot("night_roulette", TABLE_EYE, TABLE_TARGET, false),
            Shot.screen("gui_cashier", SHOT_TICKS, onServer(server -> open(server, CASHIER))),
            Shot.screen("gui_cashier_exchanged", SHOT_TICKS,
                    mc -> DevClient.press(mc, CasinoText.gui("to_chips"), 1)),
            Shot.screen("gui_vault", SHOT_TICKS, onServer(server -> open(server, SLOT_VAULT))),
            Shot.screen("gui_vault_journal", SHOT_TICKS,
                    mc -> DevClient.press(mc, CasinoText.gui("tab_journal"), 1)),
            Shot.screen("gui_slot", SHOT_TICKS, onServer(server -> open(server, SLOT_MACHINE))),
            Shot.screen("gui_slot_spinning", 25, mc -> DevClient.press(mc, CasinoText.gui("spin"), 1)),
            Shot.screen("gui_slot_stopped", 50, mc -> {}));

    /** {@code -PpreviewScene=decor} or {@code structures} shows the decor showroom or the casinos in the world. */
    private static final String SCENE = System.getProperty("boltcraft.preview_scene", "games");

    private static boolean started;
    private static int setupTicks = -1;
    private static int shot;
    private static int shotTicks;
    private static boolean sceneReady;
    private static boolean sceneFailed;

    private ArtPreview() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!DevClient.preview()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (sceneFailed) {
            return;
        }
        if (!started && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            if (!mc.options.languageCode.equals(LANGUAGE)) {
                mc.options.languageCode = LANGUAGE;
                mc.getLanguageManager().setSelected(LANGUAGE);
                mc.reloadResourcePacks();
                return;
            }
            started = true;
            DevClient.prepareOptions(mc);
            DevClient.createFlatWorld(mc, WORLD, GameType.CREATIVE, true);
            return;
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null || mc.getOverlay() != null) {
            return;
        }
        if (setupTicks < 0) {
            setupTicks = 0;
            mc.options.hideGui = true;
            writeResult(mc, "RUNNING " + SCENE);
            server.execute(() -> {
                try {
                    switch (SCENE) {
                        case "decor" -> DecorPreview.build(server);
                        case "structures" -> StructurePreview.build(server);
                        case "promo" -> PromoPreview.build(server);
                        case "promo_world" -> StructurePreview.build(server);
                        default -> buildShowroom(server);
                    }
                    mc.execute(() -> sceneReady = true);
                } catch (Exception error) {
                    CasinoMod.LOGGER.error("Preview scene {} could not be built", SCENE, error);
                    mc.execute(() -> {
                        sceneFailed = true;
                        writeResult(mc, "FAIL " + SCENE + " " + error);
                        mc.stop();
                    });
                }
            });
            return;
        }
        if (!sceneReady) {
            return;
        }
        if (setupTicks < SETTLE_TICKS) {
            setupTicks++;
            return;
        }
        List<Shot> shots = switch (SCENE) {
            case "decor" -> DecorPreview.SHOTS;
            case "structures" -> StructurePreview.SHOTS;
            case "promo" -> PromoPreview.SHOTS;
            case "promo_world" -> PromoPreview.WORLD_SHOTS;
            default -> SHOTS;
        };
        if (shot >= shots.size()) {
            writeResult(mc, "PASS " + SCENE + " " + shots.size());
            mc.stop();
            return;
        }
        Shot current = shots.get(shot);
        if (shotTicks == 0) {
            mc.options.hideGui = !current.hud();
            if (current.slot() >= 0) {
                mc.player.getInventory().selected = current.slot();
            }
            aim(mc, server, current);
            current.action().accept(mc);
        }
        // Opening a screen puts the cursor back in the middle of the window, over the buttons.
        if (mc.screen != null) {
            DevClient.parkCursor(mc);
        }
        if (++shotTicks >= current.ticks()) {
            DevClient.screenshot(mc, current.name());
            shot++;
            shotTicks = 0;
        }
    }

    private static void writeResult(Minecraft mc, String result) {
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("art-preview-result.txt"), result + "\n");
        } catch (IOException error) {
            throw new UncheckedIOException("Cannot save preview result", error);
        }
    }

    /** Casino blocks in a row on a dark floor, vanilla blocks at the ends for scale. */
    static void buildShowroom(MinecraftServer server) {
        DevClient.command(server, "gamerule doDaylightCycle false");
        DevClient.command(server, "gamerule doWeatherCycle false");
        DevClient.command(server, "time set noon");
        DevClient.command(server, "weather clear");
        DevClient.command(server, "fill -9 " + FLOOR_Y + " -8 17 " + FLOOR_Y + " 4 minecraft:dark_oak_planks");
        DevClient.command(server, "fill -9 " + (FLOOR_Y + 1) + " 3 17 " + (FLOOR_Y + 4)
                + " 3 minecraft:deepslate_tiles");
        int y = FLOOR_Y + 1;
        DevClient.command(server, "setblock -6 " + y + " 0 minecraft:chest[facing=north]");
        DevClient.command(server, "setblock -4 " + y + " 0 minecraft:iron_block");
        DevClient.command(server, "setblock -2 " + y + " 0 boltcraft_casino:vault[facing=north]");
        DevClient.command(server, "setblock 0 " + y + " 0 boltcraft_casino:vault[facing=north]");
        DevClient.command(server, "setblock 2 " + y + " 0 boltcraft_casino:cashier[facing=north]");
        DevClient.command(server, "setblock 4 " + y + " 0 boltcraft_casino:slot_machine[facing=north,half=lower]");
        DevClient.command(server, "setblock 4 " + (y + 1)
                + " 0 boltcraft_casino:slot_machine[facing=north,half=upper]");
        DevClient.command(server, "setblock 6 " + y + " 0 minecraft:furnace[facing=north]");
        if (server.overworld().getBlockEntity(new BlockPos(0, y, 0)) instanceof VaultBlockEntity vault) {
            vault.startViewing();
        }
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        if (server.overworld().getBlockEntity(SLOT_VAULT) instanceof VaultBlockEntity vault
                && server.overworld().getBlockEntity(SLOT_MACHINE) instanceof SlotMachineBlockEntity machine) {
            vault.setOwner(player);
            vault.ledger().deposit("minecraft:diamond", BACKING_DIAMONDS, false, CasinoServerConfig.rates(),
                    player.getUUID(), System.currentTimeMillis());
            vault.ledgerChanged();
            machine.setOwner(player);
            machine.linkTo(vault);
            if (server.overworld().getBlockEntity(CASHIER) instanceof CashierBlockEntity cashier) {
                cashier.setOwner(player);
                cashier.linkTo(vault);
            }
            placeRouletteTable(server, player, vault);
        }
        player.getInventory().clearContent();
        List<ItemStack> hotbar = List.of(new ItemStack(ModItems.VAULT.get()), new ItemStack(ModItems.CASHIER.get()),
                new ItemStack(ModItems.SLOT_MACHINE.get()), new ItemStack(ModItems.MANAGER_KEY.get()),
                new ItemStack(ModItems.CHIP_1.get(), 64), new ItemStack(ModItems.CHIP_10.get(), 32),
                new ItemStack(ModItems.CHIP_100.get(), 16), new ItemStack(ModItems.CHIP_1000.get(), 8));
        for (int slot = 0; slot < hotbar.size(); slot++) {
            player.getInventory().setItem(slot, hotbar.get(slot));
        }
        // Resources for the cashier and vault lists.
        player.getInventory().add(new ItemStack(Items.IRON_INGOT, 64));
        player.getInventory().add(new ItemStack(Items.GOLD_INGOT, 40));
        player.getInventory().add(new ItemStack(Items.EMERALD, 16));
        player.getInventory().add(new ItemStack(Items.DIAMOND, 24));
    }

    /** The four parts of a table facing north, owned by the player and banked by the vault. */
    private static void placeRouletteTable(MinecraftServer server, ServerPlayer player, VaultBlockEntity vault) {
        var level = server.overworld();
        var state = ModBlocks.ROULETTE_TABLE.get().defaultBlockState();
        for (int part = 0; part < 4; part++) {
            level.setBlock(RouletteTableBlock.partPos(ROULETTE, Direction.NORTH, part),
                    state.setValue(RouletteTableBlock.PART, part), Block.UPDATE_CLIENTS);
        }
        // Short rounds for the preview; the default is 20 seconds of betting.
        CasinoServerConfig.ROULETTE_BETTING_SECONDS.set(5);
        if (level.getBlockEntity(ROULETTE) instanceof RouletteTableBlockEntity table) {
            table.setOwner(player);
            table.linkTo(vault);
        }
    }

    /** Bets spread over the layout, and a draw rigged to land on {@link #ROULETTE_RESULT}. */
    static void placeRouletteBets(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        if (!(server.overworld().getBlockEntity(ROULETTE) instanceof RouletteTableBlockEntity table)) {
            return;
        }
        var rounds = RouletteRounds.get(server);
        List<RouletteBet> bets = List.of(new RouletteBet(RouletteBet.Type.NUMBER, ROULETTE_RESULT, 10),
                new RouletteBet(RouletteBet.Type.NUMBER, 0, 5), new RouletteBet(RouletteBet.Type.NUMBER, 32, 1),
                new RouletteBet(RouletteBet.Type.RED, 0, 50), new RouletteBet(RouletteBet.Type.DOZEN, 2, 20),
                new RouletteBet(RouletteBet.Type.ODD, 0, 10), new RouletteBet(RouletteBet.Type.HIGH, 0, 100));
        for (RouletteBet bet : bets) {
            var refusal = rounds.place(table, player, bet);
            if (refusal != null) {
                CasinoMod.LOGGER.warn("Preview roulette bet {} refused: {}", bet, refusal.getString());
            }
        }
        long seed = 0;
        do {
            CasinoRandom.reseed(++seed);
        } while (CasinoRandom.nextInt(37) != ROULETTE_RESULT);
        CasinoRandom.reseed(seed);
    }

    private static void closeScreen(Minecraft mc) {
        if (mc.player != null) {
            mc.player.closeContainer();
        }
    }

    /** Runs a task on the integrated server's thread. */
    static Consumer<Minecraft> onServer(Consumer<MinecraftServer> task) {
        return mc -> {
            MinecraftServer server = mc.getSingleplayerServer();
            server.execute(() -> task.accept(server));
        };
    }

    static void open(MinecraftServer server, BlockPos pos) {
        DevClient.useBlock(server.getPlayerList().getPlayers().getFirst(), pos);
    }

    private static void holdSlotMachineInOffhand(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.SLOT_MACHINE.get()));
    }

    /** Pulls the lever so that the outcome passes {@code wanted}, and logs the outcome. */
    static void spin(MinecraftServer server, Predicate<SlotMachineLogic.Result> wanted) {
        DevClient.rigNextSpin(wanted);
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        if (server.overworld().getBlockEntity(SLOT_MACHINE) instanceof SlotMachineBlockEntity machine) {
            var refusal = machine.spin(player, 1);
            CasinoMod.LOGGER.info("Preview spin: {} {} {}, payout {}, refusal {}",
                    SlotSymbol.values()[machine.symbol(0)], SlotSymbol.values()[machine.symbol(1)],
                    SlotSymbol.values()[machine.symbol(2)], machine.payout(),
                    refusal == null ? "none" : refusal.getString());
        }
    }

    private static boolean smallWin(SlotMachineLogic.Result result) {
        return result.payout() > 0 && !SlotRounds.isJackpot(result);
    }

    /**
     * Three different symbols, none of them the first or the fourth face: those two look the same whether a reel
     * turns forwards or backwards, the others do not.
     */
    private static boolean revealsReelDirection(SlotMachineLogic.Result result) {
        int[] symbols = {result.first(), result.second(), result.third()};
        boolean ambiguous = IntStream.of(symbols).anyMatch(symbol -> symbol % 3 == 0);
        return !ambiguous && IntStream.of(symbols).distinct().count() == symbols.length;
    }

    private static void aim(Minecraft mc, MinecraftServer server, Shot current) {
        Vec3 look = current.target().subtract(current.eye());
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(look.y, Math.hypot(look.x, look.z)));
        Vec3 feet = current.eye().subtract(0, mc.player.getEyeHeight(), 0);
        String teleport = String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", feet.x, feet.y, feet.z, yaw,
                pitch);
        server.execute(() -> DevClient.command(server, teleport));
        mc.player.getAbilities().flying = true;
    }
}
