package com.mbolt.casino.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.entity.OwnedBlockEntity;
import com.mbolt.casino.test.ArtPreview.Shot;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code gradlew runClient -PartPreview -PpreviewScene=structures}: builds both abandoned casinos from their plans
 * in art/structures, marks their casino blocks as fixtures, fills their chests' loot tables and saves the
 * templates worldgen uses to data/boltcraft_casino/structure. Then it places one more lounge the way worldgen
 * does, and photographs everything by day and by night.
 */
final class StructurePreview {
    private static final int FLOOR = -61;
    private static final BlockPos LOUNGE = new BlockPos(0, FLOOR, 0);
    private static final BlockPos HALL = new BlockPos(40, FLOOR, 0);
    /** Close to the others, so its chunks are loaded when the preview places it. */
    private static final BlockPos PLACED = new BlockPos(-48, FLOOR + 1, 0);

    private static final Vec3 LOUNGE_EYE = point(LOUNGE, 12.5, 8, 44);
    private static final Vec3 LOUNGE_TARGET = point(LOUNGE, 12.5, 9, 18);
    private static final Vec3 HALL_EYE = point(HALL, 20.5, 14, 68);
    private static final Vec3 HALL_TARGET = point(HALL, 20.5, 15, 30);

    static final List<Shot> SHOTS = List.of(
            new Shot("casino_lounge_facade", LOUNGE_EYE, LOUNGE_TARGET, 80),
            lounge("angle", -8, 10, 38, 12.5, 8, 14),
            lounge("side", 40, 10, 12, 20, 7, 12),
            lounge("rear", 12.5, 10, -20, 12.5, 6, 6),
            lounge("aerial", -2, 32, 40, 12.5, 1, 12),
            lounge("entrance", 12.5, 3.2, 31, 12.5, 6, 18),
            lounge("hall", 12.5, 4.6, 16.5, 12.5, 4.5, 6),
            lounge("bar", 13.5, 4.6, 10, 18, 4.5, 4),
            lounge("cashier", 8, 4.6, 9, 10, 4, 4),
            lounge("staff", 7.3, 4.6, 5.3, 3.5, 3.6, 3.5),
            new Shot("casino_hall_facade", HALL_EYE, HALL_TARGET, false),
            hall("angle", -14, 16, 62, 20.5, 14, 22),
            hall("side", 68, 15, 18, 36, 14, 18),
            hall("rear", 20.5, 18, -26, 20.5, 12, 8),
            hall("aerial", -12, 52, 66, 20.5, 4, 20),
            hall("portico", 20.5, 4.2, 49, 20.5, 8, 31),
            hall("atrium", 20.5, 5.6, 27.5, 20.5, 11, 14),
            hall("slots", 12.5, 5.6, 21, 7, 5, 12),
            hall("roulette", 29, 5.6, 21.5, 34.5, 4.5, 15),
            hall("bar", 20.5, 5.6, 11, 20.5, 5.5, 4),
            hall("gallery", 35, 14.6, 25, 20, 12, 15),
            hall("stairs", 15, 9, 23, 20, 9, 15),
            hall("landing", 24, 14.6, 4, 20, 13, 16),
            hall("front_gallery", 20.5, 14.6, 27.5, 20.5, 12, 14),
            hall("bartender", 25, 5.6, 4, 17, 5, 6),
            hall("vault", 7.5, 14.6, 7.5, 4, 13.5, 3),
            hall("balcony", 12, 14.6, 38, 20, 18, 32),
            new Shot("casino_night_lounge", LOUNGE_EYE, LOUNGE_TARGET, 40,
                    ArtPreview.onServer(server -> DevClient.command(server, "time set midnight"))),
            lounge("night_hall", 12.5, 4.6, 16.5, 12.5, 4.5, 6),
            new Shot("casino_night_hall", HALL_EYE, HALL_TARGET, false),
            hall("night_atrium", 20.5, 5.6, 27.5, 20.5, 11, 14));

