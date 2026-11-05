package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.decor.BarCounterBlock;
import com.mbolt.casino.block.decor.BoardBlock;
import com.mbolt.casino.block.decor.CounterShape;
import com.mbolt.casino.block.decor.NeonColor;
import com.mbolt.casino.block.decor.NeonGlyph;
import com.mbolt.casino.block.decor.NeonLetterBlock;
import com.mbolt.casino.block.decor.NeonPanelBlock;
import com.mbolt.casino.block.decor.StackPart;
import com.mbolt.casino.block.decor.StackingBlock;
import com.mbolt.casino.block.decor.VelvetSofaBlock;
import com.mbolt.casino.block.entity.CashierBlockEntity;
import com.mbolt.casino.game.GuideDeliveries;
import com.mbolt.casino.registry.ModBlocks;
import com.mbolt.casino.registry.ModDecor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * Decor that joins its neighbours, boards that break as one, the neon letter picker, building fixtures and the
 * one-time guide book.
 */
@GameTestHolder(CasinoMod.MODID)
@PrefixGameTestTemplate(false)
public final class DecorTests {
    private DecorTests() {
    }

    @GameTest(template = "empty")
    public static void neonBoardBreaksAsOneAndDropsOnce(GameTestHelper helper) {
        NeonPanelBlock board = ModDecor.NEON_PANEL_BOLTCRAFT.get();
        BlockState state = board.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        BlockPos leftEnd = new BlockPos(6, 2, 2);
        for (int part = 0; part < board.width(); part++) {
            // Facing north, the viewer's right is west.
            helper.setBlock(leftEnd.west(part), state.setValue(NeonPanelBlock.PART, part));
        }
        helper.destroyBlock(leftEnd.west(2));
        helper.runAfterDelay(2, () -> {
            for (int part = 0; part < board.width(); part++) {
                helper.assertBlockNotPresent(board, leftEnd.west(part));
            }
            helper.assertValueEqual(dropped(helper, board.asItem()), 1, "boards dropped");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void giantChipBreaksAsOneAndDropsOnce(GameTestHelper helper) {
        BoardBlock chip = ModDecor.GIANT_CHIPS.get(0).get();
        BlockState state = chip.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        BlockPos anchor = new BlockPos(4, 1, 2);
        for (int part = 0; part < chip.parts(); part++) {
            helper.setBlock(BoardBlock.partPos(anchor, Direction.NORTH, chip.width(), part),
                    state.setValue(BoardBlock.PART, part));
        }
        // The middle of the chip, not the bottom left part that carries the drop.
        helper.destroyBlock(BoardBlock.partPos(anchor, Direction.NORTH, chip.width(), 4));
        helper.runAfterDelay(2, () -> {
            for (int part = 0; part < chip.parts(); part++) {
                helper.assertBlockNotPresent(chip, BoardBlock.partPos(anchor, Direction.NORTH, chip.width(), part));
            }
            helper.assertValueEqual(dropped(helper, chip.asItem()), 1, "chips dropped");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void gildedPanelsJoinIntoOneField(GameTestHelper helper) {
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), ModDecor.GILDED_PANEL.get());
        }
        helper.setBlock(new BlockPos(2, 2, 1), ModDecor.GILDED_PANEL.get());
        settle(helper, new BlockPos(1, 1, 1), new BlockPos(3, 2, 1));
        helper.assertBlockProperty(new BlockPos(1, 1, 1), PipeBlock.EAST, true);
        helper.assertBlockProperty(new BlockPos(1, 1, 1), PipeBlock.WEST, false);
        helper.assertBlockProperty(new BlockPos(2, 1, 1), PipeBlock.WEST, true);
        helper.assertBlockProperty(new BlockPos(2, 1, 1), PipeBlock.UP, true);
        helper.assertBlockProperty(new BlockPos(2, 2, 1), PipeBlock.DOWN, true);
        helper.assertBlockProperty(new BlockPos(3, 1, 1), PipeBlock.UP, false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void barCountersTurnCorners(GameTestHelper helper) {
        BlockState counter = ModDecor.BAR_COUNTER.get().defaultBlockState();
        BlockPos outer = new BlockPos(2, 1, 2);
        helper.setBlock(outer, counter.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(outer.south(), counter.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        BlockPos inner = new BlockPos(5, 1, 3);
        helper.setBlock(inner, counter.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(inner.north(), counter.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        helper.assertBlockProperty(outer, BarCounterBlock.SHAPE, CounterShape.OUTER_LEFT);
        helper.assertBlockProperty(inner, BarCounterBlock.SHAPE, CounterShape.INNER_RIGHT);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sofasJoinAndKeepTheirEndArms(GameTestHelper helper) {
        BlockState sofa = ModDecor.VELVET_SOFA.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), sofa);
        }
        settle(helper, new BlockPos(1, 1, 1), new BlockPos(3, 1, 1));
        // Facing north, the sitter's left hand is to the west.
        helper.assertBlockProperty(new BlockPos(1, 1, 1), VelvetSofaBlock.ARM_LEFT, true);
        helper.assertBlockProperty(new BlockPos(1, 1, 1), VelvetSofaBlock.ARM_RIGHT, false);
        helper.assertBlockProperty(new BlockPos(2, 1, 1), VelvetSofaBlock.ARM_LEFT, false);
        helper.assertBlockProperty(new BlockPos(2, 1, 1), VelvetSofaBlock.ARM_RIGHT, false);
        helper.assertBlockProperty(new BlockPos(3, 1, 1), VelvetSofaBlock.ARM_RIGHT, true);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void columnsStackAndRopesConnect(GameTestHelper helper) {
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(1, y, 1), ModDecor.CASINO_COLUMN.get());
        }
        helper.setBlock(new BlockPos(3, 1, 1), ModDecor.VELVET_ROPES.get(0).get());
        helper.setBlock(new BlockPos(4, 1, 1), ModDecor.VELVET_ROPES.get(2).get());
        settle(helper, new BlockPos(1, 1, 1), new BlockPos(4, 3, 1));
        helper.assertBlockProperty(new BlockPos(1, 1, 1), StackingBlock.PART, StackPart.BOTTOM);
        helper.assertBlockProperty(new BlockPos(1, 2, 1), StackingBlock.PART, StackPart.MIDDLE);
        helper.assertBlockProperty(new BlockPos(1, 3, 1), StackingBlock.PART, StackPart.TOP);
        helper.assertBlockProperty(new BlockPos(3, 1, 1), PipeBlock.EAST, true);
        helper.assertBlockProperty(new BlockPos(4, 1, 1), PipeBlock.WEST, true);
        helper.assertBlockProperty(new BlockPos(4, 1, 1), PipeBlock.EAST, false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void neonLetterTakesTheCharacterPickedNearby(GameTestHelper helper) {
        BlockPos letter = new BlockPos(1, 1, 1);
        helper.setBlock(letter, ModDecor.NEON_LETTER.get());
        var near = TestPlayers.at(helper, "Signwriter", new BlockPos(1, 1, 3));
        var far = TestPlayers.at(helper, "Passerby", new BlockPos(1, 1, 3));
        far.setPos(far.position().add(0, 0, 40));
        int seven = NeonGlyph.DIGIT_7.ordinal();
        NeonLetterBlock.choose(far, helper.absolutePos(letter), seven);
        helper.assertBlockProperty(letter, NeonLetterBlock.GLYPH, 0);
        NeonLetterBlock.choose(near, helper.absolutePos(letter), seven);
        helper.assertBlockProperty(letter, NeonLetterBlock.GLYPH, seven);
        NeonLetterBlock.choose(near, helper.absolutePos(letter), NeonGlyph.values().length);
        helper.assertBlockProperty(letter, NeonLetterBlock.GLYPH, seven);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void buildingFixturesDropNothing(GameTestHelper helper) {
        BlockPos fixture = new BlockPos(1, 1, 1);
        BlockPos owned = new BlockPos(3, 1, 1);
        helper.setBlock(fixture, ModBlocks.CASHIER.get());
        helper.setBlock(owned, ModBlocks.CASHIER.get());
        CashierBlockEntity cashier = helper.getBlockEntity(fixture);
        cashier.markFixture();
        // The helper breaks blocks without loot; break them the way a player's pickaxe does, with drops.
        helper.getLevel().destroyBlock(helper.absolutePos(fixture), true);
        helper.getLevel().destroyBlock(helper.absolutePos(owned), true);
        helper.runAfterDelay(2, () -> {
            helper.assertValueEqual(dropped(helper, ModBlocks.CASHIER.get().asItem()), 1, "cashiers dropped");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void dyesPickTheNearestNeon(GameTestHelper helper) {
        helper.assertValueEqual(NeonColor.fromDye(DyeColor.LIGHT_BLUE), NeonColor.CYAN, "light blue dye");
        helper.assertValueEqual(NeonColor.fromDye(DyeColor.LIME), NeonColor.GREEN, "lime dye");
        helper.assertValueEqual(NeonColor.fromDye(DyeColor.MAGENTA), NeonColor.PURPLE, "magenta dye");
        helper.assertTrue(NeonColor.fromDye(DyeColor.BROWN) == null, "brown has no neon");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guideBookIsGivenOnce(GameTestHelper helper) {
        var deliveries = GuideDeliveries.get(helper.getLevel().getServer());
        UUID player = UUID.randomUUID();
        helper.assertTrue(deliveries.markGiven(player), "first join gets the book");
        helper.assertFalse(deliveries.markGiven(player), "the second join does not");
        helper.succeed();
    }

    private static int dropped(GameTestHelper helper, Item item) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    /** Lets every block in the box join its neighbours, as if each had been placed after the others. */
    private static void settle(GameTestHelper helper, BlockPos from, BlockPos to) {
        for (BlockPos relative : BlockPos.betweenClosed(from, to)) {
            BlockPos pos = helper.absolutePos(relative);
            BlockState state = helper.getLevel().getBlockState(pos);
            helper.getLevel().setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), pos), 3);
        }
    }
}
