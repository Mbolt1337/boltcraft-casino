package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.client.render.RouletteTableRenderer;
import com.mbolt.casino.client.screen.RouletteScreen;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.entity.CroupierEntity;
import com.mbolt.casino.game.CasinoRandom;
import com.mbolt.casino.game.PendingPayouts;
import com.mbolt.casino.game.RouletteRounds;
import com.mbolt.casino.game.roulette.RouletteBet;
import com.mbolt.casino.game.roulette.RouletteRound;
import com.mbolt.casino.game.roulette.RouletteWheel;
import com.mbolt.casino.inventory.ChipWallet;
import com.mbolt.casino.registry.ModBlocks;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.joml.Vector3f;

/** Opt-in real renderer, menu, network, inventory and croupier checks; never shipped in the mod jar. */
@EventBusSubscriber(modid = CasinoMod.MODID, value = Dist.CLIENT)
public final class RouletteClient {
    private static final BlockPos TABLE = new BlockPos(-1, -60, 0);
    private static final BlockPos VAULT = new BlockPos(-4, -60, 0);
    private static final List<String> checks = new ArrayList<>();
    private static int phase;
    private static int ticks;
    private static int total;
    private static String failure;
    private static boolean waiting;
    private static boolean rendering;
    private static UUID unloadedRound;

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Pre event) {
        if (Boolean.getBoolean("boltcraft.roulette_check") && !rendering) {
            event.setCanceled(true);
            rendering = true;
            try {
                event.getScreen().renderWithTooltip(event.getGuiGraphics(), -1, -1, event.getPartialTick());
            } finally { rendering = false; }
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("boltcraft.roulette_check")) { return; }
        var mc = Minecraft.getInstance();
        try {
            if (++total > 6000 && phase < 99) { throw new IllegalStateException("Timeout phase " + phase); }
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                if (!mc.options.languageCode.equals("ru_ru")) {
                    mc.options.languageCode = "ru_ru";
                    mc.getLanguageManager().setSelected("ru_ru");
                    mc.reloadResourcePacks();
                    return;
                }
                DevClient.prepareOptions(mc);
                if (Boolean.getBoolean("boltcraft.roulette_small")) {
                    mc.options.guiScale().set(2);
                }
                DevClient.createFlatWorld(mc, "BoltCraftRoulette", GameType.CREATIVE, false);
                next(1);
            }
            if (phase == 99 && mc.level == null && mc.getSingleplayerServer() == null
                    && mc.screen instanceof TitleScreen) {
                var path = mc.gameDirectory.toPath().resolve("../diagnostics/roulette-client.txt").normalize();
                String result = failure == null ? "PASS " + checks.size() : "FAIL " + failure;
                Files.writeString(path, result + "\n" + String.join("\n", checks));
                CasinoMod.LOGGER.info("ROULETTE CLIENT {}", result);
                phase = 100;
                mc.stop();
                return;
            }
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null || waiting
                    || mc.getOverlay() != null || phase >= 99) { return; }
            ticks++;
            mc.getToasts().clear();
            run(mc);
        } catch (Exception error) {
            failure = error.toString();
            CasinoMod.LOGGER.error("Roulette real-client failure", error);
            quit(mc);
        }
    }

    private static void run(Minecraft mc) {
        switch (phase) {
            case 1 -> {
                if (ticks > 40) {
                    mc.options.hideGui = true;
                    server(mc, RouletteClient::setup, 2);
                }
            }
            case 2 -> {
                if (ticks > 80) {
                    var clientTable = (RouletteTableBlockEntity) mc.level.getBlockEntity(TABLE);
                    var renderer = (RouletteTableRenderer) mc.getBlockEntityRenderDispatcher().getRenderer(clientTable);
                    renderer.getGeoModel().getBone("wheel").orElseThrow().setTrackingMatrices(true);
                    renderer.getGeoModel().getBone("ball").orElseThrow().setTrackingMatrices(true);
                    mc.options.hideGui = true;
                    DevClient.screenshot(mc, "roulette_day");
                    server(mc, server -> {
                        var table = table(server);
                        var dealers = server.overworld().getEntitiesOfClass(CroupierEntity.class, new AABB(TABLE).inflate(10));
                        check(dealers.size() == 1, "exactly one croupier at the linked table");
                        check(table.isCroupier(dealers.getFirst().getUUID()), "croupier belongs to this table");
                        player(server).interactOn(dealers.getFirst(), InteractionHand.MAIN_HAND);
                    }, 3);
                }
            }
            case 3 -> {
                if (ticks > 20 && mc.screen instanceof RouletteScreen screen && screen.getMenu().state() != null) {
                    check(screen.getMenu().state().balance() == 100, "NPC opens the roulette menu with the real balance");
                    DevClient.screenshot(mc, "roulette_menu_idle");
                    DevClient.press(mc, Component.literal("5"), 2);
                    DevClient.press(mc, Component.literal("0"), 1);
                    next(4);
                }
            }
            case 4 -> {
                if (ticks > 15 && mc.screen instanceof RouletteScreen screen && screen.getMenu().state().balance() == 95) {
                    check(ChipWallet.balance(mc.player) == 95, "a real packet debits one five-chip stake");
                    server(mc, server -> {
                        var guest = FakePlayerFactory.get(server.overworld(), new GameProfile(UUID.randomUUID(), "Guest"));
                        guest.setPos(TABLE.getCenter());
                        ChipWallet.give(guest, 100);
                        table(server).linkedVault().orElseThrow().ledger().deposit("minecraft:diamond", 10, true,
                                CasinoServerConfig.rates(), guest.getUUID(), System.currentTimeMillis());
                        var refusal = RouletteRounds.get(server).place(table(server), guest,
                                new RouletteBet(RouletteBet.Type.RED, 0, 10));
                        check(refusal == null, "a second player joins the same round");
                        long seed = 0;
                        do { CasinoRandom.reseed(++seed); } while (CasinoRandom.nextInt(37) != 0);
                        CasinoRandom.reseed(seed);
                    }, 5);
                }
            }
            case 5 -> {
                if (ticks == 15) { DevClient.screenshot(mc, "roulette_menu_betting"); }
                var table = (RouletteTableBlockEntity) mc.level.getBlockEntity(TABLE);
                if (table.round() != null && table.round().snapshot().phase() == RouletteRound.Phase.SPINNING) {
                    check(table.round().snapshot().bets().size() == 2, "both stakes reach nearby client block state");
                    check(table.round().snapshot().number() == 0, "the persisted server result is zero");
                    mc.player.closeContainer();
                    next(6);
                }
            }
            case 6 -> {
                if (ticks == 50) { DevClient.screenshot(mc, "roulette_spinning"); }
                var table = (RouletteTableBlockEntity) mc.level.getBlockEntity(TABLE);
                if (table.round().snapshot().phase() == RouletteRound.Phase.PAYOUT && ticks > 170) {
                    check(ChipWallet.balance(mc.player) == 275, "100 - 5 + 180 = 275 chips delivered");
                    var renderer = (RouletteTableRenderer) mc.getBlockEntityRenderDispatcher().getRenderer(table);
                    var wheel = renderer.getGeoModel().getBone("wheel").orElseThrow();
                    var ball = renderer.getGeoModel().getBone("ball").orElseThrow();
                    double angle = Math.toRadians(RouletteWheel.pocketAngle(0));
                    double radius = RouletteWheel.pocketRadius(0);
                    Vector3f expected = new Vector3f((float) (-Math.sin(angle) * radius / 16), .5f / 16,
                            (float) (-Math.cos(angle) * radius / 16))
                            .mulPosition(wheel.getModelSpaceMatrix());
                    Vector3f actual = ball.getModelSpaceMatrix().getTranslation(new Vector3f());
                    check(expected.distance(actual) < .002, "rendered ball pivot lands in the actual zero pocket");
                    DevClient.screenshot(mc, "roulette_landed_zero");
                    server(mc, server -> {
                        check(table(server).linkedVault().orElseThrow().ledger().reserved() == 0, "bank reserve released");
                        check(PendingPayouts.get(server).owedTo(player(server).getUUID()) == 0, "no unpaid duplicate");
                        DevClient.command(server, "time set midnight");
                    }, 7);
                }
            }
            case 7 -> {
                if (ticks > 65) {
                    DevClient.screenshot(mc, "roulette_night");
                    server(mc, server -> table(server).openMenu(player(server)), 8);
                }
            }
            case 8 -> {
                if (ticks > 20 && mc.screen instanceof RouletteScreen) {
                    check(ChipWallet.balance(mc.player) == 275, "completed round never pays twice");
                    DevClient.screenshot(mc, "roulette_menu_result");
                    server(mc, server -> {
                        player(server).closeContainer();
                        DevClient.command(server, "time set noon");
                        player(server).connection.teleport(0, -57.8, 1, 0, 90);
                    }, 9);
                }
            }
            case 9 -> {
                if (ticks > 20) {
                    DevClient.screenshot(mc, "roulette_top_zero");
                    if (Boolean.getBoolean("boltcraft.roulette_chunks")) {
                        server(mc, server -> {
                            var refusal = RouletteRounds.get(server).place(table(server), player(server),
                                    new RouletteBet(RouletteBet.Type.NUMBER, 0, 5));
                            check(refusal == null, "a new round accepts a stake before chunk unloading");
                            unloadedRound = table(server).roundId();
                            long seed = 0;
                            do { CasinoRandom.reseed(++seed); } while (CasinoRandom.nextInt(37) != 0);
                            CasinoRandom.reseed(seed);
                            DevClient.command(server, "gamerule spawnChunkRadius 0");
                            player(server).connection.teleport(4096, -55, 4096, 0, 0);
                        }, 10);
                    } else {
                        quit(mc);
                    }
                }
            }
            case 10 -> {
                if (ticks > 350) {
                    server(mc, server -> {
                        var chunks = server.overworld().getChunkSource();
                        check(chunks.getChunkNow(TABLE.getX() >> 4, TABLE.getZ() >> 4) == null,
                                "the real table chunk unloads before betting closes");
                        check(chunks.getChunkNow(VAULT.getX() >> 4, VAULT.getZ() >> 4) == null,
                                "the real bank chunk unloads during the same round");
                    }, 11);
                }
            }
            case 11 -> {
                if (ticks > 320) {
                    server(mc, server -> {
                        var saved = RouletteRounds.get(server).find(unloadedRound).snapshot();
                        check(saved.number() == 0 && saved.phase() == RouletteRound.Phase.PAYOUT && !saved.settled(),
                                "an unloaded round chooses once and waits for its bank before settlement");
                        player(server).connection.teleport(2, -59, -2.6, 0, 25);
                    }, 12);
                }
            }
            case 12 -> {
                if (ticks > 140) {
                    server(mc, server -> {
                        check(ChipWallet.balance(player(server)) == 450,
                                "returning to unloaded chunks delivers 180 chips exactly once");
                        check(table(server).round().snapshot().number() == 0
                                        && table(server).round().snapshot().phase() == RouletteRound.Phase.COMPLETE,
                                "the reloaded table receives the completed authoritative result");
                        check(table(server).linkedVault().orElseThrow().ledger().reserved() == 0,
                                "chunk reload releases the bank reservation");
                        var dealers = server.overworld().getEntitiesOfClass(CroupierEntity.class,
                                new AABB(TABLE).inflate(10));
                        check(dealers.size() == 1, "chunk reload retains exactly one croupier");
                    }, 13);
                }
            }
            case 13 -> quit(mc);
        }
    }

    private static void setup(MinecraftServer server) {
        DevClient.command(server, "gamerule doDaylightCycle false");
        DevClient.command(server, "time set noon");
        DevClient.command(server, "fill -8 -61 -6 8 -61 6 minecraft:dark_oak_planks");
        var level = server.overworld();
        level.setBlock(VAULT, ModBlocks.VAULT.get().defaultBlockState(), 3);
        var state = ModBlocks.ROULETTE_TABLE.get().defaultBlockState();
        level.setBlock(TABLE, state, 2);
        ModBlocks.ROULETTE_TABLE.get().setPlacedBy(level, TABLE, state, player(server), ItemStack.EMPTY);
        var vault = (VaultBlockEntity) level.getBlockEntity(VAULT);
        vault.setOwner(player(server));
        vault.ledger().deposit("minecraft:diamond", 500, false, CasinoServerConfig.rates(), player(server).getUUID(), 0);
        vault.ledger().deposit("minecraft:diamond", 10, true, CasinoServerConfig.rates(), player(server).getUUID(), 1);
        vault.ledgerChanged();
        table(server).linkTo(vault);
        player(server).getInventory().clearContent();
        ChipWallet.give(player(server), 100);
        player(server).getAbilities().flying = true;
        player(server).onUpdateAbilities();
        Vec3 foot = new Vec3(2, -59.0, -2.6);
        Vec3 target = new Vec3(0, -59.1, 1);
        Vec3 delta = target.subtract(foot.add(0, player(server).getEyeHeight(), 0));
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
        player(server).connection.teleport(foot.x, foot.y, foot.z, yaw, pitch);
    }

    private static ServerPlayer player(MinecraftServer server) { return server.getPlayerList().getPlayers().getFirst(); }
    private static RouletteTableBlockEntity table(MinecraftServer server) {
        return (RouletteTableBlockEntity) server.overworld().getBlockEntity(TABLE);
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
        checks.add("PASS " + message);
    }
    private static void next(int value) { phase = value; ticks = 0; }
    private static void server(Minecraft mc, Consumer<MinecraftServer> action, int next) {
        waiting = true;
        mc.getSingleplayerServer().execute(() -> {
            try {
                action.accept(mc.getSingleplayerServer());
                mc.execute(() -> { waiting = false; next(next); });
            } catch (Exception error) {
                mc.execute(() -> { failure = error.toString(); waiting = false; quit(mc); });
            }
        });
    }
    private static void quit(Minecraft mc) {
        next(99);
        if (mc.level != null) { mc.level.disconnect(); mc.disconnect(new TitleScreen()); }
    }
}
