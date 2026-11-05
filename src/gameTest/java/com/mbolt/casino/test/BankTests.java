package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.Protection;
import com.mbolt.casino.block.entity.CashierBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.economy.VaultLedger;
import com.mbolt.casino.inventory.ChipWallet;
import com.mbolt.casino.inventory.PlayerItems;
import com.mbolt.casino.item.ChipItem;
import com.mbolt.casino.menu.CashierMenu;
import com.mbolt.casino.menu.VaultMenu;
import com.mbolt.casino.network.CashierExchange;
import com.mbolt.casino.registry.ModBlocks;
import com.mbolt.casino.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CasinoMod.MODID)
@PrefixGameTestTemplate(false)
public final class BankTests {
    private static final BlockPos VAULT = new BlockPos(1, 1, 1);
    private static final BlockPos CASHIER = new BlockPos(3, 1, 1);
    private static final String DIAMOND = "minecraft:diamond";
    /** Menu actions are rate limited to one per four ticks. */
    private static final int AFTER_COOLDOWN = 5;

    private BankTests() {
    }

    private record Bank(ServerPlayer owner, VaultBlockEntity vault, CashierBlockEntity cashier, CashierMenu menu) {
    }

    private static Bank bank(GameTestHelper helper) {
        helper.setBlock(VAULT, ModBlocks.VAULT.get());
        helper.setBlock(CASHIER, ModBlocks.CASHIER.get());
        ServerPlayer owner = TestPlayers.at(helper, "Owner", CASHIER);
        VaultBlockEntity vault = helper.getBlockEntity(VAULT);
        CashierBlockEntity cashier = helper.getBlockEntity(CASHIER);
        vault.setOwner(owner);
        cashier.setOwner(owner);
        cashier.linkTo(vault);
        var menu = new CashierMenu(1, owner.getInventory(), helper.absolutePos(CASHIER));
        owner.containerMenu = menu;
        return new Bank(owner, vault, cashier, menu);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void tenDiamondsBecomeHundredChipsAndBack(GameTestHelper helper) {
        Bank bank = bank(helper);
        bank.owner().getInventory().add(new ItemStack(Items.DIAMOND, 10));

        bank.menu().exchange(new CashierExchange(1, DIAMOND, 10, true));
        helper.assertValueEqual(ChipWallet.balance(bank.owner()), 100L, "chips after buying");
        helper.assertValueEqual(PlayerItems.count(bank.owner(), Items.DIAMOND), 0L, "diamonds after buying");
        helper.assertValueEqual(bank.vault().ledger().outstanding(), 100L, "chips owed by the vault");

        helper.runAfterDelay(AFTER_COOLDOWN, () -> {
            bank.menu().exchange(new CashierExchange(1, DIAMOND, 0, false));
            helper.assertValueEqual(ChipWallet.balance(bank.owner()), 0L, "chips after cashing out");
            helper.assertValueEqual(PlayerItems.count(bank.owner(), Items.DIAMOND), 10L, "diamonds after cashing out");
            helper.assertValueEqual(bank.vault().ledger().outstanding(), 0L, "chips owed after cashing out");
            helper.assertValueEqual(bank.vault().ledger().count(DIAMOND), 0L, "diamonds left in the vault");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void repeatedRequestsInOneTickRunOnce(GameTestHelper helper) {
        Bank bank = bank(helper);
        bank.owner().getInventory().add(new ItemStack(Items.DIAMOND, 10));

        bank.menu().exchange(new CashierExchange(1, DIAMOND, 1, true));
        bank.menu().exchange(new CashierExchange(1, DIAMOND, 1, true));
        helper.assertValueEqual(ChipWallet.balance(bank.owner()), 10L, "one exchange per burst");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void strangerCannotBreakSomeoneElsesVault(GameTestHelper helper) {
        Bank bank = bank(helper);
        ServerPlayer stranger = TestPlayers.at(helper, "Stranger", VAULT);

        helper.assertTrue(Protection.breakRefusal(stranger, bank.vault()) != null, "stranger must be refused");
        helper.assertTrue(Protection.breakRefusal(bank.owner(), bank.vault()) == null, "owner may break");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void vaultWithChipsInCirculationCannotBeBroken(GameTestHelper helper) {
        Bank bank = bank(helper);
        bank.owner().getInventory().add(new ItemStack(Items.DIAMOND, 10));
        bank.menu().exchange(new CashierExchange(1, DIAMOND, 10, true));

        helper.assertTrue(Protection.breakRefusal(bank.owner(), bank.vault()) != null,
                "the owner must not break a vault backing chips");
        helper.runAfterDelay(AFTER_COOLDOWN, () -> {
            bank.menu().exchange(new CashierExchange(1, DIAMOND, 0, false));
            helper.assertTrue(Protection.breakRefusal(bank.owner(), bank.vault()) == null,
                    "once every chip is back the owner may break the vault");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void removedVaultDropsItsResourcesOnce(GameTestHelper helper) {
        Bank bank = bank(helper);
        bank.vault().ledger().deposit(DIAMOND, 10, false, CasinoServerConfig.rates(), bank.owner().getUUID(), 0);

        helper.destroyBlock(VAULT);
        helper.runAfterDelay(2, () -> {
            var area = new AABB(helper.absolutePos(VAULT)).inflate(2);
            long dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream()
                    .filter(entity -> entity.getItem().is(Items.DIAMOND))
                    .mapToLong(entity -> entity.getItem().getCount())
                    .sum();
            helper.assertValueEqual(dropped, 10L, "dropped diamonds");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void vaultLedgerSurvivesSaveAndLoad(GameTestHelper helper) {
        Bank bank = bank(helper);
        bank.vault().ledger().deposit(DIAMOND, 10, true, CasinoServerConfig.rates(), bank.owner().getUUID(), 7);

        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = bank.vault().saveWithFullMetadata(registries);
        BlockEntity loaded = BlockEntity.loadStatic(helper.absolutePos(VAULT), bank.vault().getBlockState(), saved,
                registries);
        helper.assertTrue(loaded instanceof VaultBlockEntity, "a vault must load back");
        VaultBlockEntity copy = (VaultBlockEntity) loaded;
        helper.assertValueEqual(copy.identity(), bank.vault().identity(), "vault identity");
        helper.assertValueEqual(copy.ledger().count(DIAMOND), 10L, "stored diamonds");
        helper.assertValueEqual(copy.ledger().outstanding(), 100L, "chips owed");
        helper.assertValueEqual(copy.ledger().journal().size(), 1, "journal entries");
        helper.assertTrue(copy.isOwner(bank.owner()), "owner");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void managerKeyClaimsAnUnownedVaultAndLinksACashier(GameTestHelper helper) {
        helper.setBlock(VAULT, ModBlocks.VAULT.get());
        helper.setBlock(CASHIER, ModBlocks.CASHIER.get());
        ServerPlayer player = TestPlayers.at(helper, "Finder", VAULT);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MANAGER_KEY.get()));
        VaultBlockEntity vault = helper.getBlockEntity(VAULT);
        CashierBlockEntity cashier = helper.getBlockEntity(CASHIER);
        helper.assertFalse(vault.hasOwner(), "a block set by a structure has no owner");

        useKeyOn(helper, player, VAULT);
        helper.assertTrue(vault.isOwner(player), "the key claims an unowned vault");
        useKeyOn(helper, player, CASHIER);
        helper.assertTrue(cashier.isOwner(player), "linking claims an unowned cashier");
        helper.assertTrue(cashier.linkedVault().orElse(null) == vault, "the cashier is linked to the vault");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void chipsThatDoNotFitAreDroppedNotLost(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.at(helper, "FullBag", VAULT);
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().items.set(slot, new ItemStack(Items.DIRT, 64));
        }
        player.getInventory().offhand.set(0, new ItemStack(Items.DIRT, 64));

        ChipWallet.give(player, 1234);
        helper.runAfterDelay(2, () -> {
            var area = new AABB(player.blockPosition()).inflate(3);
            long dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream()
                    .filter(entity -> entity.getItem().getItem() instanceof ChipItem)
                    .mapToLong(entity -> entity.getItem().getCount()
                            * ((ChipItem) entity.getItem().getItem()).value())
                    .sum();
            helper.assertValueEqual(dropped, 1234L, "value of dropped chips");
            helper.succeed();
        });
    }

    private static void useKeyOn(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        var hit = new BlockHitResult(pos.getCenter(), Direction.UP, pos, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    @GameTest(template = "empty")
    public static void fullJournalUpdatesWhenBalancesAndEntryCountStayTheSame(GameTestHelper helper) {
        Bank bank = bank(helper);
        VaultBlockEntity vault = bank.vault();
        var rates = CasinoServerConfig.rates();
        for (int entry = 0; entry < VaultLedger.JOURNAL_LIMIT; entry++) {
            vault.ledger().deposit(DIAMOND, 1, false, rates, bank.owner().getUUID(), entry);
        }
        vault.ledgerChanged();
        var menu = new VaultMenu(2, bank.owner().getInventory(), helper.absolutePos(VAULT));
        bank.owner().containerMenu = menu;
        menu.broadcastChanges();
        var before = menu.state();

        vault.ledger().deposit(DIAMOND, 1, false, rates, bank.owner().getUUID(), 201);
        vault.ledgerChanged();
        vault.ledger().withdrawProfit(DIAMOND, 1, rates, bank.owner().getUUID(), 202);
        vault.ledgerChanged();
        menu.broadcastChanges();
        var after = menu.state();
        helper.assertValueEqual(after.totals(), before.totals(), "balances are unchanged");
        helper.assertValueEqual(after.rows(), before.rows(), "resource rows are unchanged");
        helper.assertValueEqual(after.journalSize(), VaultLedger.JOURNAL_LIMIT, "journal stays full");
        helper.assertTrue(after.ledgerRevision() > before.ledgerRevision(), "new entries change the menu state");
        helper.assertTrue(!after.equals(before), "the owner receives an updated state even at the journal limit");

        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = vault.saveWithFullMetadata(registries);
        var copy = (VaultBlockEntity) BlockEntity.loadStatic(helper.absolutePos(VAULT), vault.getBlockState(), saved,
                registries);
        helper.assertValueEqual(copy.ledgerRevision(), after.ledgerRevision(), "revision survives reload");
        helper.assertValueEqual(copy.ledger().journal().getLast().time(), 202L, "latest entry survives reload");
        helper.succeed();
    }
}
