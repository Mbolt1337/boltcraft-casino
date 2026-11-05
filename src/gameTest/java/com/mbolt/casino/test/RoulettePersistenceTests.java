package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.game.RouletteRoundNbt;
import com.mbolt.casino.game.roulette.RouletteBet;
import com.mbolt.casino.game.roulette.RouletteRound;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.UUID;

@GameTestHolder(CasinoMod.MODID)
@PrefixGameTestTemplate(false)
public final class RoulettePersistenceTests {
    private RoulettePersistenceTests() {
    }

    @GameTest(template = "empty")
    public static void roundNbtKeepsTheChosenNumberPlayersAndSettlementState(GameTestHelper helper) {
        var round = new RouletteRound(UUID.randomUUID(), 400, 160, 60);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        var first = new RouletteRound.PlacedBet(UUID.randomUUID(), alice,
                new RouletteBet(RouletteBet.Type.NUMBER, 0, 10));
        var second = new RouletteRound.PlacedBet(UUID.randomUUID(), bob,
                new RouletteBet(RouletteBet.Type.RED, 0, 20));
        round.place(first, 0, 100, 20000, 500, 20000);
        round.place(second, 0, 100, 20000, 500, 20000);
        var bettingCopy = RouletteRoundNbt.load(RouletteRoundNbt.save(round));
        helper.assertValueEqual(bettingCopy.snapshot(), round.snapshot(), "all pending stakes survive NBT");
        bettingCopy.advance(400, bound -> 0);
        var spinningCopy = RouletteRoundNbt.load(RouletteRoundNbt.save(bettingCopy));
        helper.assertValueEqual(spinningCopy.snapshot().number(), 0, "zero is a persisted result");
        spinningCopy.advance(560, bound -> { throw new AssertionError("The result must not be drawn again"); });
        helper.assertValueEqual(spinningCopy.payouts(), Map.of(alice, 360L), "the original winner is preserved");
        helper.assertTrue(spinningCopy.settle(), "first settlement");
        var settledCopy = RouletteRoundNbt.load(RouletteRoundNbt.save(spinningCopy));
        helper.assertFalse(settledCopy.settle(), "a restored settled round cannot settle again");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void corruptRoundDoesNotSilentlyLoseStakesOrChangeItsResult(GameTestHelper helper) {
        var round = new RouletteRound(UUID.randomUUID(), 400, 160, 60);
        var bet = new RouletteRound.PlacedBet(UUID.randomUUID(), UUID.randomUUID(),
                new RouletteBet(RouletteBet.Type.NUMBER, 0, 10));
        round.place(bet, 0, 100, 20000, 500, 20000);
        round.advance(400, bound -> 0);
        var saved = RouletteRoundNbt.save(round);
        saved.remove("Number");
        boolean rejected = false;
        try {
            RouletteRoundNbt.load(saved);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "missing result must be reported instead of silently becoming zero");
        helper.succeed();
    }
}
