package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Real normal-world locate and natural generation, using the same search API as /locate. Never shipped. */
@EventBusSubscriber(modid = CasinoMod.MODID, value = Dist.CLIENT)
public final class WorldgenClient {
    private static final List<String> BUILDINGS = List.of("neon_lounge", "grand_hall");
    private static final List<String> checks = new ArrayList<>();
    /** Measurements that inform, not checks that pass or fail. */
    private static final List<String> notes = new ArrayList<>();
    private static final int SURVEY_RADIUS = 6000;
    /** Before 27.10.2026 seed 731 had one lounge and no grand hall this close to spawn. */
    private static final List<Integer> SURVEY_MINIMUM = List.of(12, 4);
    private static int phase;
    private static int ticks;
    /** What to photograph: the nearest of each building, then the steepest sites the survey found. */
    private static final List<Target> targets = new ArrayList<>(List.of(
            new Target("neon_lounge", "natural_neon_lounge", null),
            new Target("grand_hall", "natural_grand_hall", null)));
    private static int building;
    private static boolean waiting;
    private static BlockPos expectedFloor;
    private static Vec3 cameraEye;
    private static float cameraYaw;
    private static float cameraPitch;
    private static ArmorStand camera;

    private WorldgenClient() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("boltcraft.worldgen_check") || phase == 99) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                DevClient.prepareOptions(mc);
                DevClient.createNormalWorld(mc, "BoltCraftTerrain");
                phase = 1;
            }
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null || waiting) {
                return;
            }
            mc.options.hideGui = true;
            if (phase == 2) {
                aim(mc);
            }
            if (++ticks < 200) {
                return;
            }
            if (phase == 2) {
                if (mc.level.getBlockState(expectedFloor).isAir()) {
                    if (ticks > 2400) {
                        throw new IllegalStateException("Client did not receive the located casino chunks; chunk "
                                + (mc.level.hasChunkAt(expectedFloor) ? "loaded" : "missing") + ", player at "
                                + mc.player.blockPosition() + ", floor " + expectedFloor);
                    }
                    return;
                }
                CasinoMod.LOGGER.info("Worldgen client has the casino after {} ticks", ticks);
                DevClient.screenshot(mc, targets.get(building).shot());
                if (++building == targets.size()) {
                    finish(mc, "PASS");
                    return;
                }
            }
            waiting = true;
            MinecraftServer server = mc.getSingleplayerServer();
            // The survey tries hundreds of sites; off the server thread, like worldgen, so the client stays connected.
            var survey = building == 0 && notes.isEmpty()
                    ? CompletableFuture.runAsync(() -> survey(server)) : CompletableFuture.<Void>completedFuture(null);
            survey.whenComplete((ignored, failure) -> server.execute(() -> {
                try {
                    if (failure != null) {
                        throw new IllegalStateException(failure.getCause() != null ? failure.getCause() : failure);
                    }
                    locate(server, targets.get(building));
                    mc.execute(() -> {
                        phase = 2;
                        ticks = 0;
                        waiting = false;
                    });
                } catch (Exception error) {
                    mc.execute(() -> finish(mc, "FAIL " + error));
                }
            }));
        } catch (Exception error) {
            finish(mc, "FAIL " + error);
        }
    }

    /** How many casinos the datapack places round spawn: at least the minimum for each building. */
    private static void survey(MinecraftServer server) {
        for (int i = 0; i < BUILDINGS.size(); i++) {
            String name = BUILDINGS.get(i);
            var result = CasinoSurvey.survey(server.overworld(), name, SURVEY_RADIUS);
            notes.add("SURVEY " + result.line());
            CasinoMod.LOGGER.info("Worldgen {}", notes.getLast());
            if (result.steepest() != null) {
                targets.add(new Target(name, "natural_" + name + "_slope", result.steepest()));
            }
            require(result.valid() >= SURVEY_MINIMUM.get(i),
                    name + " places at least " + SURVEY_MINIMUM.get(i) + " within " + SURVEY_RADIUS
                            + " blocks of spawn (" + result.valid() + ")");
        }
    }

    /** A building to find and photograph; with a chunk, the site the survey chose instead of the nearest. */
    private record Target(String name, String shot, @Nullable ChunkPos site) {
    }

    private static void locate(MinecraftServer server, Target goal) {
        String name = goal.name();
        var level = server.overworld();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var holder = registry.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, CasinoMod.id(name)));
        ChunkPos chunk = goal.site();
        if (chunk == null) {
            CasinoMod.LOGGER.info("Worldgen locating {} in normal seed 731", name);
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level,
                    HolderSet.direct(holder), BlockPos.ZERO, 16, false);
            require(found != null, name + " is found by the locate API");
            chunk = new ChunkPos(found.getFirst());
        } else {
            CasinoMod.LOGGER.info("Worldgen visiting the steepest {} site at {}", name, chunk);
        }
        var start = level.getChunk(chunk.x, chunk.z).getStartForStructure(holder.value());
        require(start != null && start.isValid(), name + " has a naturally generated start at " + chunk);
        var terrainBox = start.getBoundingBox();
        ChunkPos.rangeClosed(new ChunkPos(terrainBox.minX() >> 4, terrainBox.minZ() >> 4),
                new ChunkPos(terrainBox.maxX() >> 4, terrainBox.maxZ() >> 4))
                .forEach(pos -> level.getChunk(pos.x, pos.z));
        var piece = start.getPieces().getFirst();
        var tag = piece.createTag(StructurePieceSerializationContext.fromLevel(level));
        var origin = new BlockPos(tag.getInt("TPX"), tag.getInt("TPY"), tag.getInt("TPZ"));
        Rotation rotation = Rotation.valueOf(tag.getString("Rot"));
        var settings = new StructurePlaceSettings().setRotation(rotation);
        var template = server.getStructureManager().getOrCreate(CasinoMod.id(name));
        // StructureStart expands its box by 12 blocks for beard_box terrain adaptation.
        // Inspect the actual template floor, rather than that underground terrain-influence volume.
        var box = template.getBoundingBox(settings, origin);
        BlockPos entrance = StructureTemplate.calculateRelativePosition(settings,
                new BlockPos(template.getSize().getX() / 2, 0, template.getSize().getZ() - 1)).offset(origin);
        expectedFloor = entrance;
        require(level.getBlockState(entrance).isFaceSturdy(level, entrance, Direction.UP),
                name + " entrance has a solid floor " + entrance);
        require(level.getBlockState(entrance.above()).getCollisionShape(level, entrance.above()).isEmpty()
                        && level.getBlockState(entrance.above(2)).getCollisionShape(level, entrance.above(2)).isEmpty(),
                name + " entrance has clear headroom");
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                var floor = new BlockPos(x, box.minY(), z);
                requireDry(level.getBlockState(floor).getFluidState().isEmpty()
                                && level.getBlockState(floor.below()).isFaceSturdy(level, floor.below(), Direction.UP),
                        name + " has water or an unsupported foundation at " + floor);
            }
        }
        checks.add("PASS " + name + " entire foundation is solid and dry; " + box);
        BlockPos forward = StructureTemplate.calculateRelativePosition(settings, new BlockPos(0, 0, 1));
        double distance = Math.max(28, Math.max(box.getXSpan(), box.getZSpan()) * 1.05);
        double lookInside = template.getSize().getZ() * .35;
        Vec3 proposedEye = Vec3.atCenterOf(entrance).add(forward.getX() * distance, box.getYSpan() * .55,
                forward.getZ() * distance);
        BlockPos cameraColumn = BlockPos.containing(proposedEye);
        // Level.getHeight returns the world's minimum Y for an unloaded column.
        level.getChunkAt(cameraColumn);
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, cameraColumn.getX(), cameraColumn.getZ());
        Vec3 eye = new Vec3(proposedEye.x, Math.max(proposedEye.y, ground + 6), proposedEye.z);
        Vec3 target = Vec3.atCenterOf(entrance).add(-forward.getX() * lookInside, box.getYSpan() * .4,
                -forward.getZ() * lookInside);
        Vec3 delta = target.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
        CasinoMod.LOGGER.info("Worldgen camera {}: eye {}, terrain {}, target {}", name, eye, ground, target);
        var player = server.getPlayerList().getPlayers().getFirst();
        player.connection.teleport(eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
        Minecraft.getInstance().execute(() -> {
            cameraEye = eye;
            cameraYaw = yaw;
            cameraPitch = pitch;
            camera = null;
        });
        DevClient.command(server, "time set noon");
        CasinoMod.LOGGER.info("Worldgen verified {} {}", name, box);
    }

    /** Keep evidence independent of mouse movement while the player waits for remote chunks. */
    private static void aim(Minecraft mc) {
        if (cameraEye == null) {
            return;
        }
        if (camera == null) {
            camera = EntityType.ARMOR_STAND.create(mc.level);
        }
        camera.moveTo(cameraEye.x, cameraEye.y - camera.getEyeHeight(), cameraEye.z, cameraYaw, cameraPitch);
        camera.setOldPosAndRot();
        camera.yHeadRot = cameraYaw;
        camera.yHeadRotO = cameraYaw;
        mc.setCameraEntity(camera);
    }

    private static void require(boolean condition, String message) {
        requireDry(condition, message);
        checks.add("PASS " + message);
    }

    private static void requireDry(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void finish(Minecraft mc, String result) {
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("../diagnostics/worldgen-client.txt").normalize(),
                    result + " " + checks.size() + "\n" + String.join("\n", checks)
                            + (notes.isEmpty() ? "" : "\n" + String.join("\n", notes)));
        } catch (Exception error) {
            CasinoMod.LOGGER.error("Could not write worldgen result", error);
        }
        CasinoMod.LOGGER.info("WORLDGEN CLIENT {}", result);
        phase = 99;
        mc.setCameraEntity(mc.player);
        mc.stop();
    }
}
