package com.mbolt.casino.datagen;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.registry.HospitalityTags;
import com.mbolt.casino.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

final class CasinoItemTags extends ItemTagsProvider {
    CasinoItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                   CompletableFuture<TagsProvider.TagLookup<Block>> blocks, ExistingFileHelper files) {
        super(output, lookup, blocks, CasinoMod.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        var menu = tag(HospitalityTags.BAR_MENU);
        ModItems.barMenu().forEach(menu::add);
        menu.add(Items.APPLE, Items.COOKIE, Items.BREAD, Items.PUMPKIN_PIE, Items.HONEY_BOTTLE);
    }
}
