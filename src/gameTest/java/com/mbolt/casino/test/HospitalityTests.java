package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.decor.TabletopSupport;
import com.mbolt.casino.block.entity.ServingTrayBlockEntity;
import com.mbolt.casino.registry.ModDecor;
import com.mbolt.casino.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CasinoMod.MODID)
@PrefixGameTestTemplate(false)
public final class HospitalityTests {
    private static final BlockPos TABLE = new BlockPos(2, 1, 2);
    private static final BlockPos TRAY = TABLE.above();

    private HospitalityTests() {
    }

    @GameTest(template = "empty")
    public static void trayHasFourPersistentServings(GameTestHelper helper) {
        var tray = tray(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var food = new ItemStack(ModItems.CANAPE.get(), 6);
        food.set(DataComponents.CUSTOM_NAME, Component.literal("Chef's canape"));
        player.setItemInHand(InteractionHand.MAIN_HAND, food);
        for (int slot = 0; slot < 4; slot++) {
            helper.assertTrue(tray.serve(player, slot), "one serving enters slot " + slot);
        }
        helper.assertFalse(tray.serve(player, 0), "a full tray refuses a fifth serving");
        helper.assertValueEqual(food.getCount(), 2, "only four items were consumed");
        var saved = tray.saveWithoutMetadata(helper.getLevel().registryAccess());
        var restored = new ServingTrayBlockEntity(helper.absolutePos(TRAY), tray.getBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        for (int slot = 0; slot < 4; slot++) {
            helper.assertTrue(restored.getItem(slot).is(ModItems.CANAPE.get()), "food survives saving");
            helper.assertValueEqual(restored.getItem(slot).getCount(), 1, "one item per slot");
            helper.assertTrue(restored.getItem(slot).has(DataComponents.CUSTOM_NAME), "item components survive");
        }
        helper.assertFalse(tray.canPlaceItem(0, new ItemStack(Items.DIAMOND)), "non-food is not served");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinksReturnTheirVesselsExactlyOnce(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var soda = new ItemStack(ModItems.BERRY_FIZZ.get(), 2);
        helper.assertTrue(soda.getUseAnimation() == UseAnim.DRINK, "soda is drunk instead of bitten");
        ItemStack remaining = soda.finishUsingItem(helper.getLevel(), player);
        helper.assertValueEqual(remaining.getCount(), 1, "one soda remains");
        helper.assertValueEqual(player.getInventory().countItem(Items.GLASS_BOTTLE), 1, "one bottle returns");
        var cocoa = new ItemStack(ModItems.SPICED_COCOA.get());
        ItemStack cup = cocoa.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(cup.is(ModItems.CERAMIC_CUP.get()), "the last cocoa returns a reusable cup");
        helper.assertValueEqual(cup.getCount(), 1, "one cup, without a duplicate in inventory");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.CERAMIC_CUP.get()), 0, "no duplicate cup");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lowTablePropsRestOnTheSurface(GameTestHelper helper) {
        helper.setBlock(TABLE, ModDecor.COFFEE_TABLE.get());
        var level = helper.getLevel();
        var pos = helper.absolutePos(TRAY);
        var state = Block.updateFromNeighbourShapes(ModDecor.SERVING_TRAY.get().defaultBlockState(), level, pos);
        level.setBlock(pos, state, 3);
        helper.assertValueEqual(state.getValue(TabletopSupport.LOWERING), 8, "half-height table lowers the tray");
        helper.assertValueEqual(TabletopSupport.lowering(level, pos), 8, "support height is measured");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(TABLE.north()).getCenter());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CANAPE.get()));
        clickSupport(helper, player);
        ServingTrayBlockEntity tray = helper.getBlockEntity(TRAY);
        helper.assertFalse(tray.isEmpty(), "clicking the low table reaches its tray");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        clickSupport(helper, player);
        helper.assertValueEqual(player.getInventory().countItem(ModItems.CANAPE.get()), 1,
                "a serving can be taken through the low table's hitbox");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaSlabCanServeAndReleaseATray(GameTestHelper helper) {
        helper.setBlock(TABLE, Blocks.DARK_OAK_SLAB);
        var level = helper.getLevel();
        var pos = helper.absolutePos(TRAY);
        level.setBlock(pos, Block.updateFromNeighbourShapes(ModDecor.SERVING_TRAY.get().defaultBlockState(),
                level, pos), 3);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(TABLE.north()).getCenter());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CANAPE.get()));
        clickSupport(helper, player);
        ServingTrayBlockEntity tray = helper.getBlockEntity(TRAY);
        helper.assertFalse(tray.isEmpty(), "a vanilla slab forwards the click to the lowered tray");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        clickSupport(helper, player);
        helper.assertBlockPresent(Blocks.AIR, TRAY);
        helper.assertValueEqual(dropped(helper, ModItems.CANAPE.get()), 1, "one serving drops on pickup");
        helper.assertValueEqual(dropped(helper, ModDecor.SERVING_TRAY.get().asItem()), 1, "one tray drops");
        helper.succeed();
    }

    private static void clickSupport(GameTestHelper helper, Player player) {
        BlockPos pos = helper.absolutePos(TABLE);
        var hit = new BlockHitResult(pos.getCenter(), Direction.UP, pos, false);
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.isCanceled(), "the support click is handled by the tray");
    }

    @GameTest(template = "empty")
    public static void removingSupportDropsTheTrayContentsOnce(GameTestHelper helper) {
        ServingTrayBlockEntity tray = tray(helper);
        for (int slot = 0; slot < 4; slot++) {
            tray.setItem(slot, new ItemStack(ModItems.BERRY_TART.get()));
        }
        helper.getLevel().destroyBlock(helper.absolutePos(TABLE), true);
        helper.runAfterDelay(3, () -> {
            helper.assertBlockPresent(Blocks.AIR, TRAY);
            helper.assertValueEqual(dropped(helper, ModItems.BERRY_TART.get()), 4, "four servings survive");
            helper.assertValueEqual(dropped(helper, ModDecor.SERVING_TRAY.get().asItem()), 1, "one tray drops");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void takingAServingCannotDuplicateIt(GameTestHelper helper) {
        var tray = tray(helper);
        tray.setItem(0, new ItemStack(ModItems.CLUB_SANDWICH.get()));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(tray.take(player, 0), "the first take succeeds");
        helper.assertFalse(tray.take(player, 0), "the same slot cannot be taken twice");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.CLUB_SANDWICH.get()), 1,
                "exactly one sandwich was delivered");
        helper.succeed();
    }

    private static ServingTrayBlockEntity tray(GameTestHelper helper) {
        helper.setBlock(TABLE, Blocks.STONE);
        helper.setBlock(TRAY, ModDecor.SERVING_TRAY.get());
        return helper.getBlockEntity(TRAY);
    }

    private static int dropped(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                        new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8), entity -> entity.getItem().is(item))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }
}
