package com.mbolt.casino.datagen;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.registry.ModDecor;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;

final class CasinoItemModels extends ItemModelProvider {
    private static final List<String> FLAT_ITEMS = List.of("chip_1", "chip_10", "chip_100", "chip_1000",
            "manager_key", "guide_book");

    CasinoItemModels(PackOutput output, ExistingFileHelper files) {
        super(output, CasinoMod.MODID, files);
    }

    @Override
    protected void registerModels() {
        for (String item : FLAT_ITEMS) {
            basicItem(modLoc(item));
        }
        withExistingParent("cashier", modLoc("block/cashier"));
        // Animated blocks are shown in the inventory by baked copies of their GeckoLib geometry.
        withExistingParent("vault", modLoc("block/vault_item"));
        withExistingParent("slot_machine", modLoc("block/slot_machine_item"));
        withExistingParent("roulette_table", modLoc("block/roulette_table_item"));
        decor();
        withExistingParent("gilded_trim", modLoc("block/architecture/gilded_trim"));
        // Boards and the railing have item models of their own, written by art/boards.py and art/decor_models.py.
        withExistingParent("gilded_panel", modLoc("block/gilded_panel_000000"));
        withExistingParent("gilded_cornice", modLoc("block/gilded_cornice"));
        withExistingParent("coffered_ceiling", modLoc("block/coffered_ceiling"));
        withExistingParent("carpet_stairs", modLoc("block/carpet_stairs"));
        withExistingParent("gilded_lantern", modLoc("block/architecture/gilded_lantern"));
        withExistingParent("onyx_column", modLoc("block/decor/onyx_column_single"));
        for (String name : List.of("ceramic_cup", "canape", "club_sandwich", "berry_tart", "chocolate_truffle",
                "berry_fizz", "spiced_cocoa")) {
            singleTexture(name, mcLoc("item/generated"), "layer0", modLoc("item/" + name));
        }
        for (String name : List.of("dining_chair", "serving_tray", "table_lamp", "bottle_shelf")) {
            withExistingParent(name, modLoc("block/hospitality/" + name));
        }
    }

    /**
     * Decor items that show their block model. Neon, marquee, ropes, the sofa and the grand chandelier have item
     * models of their own, written by art/neon.py and art/decor_models.py.
     */
    private void decor() {
        for (String pattern : ModDecor.CARPETS) {
            withExistingParent("casino_carpet_" + pattern, modLoc("block/casino_carpet_" + pattern));
            withExistingParent("casino_carpet_" + pattern + "_block", modLoc("block/casino_carpet_" + pattern + "_block"));
        }
        for (String name : List.of("marble_tiles", "marble_tile_stairs", "marble_tile_slab", "checkered_tiles",
                "checkered_tile_stairs", "checkered_tile_slab", "velvet_wall_panel", "wood_wall_panel")) {
            withExistingParent(name, modLoc("block/" + name));
        }
        for (String name : List.of("bar_counter", "bar_stool", "gold_skirting", "crystal_chandelier", "wall_sconce",
                "coffee_table", "card_table", "gold_planter")) {
            withExistingParent(name, modLoc("block/decor/" + name));
        }
        withExistingParent("casino_column", modLoc("block/decor/column_single"));
        withExistingParent("velvet_curtain", modLoc("block/decor/velvet_curtain_single"));
    }
}
