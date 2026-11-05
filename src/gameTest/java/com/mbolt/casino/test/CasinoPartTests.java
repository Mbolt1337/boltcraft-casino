package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.RouletteTableBlock;
import com.mbolt.casino.block.SlotMachineBlock;
import com.mbolt.casino.block.entity.CasinoPartBlockEntity;
import com.mbolt.casino.registry.ModBlockEntities;
import com.mbolt.casino.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(CasinoMod.MODID)
@PrefixGameTestTemplate(false)
public final class CasinoPartTests {
    private CasinoPartTests() {
    }

    @GameTest(template = "empty")
    public static void companionPartsReloadWithoutCreatingAnotherGame(GameTestHelper helper) {
        var slot = ModBlocks.SLOT_MACHINE.get();
        var roulette = ModBlocks.ROULETTE_TABLE.get();
        var states = List.of(slot.defaultBlockState().setValue(SlotMachineBlock.HALF, DoubleBlockHalf.UPPER),
                roulette.defaultBlockState().setValue(RouletteTableBlock.PART, 1));
        for (var state : states) {
            var pos = helper.absolutePos(new BlockPos(2, 2, 2));
            var part = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
            helper.assertTrue(part instanceof CasinoPartBlockEntity, "companion part has an inert entity");
            var saved = part.saveWithFullMetadata(helper.getLevel().registryAccess());
            var loaded = BlockEntity.loadStatic(pos, state, saved, helper.getLevel().registryAccess());
            helper.assertTrue(loaded instanceof CasinoPartBlockEntity, "chunk reload restores an inert part");
            helper.assertTrue(state.getTicker(helper.getLevel(), ModBlockEntities.CASINO_PART.get()) == null,
                    "companion part never ticks another round");
        }
        helper.succeed();
    }
}
