package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.SlotMachineBlock;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.CasinoRandom;
import com.mbolt.casino.game.PendingPayouts;
import com.mbolt.casino.inventory.ChipWallet;
import com.mbolt.casino.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CasinoMod.MODID)
@PrefixGameTestTemplate(false)
public final class SlotTests {
    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);
    private static final BlockPos VAULT = new BlockPos(3, 1, 1);
    private static final String DIAMOND = "minecraft:diamond";
    /** 100 diamonds at the default rate back the top prize of a 1-chip bet twice over. */
    private static final int BACKING_DIAMONDS = 100;
    private static final int SETTLE_MARGIN_TICKS = 5;

    private SlotTests() {
    }

    private record Casino(ServerPlayer player, VaultBlockEntity vault, SlotMachineBlockEntity machine) {
    }

    private static Casino casino(GameTestHelper helper, int backingDiamonds) {
        BlockState lower = ModBlocks.SLOT_MACHINE.get().defaultBlockState();
        helper.setBlock(MACHINE.above(), lower.setValue(SlotMachineBlock.HALF, DoubleBlockHalf.UPPER));
        helper.getLevel().setBlock(helper.absolutePos(MACHINE), lower, Block.UPDATE_CLIENTS);
        helper.setBlock(VAULT, ModBlocks.VAULT.get());
        ServerPlayer player = TestPlayers.at(helper, "Gambler", MACHINE);
        VaultBlockEntity vault = helper.getBlockEntity(VAULT);
        SlotMachineBlockEntity machine = helper.getBlockEntity(MACHINE);
        vault.setOwner(player);
        machine.setOwner(player);
        machine.linkTo(vault);
        if (backingDiamonds > 0) {
            vault.ledger().deposit(DIAMOND, backingDiamonds, false, CasinoServerConfig.rates(), player.getUUID(), 0);
        }
        ChipWallet.give(player, 10);
        return new Casino(player, vault, machine);
    }

    private static int settleDelay() {
        return CasinoServerConfig.SLOT_SPIN_DURATION_TICKS.get() + SETTLE_MARGIN_TICKS;
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void spinTakesTheBetAndSettlesTheChosenPayoutOnce(GameTestHelper helper) {
        CasinoRandom.reseed(731);
        Casino casino = casino(helper, BACKING_DIAMONDS);

        helper.assertTrue(casino.machine().spin(casino.player(), 1) == null, "the spin must start");
        long payout = casino.machine().payout();
        helper.assertValueEqual(ChipWallet.balance(casino.player()), 9L, "balance right after betting");
        helper.assertTrue(casino.vault().ledger().reserved() >= 400, "the top prize is reserved");

        helper.runAfterDelay(settleDelay(), () -> {
            helper.assertFalse(casino.machine().spinning(), "the machine stops");
            helper.assertValueEqual(casino.vault().ledger().reserved(), 0L, "reservation released");
            // Fake players are never in the player list, so the payout waits for them like for an offline player.
            long owed = PendingPayouts.get(helper.getLevel().getServer()).owedTo(casino.player().getUUID());
            helper.assertValueEqual(owed, payout, "payout owed to the player");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void spinningMachineRefusesAnotherBet(GameTestHelper helper) {
        Casino casino = casino(helper, BACKING_DIAMONDS);

        helper.assertTrue(casino.machine().spin(casino.player(), 1) == null, "first spin starts");
        helper.assertTrue(casino.machine().spin(casino.player(), 1) != null, "second spin is refused");
        helper.assertValueEqual(ChipWallet.balance(casino.player()), 9L, "only one bet taken");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void machineRefusesBetsTheVaultCannotCover(GameTestHelper helper) {
        Casino casino = casino(helper, 1);

        helper.assertTrue(casino.machine().spin(casino.player(), 1) != null, "10 chips of backing cannot cover 400");
        helper.assertValueEqual(ChipWallet.balance(casino.player()), 10L, "no bet taken");
        helper.assertValueEqual(casino.vault().ledger().reserved(), 0L, "nothing reserved");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void machineRefusesBetsNotOnTheList(GameTestHelper helper) {
        Casino casino = casino(helper, BACKING_DIAMONDS);

        helper.assertTrue(casino.machine().spin(casino.player(), 7) != null, "7 is not a configured bet");
        helper.assertTrue(casino.machine().spin(casino.player(), -1) != null, "negative bets are refused");
        helper.assertValueEqual(ChipWallet.balance(casino.player()), 10L, "no bet taken");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void brokenMachineRefundsTheBet(GameTestHelper helper) {
        // A 5-chip bet reserves 2000 chips of backing.
        Casino casino = casino(helper, 250);
        helper.assertTrue(casino.machine().spin(casino.player(), 5) == null, "spin starts");

        helper.destroyBlock(MACHINE);
        helper.runAfterDelay(SETTLE_MARGIN_TICKS, () -> {
            helper.assertValueEqual(casino.vault().ledger().reserved(), 0L, "reservation released");
            long owed = PendingPayouts.get(helper.getLevel().getServer()).owedTo(casino.player().getUUID());
            helper.assertValueEqual(owed, 5L, "bet refunded");
            helper.assertBlockNotPresent(ModBlocks.SLOT_MACHINE.get(), MACHINE.above());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void roundSurvivesSaveAndLoadOfTheWorldData(GameTestHelper helper) {
        Casino casino = casino(helper, BACKING_DIAMONDS);
        helper.assertTrue(casino.machine().spin(casino.player(), 1) == null, "spin starts");
        var registries = helper.getLevel().registryAccess();
        var saved = casino.machine().saveWithFullMetadata(registries);

        helper.assertTrue(saved.getBoolean("Spinning"), "the spin is persisted with the machine");
        helper.assertTrue(saved.hasUUID("Round"), "the round id is persisted with the machine");
        helper.runAfterDelay(settleDelay(), helper::succeed);
    }
}
