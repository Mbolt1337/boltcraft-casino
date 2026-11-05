package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.decor.TabletopSupport;
import com.mbolt.casino.block.entity.ServingTrayBlockEntity;
import com.mbolt.casino.client.screen.GuideBookScreen;
import com.mbolt.casino.entity.SeatEntity;
import com.mbolt.casino.registry.ModDecor;
import com.mbolt.casino.registry.ModItems;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Real-client visual review of the existing catalogue, seated player, dismount and broken furniture. */
@EventBusSubscriber(modid = CasinoMod.MODID, value = Dist.CLIENT)
public final class FurnitureClient {
    private static final BlockPos SOFA = new BlockPos(-4, -60, -10);
    private static final BlockPos STOOL = new BlockPos(1, -60, -10);
    private static final BlockPos CHAIR = new BlockPos(-1, -60, -10);
    private static final List<ArtPreview.Shot> VIEWS = DecorPreview.SHOTS.stream()
            .filter(shot -> !shot.hud() && !shot.name().contains("night")).toList();
    private static final List<String> checks = new ArrayList<>();
    private static int phase;
    private static int ticks;
    private static int view;
    private static boolean waiting;
    private static Entity camera;
    private static int bookSpread;
    private static boolean rendering;

    private FurnitureClient() {
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Pre event) {
        if (!Boolean.getBoolean("boltcraft.furniture_check") || rendering) {
            return;
        }
        event.setCanceled(true);
        rendering = true;
        try {
            event.getScreen().renderWithTooltip(event.getGuiGraphics(), -1, -1, event.getPartialTick());
        } finally {
            rendering = false;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("boltcraft.furniture_check")) {
            return;
        }
        var mc = Minecraft.getInstance();
        try {
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                if (!mc.options.languageCode.equals("ru_ru")) {
                    mc.options.languageCode = "ru_ru";
                    mc.getLanguageManager().setSelected("ru_ru");
                    mc.reloadResourcePacks();
                    return;
                }
                DevClient.prepareOptions(mc);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                DevClient.createFlatWorld(mc, "BoltCraftFurniture", GameType.CREATIVE, true);
                phase = 1;
            }
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null || waiting
                    || mc.getOverlay() != null || phase >= 99) {
                return;
            }
            ticks++;
            mc.options.hideGui = true;
            mc.getToasts().clear();
            run(mc);
        } catch (Exception error) {
            CasinoMod.LOGGER.error("Furniture client check failed", error);
            finish(mc, "FAIL " + error);
        }
    }

    private static void run(Minecraft mc) {
        switch (phase) {
            case 1 -> {
                if (ticks > 40) {
                    server(mc, server -> {
                        DecorPreview.build(server);
                        DevClient.command(server, "fill -9 -61 -15 6 -61 -7 minecraft:dark_oak_planks");
                        var level = server.overworld();
                        level.setBlock(SOFA, ModDecor.VELVET_SOFA.get().defaultBlockState(), 3);
                        level.setBlock(STOOL, ModDecor.BAR_STOOL.get().defaultBlockState(), 3);
                        level.setBlock(CHAIR, ModDecor.DINING_CHAIR.get().defaultBlockState(), 3);
                        for (int x : new int[]{4, 6}) {
                            var tablePos = new BlockPos(x, -60, -10);
                            level.setBlock(tablePos, ModDecor.COFFEE_TABLE.get().defaultBlockState(), 3);
                            Block prop = x == 4 ? ModDecor.SERVING_TRAY.get() : ModDecor.TABLE_LAMP.get();
                            level.setBlock(tablePos.above(), prop.defaultBlockState()
                                    .setValue(TabletopSupport.LOWERING, 8), 3);
                        }
                        var tray = (ServingTrayBlockEntity) level.getBlockEntity(new BlockPos(4, -59, -10));
                        for (int slot = 0; slot < 4; slot++) {
                            tray.setItem(slot, ModItems.barMenu().get(slot).getDefaultInstance());
                        }
                        DevClient.command(server, "fill 3 -60 -8 7 -57 -8 minecraft:dark_oak_planks");
                        level.setBlock(new BlockPos(5, -58, -9), ModDecor.BOTTLE_SHELF.get().defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 3);
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.getInventory().clearContent();
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    }, 2);
                }
            }
            case 2 -> {
                if (ticks == 40) {
                    var shot = VIEWS.get(view);
                    camera(mc, shot.eye(), shot.target());
                }
                if (ticks > 65) {
                    DevClient.screenshot(mc, "review_" + VIEWS.get(view).name());
                    view++;
                    if (view == VIEWS.size()) {
                        server(mc, server -> mount(server, SOFA), 3);
                    } else {
                        ticks = 39;
                    }
                }
            }
            case 3 -> {
                if (ticks == 20) {
                    camera(mc, new Vec3(-6, -58.6, -13.5), new Vec3(-3.5, -59.5, -9.5));
                }
                if (ticks > 50) {
                    check(mc.player.getVehicle() instanceof SeatEntity, "the real client renders the sofa rider");
                    DevClient.screenshot(mc, "seat_sofa");
                    server(mc, server -> {
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.stopRiding();
                        mount(server, STOOL);
                    }, 4);
                }
            }
            case 4 -> {
                if (ticks == 20) {
                    camera(mc, new Vec3(-1, -58.1, -13.5), new Vec3(1.5, -59.1, -9.5));
                }
                if (ticks > 50) {
                    check(mc.player.getVehicle() instanceof SeatEntity, "the client can switch to the bar stool");
                    DevClient.screenshot(mc, "seat_stool");
                    server(mc, server -> server.overworld().destroyBlock(STOOL, false), 5);
                }
            }
            case 5 -> {
                if (ticks > 40) {
                    check(!mc.player.isPassenger(), "breaking the stool dismounts the real client");
                    DevClient.screenshot(mc, "seat_removed");
                    server(mc, server -> mount(server, CHAIR), 6);
                }
            }
            case 6 -> {
                if (ticks == 20) {
                    camera(mc, new Vec3(-3, -58.4, -13.5), new Vec3(-.5, -59.35, -9.5));
                }
                if (ticks > 45) {
                    check(mc.player.getVehicle() instanceof SeatEntity, "the dining chair works in the real client");
                    DevClient.screenshot(mc, "seat_dining_chair");
                    server(mc, server -> {
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.getInventory().setItem(0, new ItemStack(ModItems.SPICED_COCOA.get()));
                        player.getInventory().selected = 0;
                        player.containerMenu.broadcastChanges();
                    }, 7);
                }
            }
            case 7 -> {
                if (ticks == 20) {
                    mc.player.getInventory().selected = 0;
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    mc.options.keyUse.setDown(true);
                }
                if (ticks == 35) {
                    DevClient.screenshot(mc, "seated_drinking_cocoa");
                }
                if (ticks > 75) {
                    mc.options.keyUse.setDown(false);
                    server(mc, server -> {
                        var player = server.getPlayerList().getPlayers().getFirst();
                        check(player.getInventory().countItem(ModItems.SPICED_COCOA.get()) == 0,
                                "the real use-item packet consumes one cocoa");
                        check(player.getInventory().countItem(ModItems.CERAMIC_CUP.get()) == 1,
                                "drinking returns exactly one cup");
                        player.stopRiding();
                        player.setGameMode(GameType.CREATIVE);
                    }, 8);
                }
            }
            case 8 -> {
                if (ticks == 15) {
                    camera(mc, new Vec3(2, -57.7, -13.8), new Vec3(5.0, -59.0, -9));
                }
                if (ticks > 40) {
                    DevClient.screenshot(mc, "hospitality_day");
                    server(mc, server -> DevClient.command(server, "time set midnight"), 9);
                }
            }
            case 9 -> {
                if (ticks > 45) {
                    DevClient.screenshot(mc, "hospitality_night");
                    DecorPreview.openDecorTab(mc);
                    phase = 10;
                    ticks = 0;
                }
            }
            case 10 -> {
                DevClient.parkCursor(mc);
                if (ticks == 30) {
                    DevClient.screenshot(mc, "decor_inventory_top");
                    ((CreativeModeInventoryScreen) mc.screen).getMenu().scrollTo(1);
                }
                if (ticks > 50) {
                    DevClient.screenshot(mc, "decor_inventory_bottom");
                    mc.setScreen(null);
                    server(mc, server -> {
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.getInventory().clearContent();
                        player.getInventory().selected = 0;
                        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CANAPE.get(), 2));
                        player.connection.teleport(4.5, -60, -12, 0, 0);
                        var tray = (ServingTrayBlockEntity) server.overworld()
                                .getBlockEntity(new BlockPos(4, -59, -10));
                        tray.removeItem(0, 1);
                        player.containerMenu.broadcastChanges();
                    }, 12);
                }
            }
            case 11 -> {
                if (ticks > 15) {
                    DevClient.screenshot(mc, "guide_hospitality_spread_" + bookSpread);
                    if (++bookSpread == 16) {
                        finish(mc, "PASS");
                    } else {
                        ((GuideBookScreen) mc.screen).turn(1);
                        ticks = 0;
                    }
                }
            }
            case 12 -> {
                if (ticks == 20) {
                    useTrayThroughTable(mc);
                }
                if (ticks > 50) {
                    server(mc, server -> {
                        var player = server.getPlayerList().getPlayers().getFirst();
                        var tray = (ServingTrayBlockEntity) server.overworld()
                                .getBlockEntity(new BlockPos(4, -59, -10));
                        check(tray.getItem(0).is(ModItems.CANAPE.get())
                                        && player.getInventory().countItem(ModItems.CANAPE.get()) == 1,
                                "a real support-click packet serves exactly one item on the low table");
                    }, 13);
                }
            }
            case 13 -> {
                if (ticks == 20) {
                    mc.player.getInventory().selected = 1;
                    useTrayThroughTable(mc);
                }
                if (ticks > 50) {
                    server(mc, server -> {
                        var player = server.getPlayerList().getPlayers().getFirst();
                        var tray = (ServingTrayBlockEntity) server.overworld()
                                .getBlockEntity(new BlockPos(4, -59, -10));
                        check(tray.getItem(0).isEmpty()
                                        && player.getInventory().countItem(ModItems.CANAPE.get()) == 2,
                                "a real empty-hand support-click packet returns that serving exactly once");
                    }, 14);
                }
            }
            case 14 -> {
                mc.setScreen(new GuideBookScreen());
                phase = 11;
                ticks = 0;
            }
        }
    }

    private static void useTrayThroughTable(Minecraft mc) {
        var hit = new BlockHitResult(new Vec3(4.25, -59.5, -9.75), Direction.UP,
                new BlockPos(4, -60, -10), false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
    }

    private static void mount(MinecraftServer server, BlockPos pos) {
        var player = server.getPlayerList().getPlayers().getFirst();
        player.getInventory().clearContent();
        player.connection.teleport(pos.getX() + .5, pos.getY(), pos.getZ() - 1, 180, 0);
        SeatEntity.sit(server.overworld(), pos, server.overworld().getBlockState(pos), player);
        check(player.getVehicle() instanceof SeatEntity, "server mounts the player at " + pos.toShortString());
    }

    private static void camera(Minecraft mc, Vec3 eye, Vec3 target) {
        if (camera == null) {
            camera = EntityType.ARMOR_STAND.create(mc.level);
        }
        Vec3 delta = target.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
        camera.moveTo(eye.x, eye.y - camera.getEyeHeight(), eye.z, yaw, pitch);
        camera.setOldPosAndRot();
        if (camera instanceof LivingEntity living) {
            living.yHeadRot = yaw;
            living.yHeadRotO = yaw;
            living.yBodyRot = yaw;
            living.yBodyRotO = yaw;
        }
        mc.setCameraEntity(camera);
    }

    private static void server(Minecraft mc, Consumer<MinecraftServer> action, int next) {
        waiting = true;
        mc.getSingleplayerServer().execute(() -> {
            try {
                action.accept(mc.getSingleplayerServer());
                mc.execute(() -> {
                    phase = next;
                    ticks = 0;
                    waiting = false;
                });
            } catch (Exception error) {
                mc.execute(() -> finish(mc, "FAIL " + error));
            }
        });
    }

    private static void check(boolean value, String message) {
        if (!value) {
            throw new IllegalStateException(message);
        }
        checks.add("PASS " + message);
    }

    private static void finish(Minecraft mc, String result) {
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("../diagnostics/furniture-client.txt").normalize(),
                    result + " " + checks.size() + "\n" + String.join("\n", checks));
        } catch (Exception error) {
            CasinoMod.LOGGER.error("Could not write furniture client results", error);
        }
        phase = 99;
        mc.setCameraEntity(mc.player);
        mc.stop();
    }
}
