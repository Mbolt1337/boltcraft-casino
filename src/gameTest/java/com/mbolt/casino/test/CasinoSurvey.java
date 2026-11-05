package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.world.CasinoStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * How often a casino can generate: every placement candidate within a radius of spawn, tried the way worldgen
 * tries it, with the datapack's slope limit and with any other limits asked about. Safe off the server thread,
 * like worldgen itself. Never shipped.
 */
final class CasinoSurvey {
    private CasinoSurvey() {
    }

    /**
     * The line to report, how many casinos the datapack as shipped would place, and of those the site on the
     * steepest ground.
     */
    record Result(String line, int valid, @Nullable ChunkPos steepest) {
    }

    static Result survey(ServerLevel level, String name, int radius, int... slopes) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(CasinoMod.id(name));
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(CasinoMod.id(name));
        if (structure == null || set == null
                || !(set.placement() instanceof RandomSpreadStructurePlacement placement)) {
            throw new IllegalStateException("No random spread structure set " + name);
        }
        var settings = new Structure.StructureSettings(structure.biomes(), structure.spawnOverrides(),
                structure.step(), structure.terrainAdaptation());
        List<Structure> variants = new ArrayList<>(List.of(structure));
        for (int slope : slopes) {
            variants.add(new CasinoStructure(settings, CasinoMod.id(name), slope));
        }
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        int spacing = placement.spacing();
        int regions = (radius >> 4) / spacing + 1;
        var state = level.getChunkSource().getGeneratorState();
        int candidates = 0;
        int excluded = 0;
        int inBiome = 0;
        int[] valid = new int[variants.size()];
        double[] nearest = new double[variants.size()];
        Arrays.fill(nearest, Double.MAX_VALUE);
        Map<String, Integer> biomes = new TreeMap<>();
        ChunkPos steepest = null;
        int steepestRange = -1;
        long started = System.nanoTime();
        for (int rx = -regions; rx <= regions; rx++) {
            for (int rz = -regions; rz <= regions; rz++) {
                ChunkPos chunk = placement.getPotentialStructureChunk(level.getSeed(), rx * spacing, rz * spacing);
                BlockPos corner = chunk.getWorldPosition();
                double distance = Math.hypot(corner.getX(), corner.getZ());
                if (distance > radius) {
                    continue;
                }
                candidates++;
                // The exclusion zone round villages, as worldgen applies it.
                if (!placement.isStructureChunk(state, chunk.x, chunk.z)) {
                    excluded++;
                    continue;
                }
                int y = generator.getBaseHeight(corner.getX(), corner.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level,
                        random);
                var biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(corner.getX()),
                        QuartPos.fromBlock(y), QuartPos.fromBlock(corner.getZ()), random.sampler());
                biomes.merge(biome.unwrapKey().map(key -> key.location().getPath()).orElse("?"), 1, Integer::sum);
                if (!structure.biomes().contains(biome)) {
                    continue;
                }
                inBiome++;
                for (int i = 0; i < variants.size(); i++) {
                    var start = variants.get(i).generate(level.registryAccess(), generator, generator.getBiomeSource(),
                            random, level.getStructureManager(), level.getSeed(), chunk, 0, level,
                            structure.biomes()::contains);
                    if (start.isValid()) {
                        valid[i]++;
                        nearest[i] = Math.min(nearest[i], distance);
                        if (i == 0) {
                            int range = range(level, start.getPieces().getFirst().getBoundingBox());
                            if (range > steepestRange) {
                                steepestRange = range;
                                steepest = chunk;
                            }
                        }
                    }
                }
            }
        }
        var line = new StringBuilder(String.format(Locale.ROOT,
                "%s, spacing %d chunks: %d candidates within %d blocks of spawn, %d next to villages, %d in its"
                        + " biomes", name, spacing, candidates, radius, excluded, inBiome));
        for (int i = 0; i < variants.size(); i++) {
            line.append(String.format(Locale.ROOT, "; %s %d valid (nearest %s)",
                    i == 0 ? "as shipped" : "max_slope " + slopes[i - 1], valid[i],
                    valid[i] == 0 ? "none" : Math.round(nearest[i]) + ""));
        }
        line.append(String.format(Locale.ROOT, "; steepest ground %d blocks at %s; %d s; biomes of all"
                        + " candidates %s", steepestRange, steepest, (System.nanoTime() - started) / 1_000_000_000,
                biomes));
        return new Result(line.toString(), valid[0], steepest);
    }

    /** How far the natural ground under a building rises and falls, every fourth column. */
    private static int range(ServerLevel level, BoundingBox box) {
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int x = box.minX(); x <= box.maxX(); x += 4) {
            for (int z = box.minZ(); z <= box.maxZ(); z += 4) {
                int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                lowest = Math.min(lowest, y);
                highest = Math.max(highest, y);
            }
        }
        return highest - lowest;
    }
}
