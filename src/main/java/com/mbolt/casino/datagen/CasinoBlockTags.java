package com.mbolt.casino.datagen;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.registry.DecorTags;
import com.mbolt.casino.registry.ModBlocks;
import com.mbolt.casino.registry.ModDecor;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;
import java.util.concurrent.CompletableFuture;

final class CasinoBlockTags extends BlockTagsProvider {
    CasinoBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, CasinoMod.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.VAULT.get(), ModBlocks.SLOT_MACHINE.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.CASHIER.get(), ModBlocks.ROULETTE_TABLE.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.VAULT.get(), ModBlocks.SLOT_MACHINE.get());
        decor();
    }

    /** Every decor block sits in one boltcraft_casino:decor/* group, and all of them in boltcraft_casino:decor. */
    private void decor() {
        group("neon", ModDecor.NEON_LETTER, ModDecor.NEON_TUBE, ModDecor.NEON_PANEL_BOLTCRAFT,
                ModDecor.NEON_PANEL_JACKPOT, ModDecor.NEON_PANEL_BAR, ModDecor.NEON_PANEL_VIP);
        ModDecor.NEON_SIGN_BLOCKS.forEach(sign -> tag(DecorTags.group("neon")).add(sign.get()));
        group("lights", ModDecor.MARQUEE_LIGHTS, ModDecor.CRYSTAL_CHANDELIER, ModDecor.GRAND_CHANDELIER,
                ModDecor.WALL_SCONCE);
        ModDecor.CARPET_BLOCKS.forEach(carpet -> tag(DecorTags.group("carpets")).add(carpet.get()));
        ModDecor.CARPET_FULL_BLOCKS.forEach(carpet -> tag(DecorTags.group("carpets")).add(carpet.get()));
        group("floors", ModDecor.MARBLE_TILES, ModDecor.MARBLE_TILE_STAIRS, ModDecor.MARBLE_TILE_SLAB,
                ModDecor.CHECKERED_TILES, ModDecor.CHECKERED_TILE_STAIRS, ModDecor.CHECKERED_TILE_SLAB);
        group("walls", ModDecor.VELVET_WALL_PANEL, ModDecor.WOOD_WALL_PANEL, ModDecor.GOLD_SKIRTING,
                ModDecor.CASINO_COLUMN, ModDecor.VELVET_CURTAIN, ModDecor.GILDED_TRIM);
        group("furniture", ModDecor.BAR_COUNTER, ModDecor.BAR_STOOL, ModDecor.VELVET_SOFA, ModDecor.COFFEE_TABLE,
                ModDecor.CARD_TABLE, ModDecor.GOLD_PLANTER);
        ModDecor.VELVET_ROPES.forEach(rope -> tag(DecorTags.group("furniture")).add(rope.get()));
        group("furniture", ModDecor.DINING_CHAIR, ModDecor.SERVING_TRAY, ModDecor.BOTTLE_SHELF);
        group("lights", ModDecor.TABLE_LAMP);
        group("architecture", ModDecor.CASINO_MARQUEE, ModDecor.CASINO_MARQUEE_SMALL, ModDecor.BOLTCRAFT_BANNER,
                ModDecor.GILDED_PANEL, ModDecor.GILDED_CORNICE, ModDecor.COFFERED_CEILING, ModDecor.CARPET_STAIRS,
                ModDecor.GILDED_RAILING, ModDecor.GILDED_LANTERN, ModDecor.ONYX_COLUMN);
        ModDecor.GIANT_CHIPS.forEach(chip -> tag(DecorTags.group("architecture")).add(chip.get()));
        var all = tag(DecorTags.ALL);
        for (String group : DecorTags.GROUPS) {
            all.addTag(DecorTags.group(group));
        }

        tag(BlockTags.STAIRS).add(ModDecor.MARBLE_TILE_STAIRS.get(), ModDecor.CHECKERED_TILE_STAIRS.get(),
                ModDecor.CARPET_STAIRS.get());
        tag(BlockTags.SLABS).add(ModDecor.MARBLE_TILE_SLAB.get(), ModDecor.CHECKERED_TILE_SLAB.get());
        var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        for (var block : List.of(ModDecor.MARBLE_TILES, ModDecor.MARBLE_TILE_STAIRS, ModDecor.MARBLE_TILE_SLAB,
                ModDecor.CHECKERED_TILES, ModDecor.CHECKERED_TILE_STAIRS, ModDecor.CHECKERED_TILE_SLAB,
                ModDecor.CASINO_COLUMN, ModDecor.MARQUEE_LIGHTS, ModDecor.CRYSTAL_CHANDELIER,
                ModDecor.GRAND_CHANDELIER, ModDecor.WALL_SCONCE, ModDecor.GOLD_PLANTER)) {
            pickaxe.add(block.get());
        }
        ModDecor.VELVET_ROPES.forEach(rope -> pickaxe.add(rope.get()));
        pickaxe.add(ModDecor.TABLE_LAMP.get());
        pickaxe.add(ModDecor.GILDED_TRIM.get());
        for (var block : List.of(ModDecor.CASINO_MARQUEE, ModDecor.CASINO_MARQUEE_SMALL, ModDecor.GILDED_PANEL,
                ModDecor.GILDED_CORNICE, ModDecor.CARPET_STAIRS, ModDecor.GILDED_RAILING, ModDecor.GILDED_LANTERN,
                ModDecor.ONYX_COLUMN)) {
            pickaxe.add(block.get());
        }
        ModDecor.GIANT_CHIPS.forEach(chip -> pickaxe.add(chip.get()));
        var axe = tag(BlockTags.MINEABLE_WITH_AXE);
        axe.add(ModDecor.DINING_CHAIR.get(), ModDecor.SERVING_TRAY.get(), ModDecor.BOTTLE_SHELF.get(),
                ModDecor.COFFERED_CEILING.get());
        for (var block : List.of(ModDecor.BAR_COUNTER, ModDecor.BAR_STOOL, ModDecor.WOOD_WALL_PANEL,
                ModDecor.GOLD_SKIRTING, ModDecor.VELVET_SOFA, ModDecor.COFFEE_TABLE, ModDecor.CARD_TABLE)) {
            axe.add(block.get());
        }
    }

    @SafeVarargs
    private void group(String name, DeferredBlock<? extends Block>... blocks) {
        var tag = tag(DecorTags.group(name));
        for (DeferredBlock<? extends Block> block : blocks) {
            tag.add(block.get());
        }
    }
}
