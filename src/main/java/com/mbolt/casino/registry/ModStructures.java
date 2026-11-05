package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.world.CasinoPiece;
import com.mbolt.casino.world.CasinoStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The abandoned casinos' structure type and piece; the buildings themselves are data (see data/.../worldgen). */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, CasinoMod.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, CasinoMod.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<CasinoStructure>> CASINO =
            TYPES.register("casino", () -> () -> CasinoStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CASINO_PIECE =
            PIECES.register("casino", () -> (StructurePieceType.StructureTemplateType) CasinoPiece::new);

    private ModStructures() {
    }
}
