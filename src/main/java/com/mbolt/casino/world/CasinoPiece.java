package com.mbolt.casino.world;

import com.mbolt.casino.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * The building itself. Before the template goes down, every column under its floor is filled with stone down to
 * solid ground, so a casino on a slope stands on a plinth instead of hanging in the air.
 */
public final class CasinoPiece extends TemplateStructurePiece {
    private static final String ROTATION = "Rot";
    /** Deeper holes than this are left alone; the terrain adaptation smooths what is left. */
    private static final int MAX_FOUNDATION = 16;
    private static final BlockState FOUNDATION = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();

    public CasinoPiece(StructureTemplateManager templates, ResourceLocation template, BlockPos position,
                       Rotation rotation) {
        super(ModStructures.CASINO_PIECE.get(), 0, templates, template, template.toString(), settings(rotation),
                position);
    }

    public CasinoPiece(StructureTemplateManager templates, CompoundTag tag) {
        super(ModStructures.CASINO_PIECE.get(), tag, templates,
                location -> settings(Rotation.valueOf(tag.getString(ROTATION))));
    }

    /**
     * The template already holds every block in its final shape. Worldgen places it chunk by chunk, and letting
     * blocks reshape there would break the boards and panels that span a chunk border: their other parts are not
     * placed yet, so each half would see itself cut off.
     */
    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation).setKnownShape(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString(ROTATION, placeSettings.getRotation().name());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunkBox, ChunkPos chunk, BlockPos pivot) {
        BoundingBox building = template.getBoundingBox(placeSettings, templatePosition);
        int floor = building.minY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(building.minX(), chunkBox.minX()); x <= Math.min(building.maxX(), chunkBox.maxX()); x++) {
            for (int z = Math.max(building.minZ(), chunkBox.minZ()); z <= Math.min(building.maxZ(), chunkBox.maxZ());
                 z++) {
                for (int y = floor - 1; y >= floor - MAX_FOUNDATION && y > level.getMinBuildHeight(); y--) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isSolidRender(level, pos)) {
                        break;
                    }
                    level.setBlock(pos, FOUNDATION, 2);
                }
            }
        }
        super.postProcess(level, structures, generator, random, chunkBox, chunk, pivot);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random,
                                    BoundingBox box) {
    }
}
