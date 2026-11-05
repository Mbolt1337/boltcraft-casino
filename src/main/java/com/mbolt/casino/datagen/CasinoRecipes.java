package com.mbolt.casino.datagen;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.registry.ModBlocks;
import com.mbolt.casino.registry.ModDecor;
import com.mbolt.casino.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.List;

import java.util.concurrent.CompletableFuture;

/**
 * Game blocks are a mid-game goal: the vault needs a netherite ingot. Chips are never crafted. Decor is cheap, so
 * a casino can be furnished from the first gold found.
 */
final class CasinoRecipes extends RecipeProvider {
    CasinoRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        hospitality(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.VAULT.get())
                .pattern("IOI")
                .pattern("ONO")
                .pattern("IOI")
                .define('I', Items.IRON_BLOCK)
                .define('O', Items.OBSIDIAN)
                .define('N', Items.NETHERITE_INGOT)
                .unlockedBy("has_netherite", has(Items.NETHERITE_INGOT))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.CASHIER.get())
                .pattern("GIG")
                .pattern("WBW")
                .pattern("WWW")
                .define('G', Items.GOLD_INGOT)
                .define('I', Items.IRON_BARS)
                .define('B', Items.BOOK)
                .define('W', Items.DARK_OAK_PLANKS)
                .unlockedBy("has_gold", has(Items.GOLD_INGOT))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.SLOT_MACHINE.get())
                .pattern("DGD")
                .pattern("GRG")
                .pattern("GIG")
                .define('D', Items.DIAMOND)
                .define('G', Items.GOLD_BLOCK)
                .define('R', Items.REDSTONE)
                .define('I', Items.GLASS)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MANAGER_KEY.get())
                .pattern(" E")
                .pattern("GG")
                .pattern("G ")
                .define('E', Items.EMERALD)
                .define('G', Items.GOLD_INGOT)
                .unlockedBy("has_emerald", has(Items.EMERALD))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.ROULETTE_TABLE.get())
                .pattern("DED").pattern("WWW").pattern("W W")
                .define('D', Items.DIAMOND).define('E', Items.EMERALD_BLOCK).define('W', Items.DARK_OAK_PLANKS)
                .unlockedBy("has_emerald_block", has(Items.EMERALD_BLOCK)).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.GUIDE_BOOK.get())
                .requires(Items.BOOK).requires(Items.GOLD_NUGGET)
                .unlockedBy("has_book", has(Items.BOOK)).save(output);
        neon(output);
        interior(output);
    }

    private void hospitality(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.DINING_CHAIR.get(), 2)
                .pattern("W W").pattern("WRW").pattern("N N")
                .define('W', Items.DARK_OAK_PLANKS).define('R', Items.RED_WOOL).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_wool", has(Items.RED_WOOL)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.SERVING_TRAY.get())
                .pattern("NQN").pattern("WWW")
                .define('W', Items.DARK_OAK_SLAB).define('Q', Items.QUARTZ).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_quartz", has(Items.QUARTZ)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.TABLE_LAMP.get())
                .pattern("RRR").pattern(" L ").pattern(" N ")
                .define('R', Items.RED_WOOL).define('L', Items.LANTERN).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_lantern", has(Items.LANTERN)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.BOTTLE_SHELF.get())
                .pattern("WNW").pattern("BBB").pattern("WWW")
                .define('W', Items.DARK_OAK_PLANKS).define('B', Items.GLASS_BOTTLE).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_bottle", has(Items.GLASS_BOTTLE)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CERAMIC_CUP.get(), 4)
                .pattern("B B").pattern(" B ").define('B', Items.BRICK)
                .unlockedBy("has_brick", has(Items.BRICK)).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.CANAPE.get(), 4)
                .requires(Items.BREAD).requires(Items.COOKED_BEEF).requires(Items.BEETROOT)
                .unlockedBy("has_bread", has(Items.BREAD)).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.CLUB_SANDWICH.get(), 2)
                .requires(Items.BREAD).requires(Items.COOKED_CHICKEN).requires(Items.CARROT)
                .unlockedBy("has_bread", has(Items.BREAD)).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.BERRY_TART.get(), 2)
                .requires(Items.WHEAT, 3).requires(Items.SUGAR).requires(Items.SWEET_BERRIES).requires(Items.EGG)
                .unlockedBy("has_berries", has(Items.SWEET_BERRIES)).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.CHOCOLATE_TRUFFLE.get(), 4)
                .requires(Items.COCOA_BEANS, 2).requires(Items.SUGAR).requires(Items.MILK_BUCKET)
                .unlockedBy("has_cocoa", has(Items.COCOA_BEANS)).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.BERRY_FIZZ.get())
                .requires(Items.GLASS_BOTTLE).requires(Items.SWEET_BERRIES).requires(Items.SUGAR)
                .requires(Items.WATER_BUCKET).unlockedBy("has_bottle", has(Items.GLASS_BOTTLE)).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.SPICED_COCOA.get())
                .requires(ModItems.CERAMIC_CUP.get()).requires(Items.COCOA_BEANS).requires(Items.SUGAR)
                .requires(Items.MILK_BUCKET).unlockedBy("has_cocoa", has(Items.COCOA_BEANS)).save(output);
    }

    /** Neon is glass and glowstone; signs and boards are neon plus something that says what they show. */
    private void neon(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.NEON_LETTER.get(), 8)
                .pattern("PPP").pattern("PDP").pattern("PPP")
                .define('P', Items.GLASS_PANE).define('D', Items.GLOWSTONE_DUST)
                .unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.NEON_TUBE.get(), 4)
                .pattern("PDP")
                .define('P', Items.GLASS_PANE).define('D', Items.GLOWSTONE_DUST)
                .unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST)).save(output);
        List<ItemLike> symbols = List.of(Items.GOLD_NUGGET, Items.RED_DYE, Items.YELLOW_DYE, Items.PAPER, Items.BONE,
                Items.GOLD_INGOT, Items.GLASS_BOTTLE);
        for (int i = 0; i < ModDecor.NEON_SIGNS.size(); i++) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ModDecor.NEON_SIGN_BLOCKS.get(i).get())
                    .requires(ModDecor.NEON_TUBE.get(), 2).requires(symbols.get(i))
                    .unlockedBy("has_neon_tube", has(ModDecor.NEON_TUBE.get())).save(output);
        }
        panel(output, ModDecor.NEON_PANEL_BOLTCRAFT.get(), 4, Items.COPPER_INGOT);
        panel(output, ModDecor.NEON_PANEL_JACKPOT.get(), 4, Items.GOLD_NUGGET);
        panel(output, ModDecor.NEON_PANEL_BAR.get(), 2, Items.GLASS_BOTTLE);
        panel(output, ModDecor.NEON_PANEL_VIP.get(), 2, Items.RED_WOOL);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.MARQUEE_LIGHTS.get(), 3)
                .pattern("NDN")
                .define('N', Items.GOLD_NUGGET).define('D', Items.GLOWSTONE_DUST)
                .unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST)).save(output);
    }

    private void panel(RecipeOutput output, ItemLike panel, int letters, ItemLike symbol) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, panel)
                .requires(ModDecor.NEON_LETTER.get(), letters).requires(Items.GOLD_INGOT)
                .requires(Items.DARK_OAK_PLANKS).requires(symbol)
                .unlockedBy("has_neon_letter", has(ModDecor.NEON_LETTER.get())).save(output);
    }

    /** Carpets, floors, walls, light and furniture: wool, wood, calcite and a touch of gold. */
    private void interior(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModDecor.GILDED_TRIM.get(), 8)
                .pattern("BBB").pattern("BNB").pattern("BBB")
                .define('B', Items.POLISHED_BLACKSTONE).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET)).save(output);
        architecture(output);
        List<List<ItemLike>> carpetWool = List.of(List.of(Items.RED_WOOL), List.of(Items.BLUE_WOOL),
                List.of(Items.GREEN_WOOL), List.of(Items.RED_WOOL, Items.BLACK_WOOL));
        for (int i = 0; i < ModDecor.CARPETS.size(); i++) {
            ItemLike block = ModDecor.CARPET_FULL_BLOCKS.get(i).get();
            List<ItemLike> wool = carpetWool.get(i);
            var recipe = ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, block, 4)
                    .requires(Items.GOLD_NUGGET);
            for (int j = 0; j < 4; j++) {
                recipe.requires(wool.get(j % wool.size()));
            }
            recipe.unlockedBy("has_wool", has(wool.get(0))).save(output);
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.CARPET_BLOCKS.get(i).get(), 3)
                    .pattern("CC").define('C', block)
                    .unlockedBy("has_carpet_block", has(block)).save(output);
        }
        List<ItemLike> ropeWool = List.of(Items.RED_WOOL, Items.BLUE_WOOL, Items.GREEN_WOOL, Items.BLACK_WOOL);
        for (int i = 0; i < ModDecor.ROPE_COLORS.size(); i++) {
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.VELVET_ROPES.get(i).get(), 2)
                    .pattern("W").pattern("S").pattern("N")
                    .define('W', ropeWool.get(i)).define('S', Items.STICK).define('N', Items.GOLD_NUGGET)
                    .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET)).save(output);
        }
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.BAR_COUNTER.get(), 3)
                .pattern("SSS").pattern("WRW").pattern("WWW")
                .define('S', Items.DARK_OAK_SLAB).define('W', Items.DARK_OAK_PLANKS).define('R', Items.RED_WOOL)
                .unlockedBy("has_dark_oak", has(Items.DARK_OAK_PLANKS)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.BAR_STOOL.get(), 2)
                .pattern("R").pattern("S").pattern("N")
                .define('R', Items.RED_CARPET).define('S', Items.STICK).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET)).save(output);

        tiles(output, ModDecor.MARBLE_TILES.get(), ModDecor.MARBLE_TILE_STAIRS.get(), ModDecor.MARBLE_TILE_SLAB.get(),
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModDecor.MARBLE_TILES.get(), 4)
                        .pattern("CC").pattern("CC").define('C', Items.CALCITE));
        tiles(output, ModDecor.CHECKERED_TILES.get(), ModDecor.CHECKERED_TILE_STAIRS.get(),
                ModDecor.CHECKERED_TILE_SLAB.get(),
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModDecor.CHECKERED_TILES.get(), 4)
                        .pattern("CB").pattern("BC").define('C', Items.CALCITE)
                        .define('B', Items.POLISHED_BLACKSTONE));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModDecor.VELVET_WALL_PANEL.get(), 5)
                .pattern("NWN").pattern("WWW").pattern("NWN")
                .define('N', Items.GOLD_NUGGET).define('W', Items.RED_WOOL)
                .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModDecor.WOOD_WALL_PANEL.get(), 5)
                .pattern("NWN").pattern("WWW").pattern("NWN")
                .define('N', Items.GOLD_NUGGET).define('W', Items.DARK_OAK_PLANKS)
                .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.GOLD_SKIRTING.get(), 6)
                .pattern("NNN").pattern("PPP")
                .define('N', Items.GOLD_NUGGET).define('P', Items.DARK_OAK_PLANKS)
                .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModDecor.CASINO_COLUMN.get(), 3)
                .pattern("C").pattern("C").pattern("C")
                .define('C', Items.CALCITE)
                .unlockedBy("has_calcite", has(Items.CALCITE)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.CRYSTAL_CHANDELIER.get())
                .pattern(" C ").pattern("NDN").pattern(" A ")
                .define('C', Items.CHAIN).define('N', Items.GOLD_NUGGET).define('D', Items.GLOWSTONE_DUST)
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD)).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ModDecor.GRAND_CHANDELIER.get())
                .requires(ModDecor.CRYSTAL_CHANDELIER.get(), 3).requires(Items.GOLD_INGOT)
                .unlockedBy("has_crystal_chandelier", has(ModDecor.CRYSTAL_CHANDELIER.get())).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.WALL_SCONCE.get(), 2)
                .pattern("C").pattern("N")
                .define('C', Items.CANDLE).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_candle", has(Items.CANDLE)).save(output);
        // Three wool over three planks would be the red bed, so the sofa takes a second row of wool.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.VELVET_SOFA.get(), 2)
                .pattern("RRR").pattern("RRR").pattern("P P")
                .define('R', Items.RED_WOOL).define('P', Items.DARK_OAK_PLANKS)
                .unlockedBy("has_red_wool", has(Items.RED_WOOL)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.COFFEE_TABLE.get())
                .pattern("SSS").pattern("N N")
                .define('S', Items.DARK_OAK_SLAB).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_dark_oak", has(Items.DARK_OAK_PLANKS)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.CARD_TABLE.get())
                .pattern("GGG").pattern("P P")
                .define('G', Items.GREEN_CARPET).define('P', Items.DARK_OAK_PLANKS)
                .unlockedBy("has_dark_oak", has(Items.DARK_OAK_PLANKS)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.VELVET_CURTAIN.get(), 6)
                .pattern("SSS").pattern("WWW").pattern("WWW")
                .define('S', Items.STICK).define('W', Items.RED_WOOL)
                .unlockedBy("has_red_wool", has(Items.RED_WOOL)).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ModDecor.GOLD_PLANTER.get())
                .requires(Items.FLOWER_POT).requires(Items.GOLD_INGOT).requires(Items.FERN)
                .unlockedBy("has_flower_pot", has(Items.FLOWER_POT)).save(output);
    }

    /** The buildings' facade and hall pieces: blackstone, red terracotta, wool, neon and a little gold. */
    private void architecture(RecipeOutput output) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ModDecor.CASINO_MARQUEE.get())
                .requires(ModDecor.NEON_LETTER.get(), 6).requires(ModDecor.MARQUEE_LIGHTS.get(), 2)
                .requires(Items.GOLD_INGOT)
                .unlockedBy("has_marquee_lights", has(ModDecor.MARQUEE_LIGHTS.get())).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ModDecor.CASINO_MARQUEE_SMALL.get())
                .requires(ModDecor.NEON_LETTER.get(), 4).requires(ModDecor.MARQUEE_LIGHTS.get())
                .requires(Items.GOLD_INGOT)
                .unlockedBy("has_marquee_lights", has(ModDecor.MARQUEE_LIGHTS.get())).save(output);
        List<ItemLike> chipWool = List.of(Items.RED_WOOL, Items.BLUE_WOOL, Items.BLACK_WOOL);
        for (int i = 0; i < ModDecor.CHIP_COLORS.size(); i++) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ModDecor.GIANT_CHIPS.get(i).get())
                    .requires(chipWool.get(i), 3).requires(Items.WHITE_WOOL, 2).requires(Items.GOLD_NUGGET)
                    .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET)).save(output);
        }
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ModDecor.BOLTCRAFT_BANNER.get())
                .requires(Items.RED_BANNER).requires(Items.GOLD_NUGGET).requires(Items.GLOWSTONE_DUST)
                .unlockedBy("has_red_banner", has(Items.RED_BANNER)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModDecor.GILDED_PANEL.get(), 8)
                .pattern("TTT").pattern("TNT").pattern("TTT")
                .define('T', Items.RED_TERRACOTTA).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModDecor.GILDED_CORNICE.get(), 8)
                .pattern("BBB").pattern("BNB").pattern("BBB")
                .define('B', Items.POLISHED_BLACKSTONE_BRICKS).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModDecor.COFFERED_CEILING.get(), 6)
                .pattern("PPP").pattern("PRP").pattern("PNP")
                .define('P', Items.DARK_OAK_PLANKS).define('R', Items.RED_WOOL).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_dark_oak", has(Items.DARK_OAK_PLANKS)).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModDecor.CARPET_STAIRS.get())
                .requires(Items.POLISHED_BLACKSTONE_BRICK_STAIRS).requires(Items.RED_CARPET)
                .unlockedBy("has_red_carpet", has(Items.RED_CARPET)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModDecor.GILDED_RAILING.get(), 4)
                .pattern("NSN").pattern("NSN")
                .define('N', Items.GOLD_NUGGET).define('S', Items.STICK)
                .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET)).save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ModDecor.GILDED_LANTERN.get())
                .requires(Items.LANTERN).requires(Items.GOLD_NUGGET, 2)
                .unlockedBy("has_lantern", has(Items.LANTERN)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModDecor.ONYX_COLUMN.get(), 3)
                .pattern("B").pattern("B").pattern("B")
                .define('B', Items.POLISHED_BLACKSTONE)
                .unlockedBy("has_polished_blackstone", has(Items.POLISHED_BLACKSTONE)).save(output);
    }

    /** A tile block, its stairs and slab, also cut in the stonecutter like stone. */
    private void tiles(RecipeOutput output, ItemLike block, ItemLike stairs, ItemLike slab, ShapedRecipeBuilder base) {
        base.unlockedBy("has_calcite", has(Items.CALCITE)).save(output);
        stairBuilder(stairs, Ingredient.of(block)).unlockedBy("has_tiles", has(block)).save(output);
        slab(output, RecipeCategory.BUILDING_BLOCKS, slab, block);
        stonecut(output, block, stairs, 1);
        stonecut(output, block, slab, 2);
    }

    /** Vanilla's stonecutter helper names recipes in the minecraft namespace, so these name their own. */
    private void stonecut(RecipeOutput output, ItemLike block, ItemLike result, int count) {
        String name = getConversionRecipeName(result, block) + "_stonecutting";
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(block), RecipeCategory.BUILDING_BLOCKS, result, count)
                .unlockedBy("has_tiles", has(block)).save(output, CasinoMod.id(name));
    }
}