    private static Vec3 point(BlockPos origin, double x, double y, double z) {
        return new Vec3(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
    }

    private static Shot lounge(String name, double x, double y, double z, double tx, double ty, double tz) {
        return new Shot("casino_lounge_" + name, point(LOUNGE, x, y, z), point(LOUNGE, tx, ty, tz), false);
    }

    private static Shot hall(String name, double x, double y, double z, double tx, double ty, double tz) {
        return new Shot("casino_hall_" + name, point(HALL, x, y, z), point(HALL, tx, ty, tz), false);
    }

    private StructurePreview() {
    }

    static void build(MinecraftServer server) {
        DevClient.command(server, "gamerule doDaylightCycle false");
        DevClient.command(server, "gamerule doWeatherCycle false");
        DevClient.command(server, "gamerule doMobSpawning false");
        DevClient.command(server, "time set noon");
        DevClient.command(server, "weather clear");
        ServerLevel level = server.overworld();
        Path project = Path.of(System.getProperty("boltcraft.project_dir", ".."));
        for (var building : List.of(new Building("neon_lounge", LOUNGE), new Building("grand_hall", HALL))) {
            JsonObject plan = read(project.resolve("art/structures/" + building.name() + ".json"));
            Vec3i size = size(plan);
            place(level, plan, building.origin(), size);
            save(server, level, building, size, project);
        }
        placeLikeWorldgen(level, CasinoMod.id("neon_lounge"), PLACED);
    }

    /** What /place structure does, logging why it failed instead of keeping quiet. */
    private static void placeLikeWorldgen(ServerLevel level, ResourceLocation id, BlockPos pos) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        if (structure == null) {
            CasinoMod.LOGGER.warn("No structure {}", id);
            return;
        }
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        StructureStart start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
                new ChunkPos(pos), 0, level, biome -> true);
        if (!start.isValid()) {
            CasinoMod.LOGGER.warn("Structure {} found no place at {}", id, pos);
            return;
        }
        BoundingBox box = start.getBoundingBox();
        ChunkPos from = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()),
                SectionPos.blockToSectionCoord(box.minZ()));
        ChunkPos to = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()),
                SectionPos.blockToSectionCoord(box.maxZ()));
        ChunkPos.rangeClosed(from, to).forEach(chunk -> start.placeInChunk(level, level.structureManager(), generator,
                level.getRandom(), new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(),
                        chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxBuildHeight(), chunk.getMaxBlockZ()),
                chunk));
        CasinoMod.LOGGER.info("Placed {} like worldgen in {}", id, box);
    }

    private record Building(String name, BlockPos origin) {
    }

    private static JsonObject read(Path plan) {
        try {
            return JsonParser.parseString(Files.readString(plan)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + plan, e);
        }
    }

    private static Vec3i size(JsonObject plan) {
        JsonArray size = plan.getAsJsonArray("size");
        return new Vec3i(size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt());
    }

    /**
     * Sets every block of the plan without waking its neighbours, then lets each block settle against the
     * finished building, as if it had been placed by hand once everything else stood.
     */
    private static void place(ServerLevel level, JsonObject plan, BlockPos origin, Vec3i size) {
        var blocks = level.holderLookup(Registries.BLOCK);
        List<BlockPos> placed = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1, size.getY() - 1,
                size.getZ() - 1))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        for (JsonElement element : plan.getAsJsonArray("blocks")) {
            JsonArray block = element.getAsJsonArray();
            BlockPos pos = origin.offset(block.get(0).getAsInt(), block.get(1).getAsInt(), block.get(2).getAsInt());
            try {
                BlockState state = BlockStateParser.parseForBlock(blocks, block.get(3).getAsString(), false)
                        .blockState();
                level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                placed.add(pos.immutable());
            } catch (CommandSyntaxException e) {
                throw new IllegalArgumentException("Bad block in plan: " + block, e);
            }
        }
        for (BlockPos pos : placed) {
            BlockState state = level.getBlockState(pos);
            BlockState settled = Block.updateFromNeighbourShapes(state, level, pos);
            if (settled != state) {
                level.setBlock(pos, settled, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            if (level.getBlockEntity(pos) instanceof OwnedBlockEntity owned) {
                owned.markFixture();
            }
        }
        for (JsonElement element : plan.getAsJsonArray("chests")) {
            JsonArray chest = element.getAsJsonArray();
            BlockPos pos = origin.offset(chest.get(0).getAsInt(), chest.get(1).getAsInt(), chest.get(2).getAsInt());
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity box) {
                box.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.parse(chest.get(3).getAsString())));
            }
        }
    }

    /** Writes the template into the mod's resources, and into this world so it can be placed right away. */
    private static void save(MinecraftServer server, ServerLevel level, Building building, Vec3i size,
                             Path project) {
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, building.origin(), size, false, Blocks.STRUCTURE_VOID);
        template.setAuthor("BoltCraft");
        var tag = template.save(new net.minecraft.nbt.CompoundTag());
        List<Path> targets = List.of(
                project.resolve("src/main/resources/data/" + CasinoMod.MODID + "/structure/" + building.name()
                        + ".nbt"),
                server.getWorldPath(LevelResource.GENERATED_DIR).resolve(CasinoMod.MODID + "/structure/"
                        + building.name() + ".nbt"));
        for (Path target : targets) {
            try {
                Files.createDirectories(target.getParent());
                NbtIo.writeCompressed(tag, target);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot save " + target, e);
            }
        }
        CasinoMod.LOGGER.info("Saved casino template {} ({}x{}x{})", building.name(), size.getX(), size.getY(),
                size.getZ());
    }
}
