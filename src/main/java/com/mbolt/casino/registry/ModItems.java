package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.item.CasinoDrinkItem;
import com.mbolt.casino.item.ChipItem;
import com.mbolt.casino.item.GuideBookItem;
import com.mbolt.casino.item.ManagerKeyItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(CasinoMod.MODID);

    public static final DeferredItem<ChipItem> CHIP_1 = chip(1, Rarity.COMMON);
    public static final DeferredItem<ChipItem> CHIP_10 = chip(10, Rarity.COMMON);
    public static final DeferredItem<ChipItem> CHIP_100 = chip(100, Rarity.UNCOMMON);
    public static final DeferredItem<ChipItem> CHIP_1000 = chip(1000, Rarity.RARE);
    public static final DeferredItem<ManagerKeyItem> MANAGER_KEY = REGISTER.register("manager_key",
            () -> new ManagerKeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<GuideBookItem> GUIDE_BOOK = REGISTER.register("guide_book",
            () -> new GuideBookItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CERAMIC_CUP = REGISTER.registerSimpleItem("ceramic_cup",
            new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> CANAPE = food("canape", 3, .45f);
    public static final DeferredItem<Item> CLUB_SANDWICH = food("club_sandwich", 8, .7f);
    public static final DeferredItem<Item> BERRY_TART = food("berry_tart", 5, .4f);
    public static final DeferredItem<Item> CHOCOLATE_TRUFFLE = food("chocolate_truffle", 2, .7f);
    public static final DeferredItem<CasinoDrinkItem> BERRY_FIZZ = REGISTER.register("berry_fizz",
            () -> new CasinoDrinkItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(2).saturationModifier(.15f).alwaysEdible().usingConvertsTo(Items.GLASS_BOTTLE).build())));
    public static final DeferredItem<CasinoDrinkItem> SPICED_COCOA = REGISTER.register("spiced_cocoa",
            () -> new CasinoDrinkItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(.4f).alwaysEdible().usingConvertsTo(CERAMIC_CUP.get()).build())));

    public static final DeferredItem<BlockItem> VAULT = REGISTER.registerSimpleBlockItem(ModBlocks.VAULT);
    public static final DeferredItem<BlockItem> CASHIER = REGISTER.registerSimpleBlockItem(ModBlocks.CASHIER);
    public static final DeferredItem<BlockItem> SLOT_MACHINE = REGISTER.registerSimpleBlockItem(ModBlocks.SLOT_MACHINE);
    public static final DeferredItem<BlockItem> ROULETTE_TABLE = REGISTER.registerSimpleBlockItem(ModBlocks.ROULETTE_TABLE);

    private ModItems() {
    }

    public static List<Item> barMenu() {
        return List.of(CANAPE.get(), CLUB_SANDWICH.get(), BERRY_TART.get(), CHOCOLATE_TRUFFLE.get(),
                BERRY_FIZZ.get(), SPICED_COCOA.get());
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return REGISTER.registerSimpleItem(name, new Item.Properties().food(new FoodProperties.Builder()
                .nutrition(nutrition).saturationModifier(saturation).build()));
    }

    /** Chip items from the largest to the smallest value, matching {@code ChipMath.DENOMINATIONS}. */
    public static List<ChipItem> chipsByValue() {
        return List.of(CHIP_1000.get(), CHIP_100.get(), CHIP_10.get(), CHIP_1.get());
    }

    public static ChipItem chip(int value) {
        for (ChipItem chip : chipsByValue()) {
            if (chip.value() == value) {
                return chip;
            }
        }
        throw new IllegalArgumentException("No chip worth " + value);
    }

    private static DeferredItem<ChipItem> chip(int value, Rarity rarity) {
        return REGISTER.register("chip_" + value, () -> new ChipItem(value, new Item.Properties().rarity(rarity)));
    }
}
