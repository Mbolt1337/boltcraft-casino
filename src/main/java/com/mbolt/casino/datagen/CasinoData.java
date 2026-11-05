package com.mbolt.casino.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;

/** Registers every data generator; run with {@code gradlew runData}. */
public final class CasinoData {
    private CasinoData() {
    }

    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        var lookup = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new CasinoBlockStates(output, files));
        generator.addProvider(event.includeClient(), new CasinoItemModels(output, files));
        generator.addProvider(event.includeClient(), CasinoLanguage.english(output));
        generator.addProvider(event.includeClient(), CasinoLanguage.russian(output));

        generator.addProvider(event.includeServer(), new CasinoRecipes(output, lookup));
        var blocks = generator.addProvider(event.includeServer(), new CasinoBlockTags(output, lookup, files));
        generator.addProvider(event.includeServer(), new CasinoItemTags(output, lookup, blocks.contentsGetter(), files));
        generator.addProvider(event.includeServer(), new AdvancementProvider(output, lookup, files,
                List.of(new CasinoAdvancements())));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(CasinoBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
    }
}
