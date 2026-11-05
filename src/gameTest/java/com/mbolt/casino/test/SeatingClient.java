package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.entity.SeatEntity;
import com.mbolt.casino.registry.ModDecor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Actual use-block packets and mouse turns, reviewed from the side in every orientation. Never shipped. */
@EventBusSubscriber(modid = CasinoMod.MODID, value = Dist.CLIENT)
public final class SeatingClient {
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final List<String> observations = new ArrayList<>();
    private static int phase;
    private static int ticks;
    private static int sample;
    private static boolean waiting;
    private static Entity camera;

    private SeatingClient() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("boltcraft.seating_check") || phase == 99) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                DevClient.prepareOptions(mc);
                DevClient.createFlatWorld(mc, "BoltCraftSeating", GameType.SURVIVAL, true);
                phase = 1;
            }
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || waiting) {
                return;
            }
            mc.options.hideGui = true;
            ticks++;
            if (phase == 1 && ticks > 40) {
                prepare(mc);
            } else if (phase == 2 && ticks == 20) {
                BlockPos pos = pos();
                var hit = new BlockHitResult(pos.getCenter(), facing(), pos, false);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            } else if (phase == 2 && ticks == 30) {
                camera(mc);
            } else if (phase == 2 && ticks > 45) {
                if (!(mc.player.getVehicle() instanceof SeatEntity)) {
                    throw new IllegalStateException("Use-block packet did not mount sample " + sample);
                }
                record(mc, "neutral");
                verifyPose(mc, true);
                DevClient.screenshot(mc, "seating_" + sample + "_neutral");
                mc.player.turn(140 / .15, 0);
                phase = 3;
                ticks = 0;
            } else if (phase == 3 && ticks > 25) {
                record(mc, "turned");
                DevClient.screenshot(mc, "seating_" + sample + "_turned");
                sample++;
                prepare(mc);
            } else if (phase == 4 && ticks == 30) {
                camera(mc);
            } else if (phase == 4 && ticks > 65) {
                verifyPose(mc, false);
                DevClient.screenshot(mc, sample == 14 ? "seating_standing_reset" : "seating_vanilla_boat");
                observations.add("PASS " + (sample == 14 ? "standing model resets" : "boat keeps vanilla pose"));
                if (++sample == 16) {
                    finish(mc, "PASS");
                } else {
                    prepare(mc);
                }
            }
        } catch (Exception error) {
            CasinoMod.LOGGER.error("Seating client failed", error);
            finish(mc, "FAIL " + error);
        }
    }

    private static void prepare(Minecraft mc) {
        waiting = true;
        mc.getSingleplayerServer().execute(() -> {
            try {
                var server = mc.getSingleplayerServer();
                var level = server.overworld();
                DevClient.command(server, "gamerule doDaylightCycle false");
                DevClient.command(server, "time set noon");
                DevClient.command(server, "fill -4 -61 -4 26 -61 18 minecraft:stone_bricks");
                var player = server.getPlayerList().getPlayers().getFirst();
                player.stopRiding();
                player.getInventory().clearContent();
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                Block furniture = sample >= 12 ? ModDecor.DINING_CHAIR.get() : switch (sample / 4) {
                    case 0 -> ModDecor.VELVET_SOFA.get();
                    case 1 -> ModDecor.DINING_CHAIR.get();
                    default -> ModDecor.BAR_STOOL.get();
                };
                var state = furniture.defaultBlockState();
                if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                    state = state.setValue(HorizontalDirectionalBlock.FACING, facing());
                }
                level.setBlock(pos(), state, 3);
                var approach = pos().relative(facing(), 2);
                float yaw = sample >= 8 ? facing().toYRot() : facing().getOpposite().toYRot();
                player.connection.teleport(approach.getX() + .5, approach.getY(), approach.getZ() + .5, yaw, 0);
                player.setItemSlot(EquipmentSlot.LEGS, sample == 12 ? new ItemStack(Items.DIAMOND_LEGGINGS)
                        : sample == 13 ? new ItemStack(Items.LEATHER_LEGGINGS) : ItemStack.EMPTY);
                player.setItemSlot(EquipmentSlot.FEET, sample == 12 ? new ItemStack(Items.DIAMOND_BOOTS)
                        : sample == 13 ? new ItemStack(Items.LEATHER_BOOTS) : ItemStack.EMPTY);
                if (sample >= 14) {
                    level.setBlock(pos(), Blocks.AIR.defaultBlockState(), 3);
                    player.connection.teleport(pos().getX() + .5, pos().getY(), pos().getZ() + .5, yaw, 0);
                    if (sample == 15) {
                        Boat boat = new Boat(EntityType.BOAT, level);
                        boat.setPos(pos().getX() + .5, pos().getY(), pos().getZ() + .5);
                        level.addFreshEntity(boat);
                        player.startRiding(boat, true);
                    }
                }
                player.containerMenu.broadcastChanges();
                mc.execute(() -> {
                    phase = sample >= 14 ? 4 : 2;
                    ticks = 0;
                    waiting = false;
                });
            } catch (Exception error) {
                mc.execute(() -> finish(mc, "FAIL " + error));
            }
        });
    }

    private static BlockPos pos() {
        return sample >= 12 ? new BlockPos(2, -60, 14)
                : new BlockPos(2 + sample % 4 * 6, -60, 2 + sample / 4 * 6);
    }

    private static Direction facing() {
        return FACINGS[sample % 4];
    }

    private static void camera(Minecraft mc) {
        if (camera == null) {
            camera = EntityType.ARMOR_STAND.create(mc.level);
        }
        var forward = Vec3.atLowerCornerOf(facing().getNormal());
        var side = Vec3.atLowerCornerOf(facing().getClockWise().getNormal());
        Vec3 target = pos().getCenter().add(0, .25, 0);
        Vec3 eye = target.add(forward.scale(2.4)).add(side.scale(3.4)).add(0, .8, 0);
        Vec3 delta = target.subtract(eye);
        if (sample >= 14) {
            target = mc.player.position().add(0, .9, 0);
            eye = target.add(forward.scale(2.4)).add(side.scale(3.4)).add(0, .8, 0);
            delta = target.subtract(eye);
        }
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

    private static void record(Minecraft mc, String mode) {
        var player = mc.player;
        var seat = player.getVehicle();
        observations.add(String.format(Locale.ROOT,
                "%d %s %s seatYaw=%.1f body=%.1f oldBody=%.1f head=%.1f footY=%.4f anchorY=%.4f",
                sample, facing(), mode, seat.getYRot(), Mth.wrapDegrees(player.yBodyRot - seat.getYRot()),
                Mth.wrapDegrees(player.yBodyRotO - seat.getYRot()),
                Mth.wrapDegrees(player.yHeadRot - seat.getYRot()), player.getY(), seat.getY()));
    }

    private static void verifyPose(Minecraft mc, boolean seated) {
        PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
        var model = renderer.getModel();
        float expected = seated ? (float) (-Math.PI / 2) : sample == 15 ? -1.4137167f : 0;
        if (Math.abs(model.rightLeg.xRot - expected) > .02f
                || Math.abs(model.leftPants.xRot - model.leftLeg.xRot) > .001f) {
            throw new IllegalStateException("Wrong rendered leg/clothing pose at sample " + sample);
        }
    }

    private static void finish(Minecraft mc, String result) {
        phase = 99;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("../diagnostics/seating-client.txt").normalize(),
                    result + "\n" + String.join("\n", observations));
        } catch (Exception error) {
            CasinoMod.LOGGER.error("Cannot save seating observations", error);
        }
        mc.setCameraEntity(mc.player);
        mc.stop();
    }
}
