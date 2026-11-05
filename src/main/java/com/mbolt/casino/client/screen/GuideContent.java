package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.client.screen.GuideBookScreen.Block;
import com.mbolt.casino.client.screen.GuideBookScreen.Gap;
import com.mbolt.casino.client.screen.GuideBookScreen.Heading;
import com.mbolt.casino.client.screen.GuideBookScreen.Icons;
import com.mbolt.casino.client.screen.GuideBookScreen.Link;
import com.mbolt.casino.client.screen.GuideBookScreen.Paragraph;
import com.mbolt.casino.client.screen.GuideBookScreen.Pay;
import com.mbolt.casino.client.screen.GuideBookScreen.Rate;
import com.mbolt.casino.client.screen.GuideBookScreen.Recipe;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.slot.SlotMachineLogic;
import com.mbolt.casino.game.slot.SlotSymbol;
import com.mbolt.casino.registry.ModDecor;
import com.mbolt.casino.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What the guide book says, section by section (PROMPT.md 5.3). Every text is a translation key
 * {@code guide.boltcraft_casino.<section>.<n>}, so a resource pack can rewrite the book; recipes, rates and payouts
 * come from the game, so the book always matches the server it is read on.
 */
final class GuideContent {
    private static final String PREFIX = "guide." + CasinoMod.MODID + ".";

    private final Font font;
    private final List<List<Block>> sections = new ArrayList<>();
    private List<Block> section;

    private GuideContent(Font font) {
        this.font = font;
    }

    static List<List<Block>> sections(Font font) {
        GuideContent content = new GuideContent(font);
        content.build();
        return content.sections;
    }

    private void build() {
        String url = CasinoServerConfig.SUPPORT_URL.get();
        if (CasinoServerConfig.SHOW_SUPPORT_PAGE.get() && !url.isBlank()) {
            section("support");
            text("support", 1);
            text("support", 2);
            section.add(new Link(Component.translatable(PREFIX + "support.link"), url));
        }

        section("about");
        text("about", 1);
        icons(ModItems.CHIP_1.get(), ModItems.CHIP_10.get(), ModItems.CHIP_100.get(), ModItems.CHIP_1000.get());
        text("about", 2);
        text("about", 3);

        section("cashier");
        text("cashier", 1);
        recipe(ModItems.CASHIER.get());
        text("cashier", 2);
        for (Map.Entry<String, Long> rate : CasinoServerConfig.rates().entries().entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(rate.getKey()));
            section.add(new Rate(new ItemStack(item), rate.getValue()));
        }
        section.add(new Gap(4));
        text("cashier", 3);

        section("vault");
        text("vault", 1);
        recipe(ModItems.VAULT.get());
        text("vault", 2);
        recipe(ModItems.MANAGER_KEY.get());
        text("vault", 3);

        section("slot_machine");
        text("slot_machine", 1);
        recipe(ModItems.SLOT_MACHINE.get());
        text("slot_machine", 2);
        SlotMachineLogic logic = CasinoServerConfig.slotLogic();
        for (int symbol = SlotSymbol.values().length - 1; symbol >= 0; symbol--) {
            section.add(new Pay(symbol, false, logic.payout(symbol)));
        }
        if (logic.twoCherryPayout() > 0) {
            section.add(new Pay(SlotSymbol.CHERRY.ordinal(), true, logic.twoCherryPayout()));
        }
        section.add(new Gap(4));
        text("slot_machine", 3, String.format(Locale.ROOT, "%.1f%%", logic.exactRtp() * 100));

        section("roulette");
        text("roulette", 1);
        recipe(ModItems.ROULETTE_TABLE.get());
        text("roulette", 2);
        text("roulette", 3, CasinoServerConfig.ROULETTE_MAX_BET.get());

        section("decor");
        text("decor", 1);
        icons(ModDecor.NEON_LETTER.get(), ModDecor.NEON_SIGN_BLOCKS.get(1).get(), ModDecor.MARQUEE_LIGHTS.get(),
                ModDecor.CARPET_BLOCKS.get(0).get(), ModDecor.CASINO_COLUMN.get(), ModDecor.CRYSTAL_CHANDELIER.get());
        icons(ModDecor.BAR_COUNTER.get(), ModDecor.BAR_STOOL.get(), ModDecor.VELVET_SOFA.get(),
                ModDecor.VELVET_ROPES.get(0).get(), ModDecor.MARBLE_TILES.get(), ModDecor.GOLD_PLANTER.get());
        text("decor", 2);
        text("decor", 3);
        recipe(ModDecor.NEON_LETTER.get());

        section("hospitality");
        text("hospitality", 1);
        icons(ModItems.CANAPE.get(), ModItems.CLUB_SANDWICH.get(), ModItems.BERRY_TART.get(),
                ModItems.CHOCOLATE_TRUFFLE.get(), ModItems.BERRY_FIZZ.get(), ModItems.SPICED_COCOA.get());
        recipe(ModItems.SPICED_COCOA.get());
        text("hospitality", 2);
        icons(ModDecor.DINING_CHAIR.get(), ModDecor.SERVING_TRAY.get(), ModDecor.TABLE_LAMP.get(),
                ModDecor.BOTTLE_SHELF.get());
        recipe(ModDecor.SERVING_TRAY.get());
        text("hospitality", 3);

        section("world");
        text("world", 1);
        text("world", 2);
        icons(ModItems.MANAGER_KEY.get(), ModItems.VAULT.get(), ModItems.CASHIER.get(), ModItems.SLOT_MACHINE.get());
        text("world", 3);

        section("authors");
        text("authors", 1);
        text("authors", 2);
        text("authors", 3);
    }

    private void section(String name) {
        section = new ArrayList<>();
        sections.add(section);
        section.add(new Heading(Component.translatable(PREFIX + name + ".title")));
    }

    private void text(String name, int paragraph, Object... arguments) {
        Component text = Component.translatable(PREFIX + name + "." + paragraph, arguments);
        section.add(new Paragraph(GuideBookScreen.wrap(font, text)));
        section.add(new Gap(4));
    }

    private void icons(ItemLike... items) {
        section.add(new Icons(Arrays.stream(items).map(ItemStack::new).toList()));
        section.add(new Gap(2));
    }

    /** The crafting recipe the server knows for this item, if it has one. */
    private void recipe(ItemLike result) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            ItemStack output = recipe.getResultItem(level.registryAccess());
            if (!output.is(result.asItem())) {
                continue;
            }
            Ingredient[] grid = new Ingredient[9];
            Arrays.fill(grid, Ingredient.EMPTY);
            List<Ingredient> ingredients = recipe.getIngredients();
            if (recipe instanceof ShapedRecipe shaped) {
                for (int row = 0; row < shaped.getHeight(); row++) {
                    for (int column = 0; column < shaped.getWidth(); column++) {
                        grid[row * 3 + column] = ingredients.get(row * shaped.getWidth() + column);
                    }
                }
            } else {
                for (int i = 0; i < Math.min(9, ingredients.size()); i++) {
                    grid[i] = ingredients.get(i);
                }
            }
            section.add(new Recipe(List.of(grid), output.copy()));
            section.add(new Gap(4));
            return;
        }
    }
}
