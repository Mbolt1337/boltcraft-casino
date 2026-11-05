package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.CashierBlock;
import com.mbolt.casino.block.SlotMachineBlock;
import com.mbolt.casino.block.RouletteTableBlock;
import com.mbolt.casino.block.VaultBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(CasinoMod.MODID);

    /** Casino blocks resist explosions like obsidian; protection itself is handled by ProtectionEvents. */
    private static final float BLAST_RESISTANCE = 1200.0f;

    public static final DeferredBlock<VaultBlock> VAULT = REGISTER.register("vault",
            () -> new VaultBlock(gameBlock(MapColor.COLOR_BLACK, SoundType.NETHERITE_BLOCK, 8.0f)));
    public static final DeferredBlock<CashierBlock> CASHIER = REGISTER.register("cashier",
            () -> new CashierBlock(gameBlock(MapColor.COLOR_BROWN, SoundType.WOOD, 4.0f)));
    public static final DeferredBlock<SlotMachineBlock> SLOT_MACHINE = REGISTER.register("slot_machine",
            () -> new SlotMachineBlock(gameBlock(MapColor.COLOR_BLACK, SoundType.METAL, 4.0f)));
    public static final DeferredBlock<RouletteTableBlock> ROULETTE_TABLE = REGISTER.register("roulette_table",
            () -> new RouletteTableBlock(gameBlock(MapColor.COLOR_BROWN, SoundType.WOOD, 4.0f)));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties gameBlock(MapColor color, SoundType sound, float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .sound(sound)
                .strength(hardness, BLAST_RESISTANCE)
                .requiresCorrectToolForDrops()
                .pushReaction(PushReaction.BLOCK)
                .noOcclusion();
    }
}
