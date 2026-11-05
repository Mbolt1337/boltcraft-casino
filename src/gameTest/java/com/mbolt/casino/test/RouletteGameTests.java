package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.RouletteTableBlock;
import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.block.Protection;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.PendingPayouts;
import com.mbolt.casino.game.RouletteRounds;
import com.mbolt.casino.game.roulette.RouletteBet;
import com.mbolt.casino.game.roulette.RouletteRound;
import com.mbolt.casino.inventory.ChipWallet;
import com.mbolt.casino.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CasinoMod.MODID)
@PrefixGameTestTemplate(false)
public final class RouletteGameTests {
    private static final BlockPos TABLE = new BlockPos(1, 1, 1);
    private static final BlockPos VAULT = new BlockPos(4, 1, 1);

    private record Casino(RouletteTableBlockEntity table, VaultBlockEntity vault, ServerPlayer alice, ServerPlayer bob) { }

    private static Casino casino(GameTestHelper helper, int backing) {
        var state = ModBlocks.ROULETTE_TABLE.get().defaultBlockState();
        for (int part = 0; part < 4; part++) {
            helper.getLevel().setBlock(RouletteTableBlock.partPos(helper.absolutePos(TABLE), Direction.NORTH, part),
                    state.setValue(RouletteTableBlock.PART, part), Block.UPDATE_CLIENTS);
        }
        helper.setBlock(VAULT, ModBlocks.VAULT.get());
        ServerPlayer alice = TestPlayers.at(helper, "Alice", TABLE);
        ServerPlayer bob = TestPlayers.at(helper, "Bob", TABLE);
        RouletteTableBlockEntity table = helper.getBlockEntity(TABLE);
        VaultBlockEntity vault = helper.getBlockEntity(VAULT);
        table.setOwner(alice);
        vault.setOwner(alice);
        table.linkTo(vault);
        if (backing > 0) {
            vault.ledger().deposit("minecraft:diamond", backing, false, CasinoServerConfig.rates(), alice.getUUID(), 0);
            vault.ledger().deposit("minecraft:diamond", 20, true, CasinoServerConfig.rates(), alice.getUUID(), 1);
            vault.ledgerChanged();
        }
        ChipWallet.give(alice, 100);
        ChipWallet.give(bob, 100);
        return new Casino(table, vault, alice, bob);
    }

    @GameTest(template = "empty", timeoutTicks = 800)
    public static void twoPlayersShareARoundAndAnOfflineWinnerIsPaidOnce(GameTestHelper helper) {
        var casino = casino(helper, 100);
        var rounds = RouletteRounds.get(helper.getLevel().getServer());
        var red = new RouletteBet(RouletteBet.Type.RED, 0, 10);
        var black = new RouletteBet(RouletteBet.Type.BLACK, 0, 10);
        helper.assertTrue(rounds.place(casino.table(), casino.alice(), red) == null, "Alice red accepted");
        helper.assertTrue(rounds.place(casino.table(), casino.bob(), black) == null, "Bob black accepted");
        helper.assertValueEqual(casino.vault().ledger().reserved(), 20L, "Opposing bets share exposure");
        helper.assertValueEqual(ChipWallet.balance(casino.alice()), 90L, "one Alice stake charged");
        helper.assertValueEqual(ChipWallet.balance(casino.bob()), 90L, "one Bob stake charged");
        helper.assertTrue(Protection.breakRefusal(casino.alice(), casino.vault()) != null, "active bank protected");
        helper.runAfterDelay(CasinoServerConfig.ROULETTE_BETTING_SECONDS.get() * 20 + 165, () -> {
            var snapshot = casino.table().round().snapshot();
            helper.assertTrue(snapshot.phase() == RouletteRound.Phase.PAYOUT, "round reaches payout");
            long alice = casino.table().round().payouts().getOrDefault(casino.alice().getUUID(), 0L);
            long bob = casino.table().round().payouts().getOrDefault(casino.bob().getUUID(), 0L);
            var pending = PendingPayouts.get(helper.getLevel().getServer());
            helper.assertValueEqual(pending.owedTo(casino.alice().getUUID()), alice, "Alice offline payout");
            helper.assertValueEqual(pending.owedTo(casino.bob().getUUID()), bob, "Bob offline payout");
            helper.assertValueEqual(casino.vault().ledger().reserved(), 0L, "reserve released once");
            helper.assertValueEqual(casino.vault().ledger().outstanding(), 180L + alice + bob, "bank liabilities");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void removingAnyPartRefundsBothPlayersAndRemovesTheOtherParts(GameTestHelper helper) {
        var casino = casino(helper, 100);
        var rounds = RouletteRounds.get(helper.getLevel().getServer());
        var bet = new RouletteBet(RouletteBet.Type.NUMBER, 0, 5);
        rounds.place(casino.table(), casino.alice(), bet);
        rounds.place(casino.table(), casino.bob(), bet);
        helper.destroyBlock(TABLE.east().south());
        helper.runAfterDelay(5, () -> {
            var pending = PendingPayouts.get(helper.getLevel().getServer());
            helper.assertValueEqual(pending.owedTo(casino.alice().getUUID()), 5L, "Alice refunded");
            helper.assertValueEqual(pending.owedTo(casino.bob().getUUID()), 5L, "Bob refunded");
            helper.assertValueEqual(casino.vault().ledger().reserved(), 0L, "refund releases reserve");
            for (int part = 0; part < 4; part++) {
                helper.assertBlockNotPresent(ModBlocks.ROULETTE_TABLE.get(),
                        RouletteTableBlock.partPos(TABLE, Direction.NORTH, part));
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void bankNbtRestoresRouletteReservationsAndAnUnderfundedBetChargesNothing(GameTestHelper helper) {
        var empty = casino(helper, 0);
        var rounds = RouletteRounds.get(helper.getLevel().getServer());
        var bet = new RouletteBet(RouletteBet.Type.NUMBER, 0, 5);
        helper.assertTrue(rounds.place(empty.table(), empty.alice(), bet) != null, "empty vault refuses bet");
        helper.assertValueEqual(ChipWallet.balance(empty.alice()), 100L, "no debit");
        empty.vault().ledger().deposit("minecraft:diamond", 100, false, CasinoServerConfig.rates(),
                empty.alice().getUUID(), 0);
        helper.assertTrue(rounds.place(empty.table(), empty.alice(), bet) == null, "funded bet accepted");
        var vault = empty.vault();
        var registries = helper.getLevel().registryAccess();
        var copy = (VaultBlockEntity) BlockEntity.loadStatic(vault.getBlockPos(), vault.getBlockState(),
                vault.saveWithFullMetadata(registries), registries);
        helper.assertValueEqual(copy.ledger().rouletteBookings(), vault.ledger().rouletteBookings(), "bookings saved");
        helper.assertValueEqual(copy.ledger().reserved(), 180L, "reserved balance saved");
        helper.succeed();
    }
}
