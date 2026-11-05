package com.mbolt.casino.world;

import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * An abandoned casino: one building from a template, set on fairly level dry ground. The datapack decides the
 * template, the biomes ({@code has_structure/*} tags) and how often (structure sets); the server config can switch
 * new casinos off altogether.
 */
public final class CasinoStructure extends Structure {
    public static final MapCodec<CasinoStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(structure -> structure.template),
            Codec.intRange(0, 16).optionalFieldOf("max_slope", 3).forGetter(structure -> structure.maxSlope)
    ).apply(instance, CasinoStructure::new));

    private final ResourceLocation template;
    /** The most the ground anywhere under the building may differ in height. */
    private final int maxSlope;

    public CasinoStructure(StructureSettings settings, ResourceLocation template, int maxSlope) {
        super(settings);
        this.template = template;
        this.maxSlope = maxSlope;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!CasinoServerConfig.GENERATE_STRUCTURES.get()) {
            return Optional.empty();
        }
        Rotation rotation = Rotation.getRandom(context.random());
        StructureTemplate building = context.structureTemplateManager().getOrCreate(template);
        BlockPos corner = context.chunkPos().getWorldPosition();
        BoundingBox box = building.getBoundingBox(new StructurePlaceSettings().setRotation(rotation), corner);
        BlockPos entrance = StructureTemplate.calculateRelativePosition(
                new StructurePlaceSettings().setRotation(rotation),
                new BlockPos(building.getSize().getX() / 2, 0, building.getSize().getZ() - 1)).offset(corner);
        BiFunction<Integer, Integer, CasinoTerrain.Surface> read = (x, z) -> {
            NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(),
                    context.randomState());
            // WORLD_SURFACE_WG is the highest non-air block. Reuse this column for both height and water
            // detection instead of evaluating the terrain noise twice at the same position.
            int y = context.heightAccessor().getMaxBuildHeight() - 1;
            while (y > context.heightAccessor().getMinBuildHeight() && column.getBlock(y).isAir()) {
                y--;
            }
            var surface = column.getBlock(y);
            return new CasinoTerrain.Surface(y, !surface.isAir() && surface.getFluidState().isEmpty());
        };
        // Vanilla checks the biome only once this method has returned, at the stub, which stands at the
        // entrance's ground height. Checking it here first gives the same answer without scanning every column
        // under a site in the wrong biome.
        int ground = read.apply(entrance.getX(), entrance.getZ()).y();
        if (!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(corner.getX()),
                QuartPos.fromBlock(ground), QuartPos.fromBlock(corner.getZ()), context.randomState().sampler()))) {
            return Optional.empty();
        }
        var floor = CasinoTerrain.floor(box.minX(), box.minZ(), box.getXSpan(), box.getZSpan(),
                entrance.getX(), entrance.getZ(), maxSlope, read);
        if (floor.isEmpty()) {
            return Optional.empty();
        }
        BlockPos position = corner.atY(floor.getAsInt());
        return Optional.of(new GenerationStub(position, pieces -> pieces.addPiece(
                new CasinoPiece(context.structureTemplateManager(), template, position, rotation))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CASINO.get();
    }
}
