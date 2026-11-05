package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.decor.BarCounterBlock;
import com.mbolt.casino.block.decor.CounterShape;
import com.mbolt.casino.block.decor.NeonPanelBlock;
import com.mbolt.casino.block.decor.Seat;
import com.mbolt.casino.block.decor.VelvetSofaBlock;
import com.mbolt.casino.block.decor.WallConnectingBlock;
import com.mbolt.casino.entity.SeatEntity;
import com.mbolt.casino.registry.ModDecor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(CasinoMod.MODID)
@PrefixGameTestTemplate(false)
public final class FurnitureTests {
    private static final BlockPos CHAIR = new BlockPos(2, 1, 2);

    private FurnitureTests() {
    }

    @GameTest(template = "empty")
    public static void onePlayerPerSeat(GameTestHelper helper) {
        helper.setBlock(CHAIR.below(), Blocks.STONE);
        helper.setBlock(CHAIR, ModDecor.BAR_STOOL.get());
        var alice = rider(helper, CHAIR.north());
        var bob = rider(helper, CHAIR.south());
        var state = helper.getBlockState(CHAIR);
        helper.useBlock(CHAIR, alice);
        SeatEntity.sit(helper.getLevel(), helper.absolutePos(CHAIR), state, bob);
        helper.assertTrue(alice.getVehicle() instanceof SeatEntity, "Alice sits on the stool");
        helper.assertFalse(bob.isPassenger(), "Bob cannot occupy Alice's seat");
        helper.assertFalse(alice.getVehicle().shouldBeSaved(), "riding anchors are transient");
        alice.stopRiding();
        helper.runAfterDelay(2, () -> {
            var seats = helper.getLevel().getEntitiesOfClass(SeatEntity.class,
                    new AABB(helper.absolutePos(CHAIR)).inflate(1));
            helper.assertTrue(seats.isEmpty(), "empty anchors are removed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void blockedSeatsDoNotMount(GameTestHelper helper) {
        helper.setBlock(CHAIR, ModDecor.BAR_STOOL.get());
        helper.setBlock(CHAIR.above(), Blocks.STONE);
        var player = rider(helper, CHAIR.north());
        SeatEntity.sit(helper.getLevel(), helper.absolutePos(CHAIR), helper.getBlockState(CHAIR), player);
        helper.assertFalse(player.isPassenger(), "a ceiling blocks the seat");
        helper.setBlock(CHAIR.above(), Blocks.AIR);
        player.setShiftKeyDown(true);
        SeatEntity.sit(helper.getLevel(), helper.absolutePos(CHAIR), helper.getBlockState(CHAIR), player);
        helper.assertFalse(player.isPassenger(), "sneaking preserves normal building interaction");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void breakingFurnitureDismountsSafely(GameTestHelper helper) {
        for (BlockPos floor : BlockPos.betweenClosed(0, 0, 0, 5, 0, 5)) {
            helper.setBlock(floor, Blocks.STONE);
        }
        helper.setBlock(CHAIR, ModDecor.VELVET_SOFA.get());
        var player = rider(helper, CHAIR.south());
        SeatEntity.sit(helper.getLevel(), helper.absolutePos(CHAIR), helper.getBlockState(CHAIR), player);
        helper.assertTrue(player.isPassenger(), "the sofa starts with room for knees");
        helper.setBlock(CHAIR.north(), Blocks.STONE);
        helper.setBlock(CHAIR.north().above(), Blocks.STONE);
        helper.getLevel().destroyBlock(helper.absolutePos(CHAIR), false);
        helper.runAfterDelay(2, () -> {
            helper.assertFalse(player.isPassenger(), "breaking a sofa removes its rider");
            helper.assertTrue(helper.getLevel().noCollision(player), "the exit is outside the wall");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void logoutReleasesSeatBeforeSaving(GameTestHelper helper) {
        for (BlockPos floor : BlockPos.betweenClosed(0, 0, 0, 5, 0, 5)) {
            helper.setBlock(floor, Blocks.STONE);
        }
        helper.setBlock(CHAIR, ModDecor.VELVET_SOFA.get());
        var player = rider(helper, CHAIR.south());
        helper.useBlock(CHAIR, player);
        var seat = player.getVehicle();
        helper.assertTrue(seat instanceof SeatEntity, "player starts seated");
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
        helper.assertFalse(player.isPassenger(), "logout releases the player before vanilla saves their position");
        helper.assertTrue(seat.isRemoved(), "logout does not leave an occupied anchor");
        helper.assertTrue(helper.getLevel().noCollision(player), "the saved exit is clear of furniture");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void seatAnchorsMatchEveryFurnitureOrientation(GameTestHelper helper) {
        for (BlockPos floor : BlockPos.betweenClosed(0, 0, 0, 5, 0, 5)) {
            helper.setBlock(floor, Blocks.STONE);
        }
        var player = rider(helper, CHAIR.south());
        for (var block : List.of(ModDecor.VELVET_SOFA.get(), ModDecor.DINING_CHAIR.get(),
                ModDecor.BAR_STOOL.get())) {
            var surface = (Seat) block;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                var state = block.defaultBlockState();
                if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                    state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
                }
                helper.setBlock(CHAIR, state);
                player.setYRot(facing.toYRot());
                player.setPos(helper.absolutePos(CHAIR.relative(facing, 2)).getCenter());
                SeatEntity.sit(helper.getLevel(), helper.absolutePos(CHAIR), state, player);
                helper.assertTrue(player.getVehicle() instanceof SeatEntity, "valid seat for " + facing);
                var anchor = player.getVehicle();
                var expected = Vec3.atBottomCenterOf(helper.absolutePos(CHAIR))
                        .add(facing.getStepX() * surface.seatForwardOffset(), surface.seatHeight(),
                                facing.getStepZ() * surface.seatForwardOffset());
                helper.assertTrue(anchor.position().distanceToSqr(expected) < 1.0E-8,
                        "the hips align with the cushion and front edge");
                player.stopRiding();
                helper.assertTrue(helper.getLevel().noCollision(player), "dismount leaves the player clear");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wallInFrontCannotIntersectTheShins(GameTestHelper helper) {
        var player = rider(helper, CHAIR.south());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var state = ModDecor.DINING_CHAIR.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, facing);
            helper.setBlock(CHAIR, state);
            helper.setBlock(CHAIR.relative(facing), Blocks.STONE);
            SeatEntity.sit(helper.getLevel(), helper.absolutePos(CHAIR), state, player);
            helper.assertFalse(player.isPassenger(), "a wall cannot contain the seated player's shins");
            helper.setBlock(CHAIR.relative(facing), Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void newCeilingEjectsTheRiderSafely(GameTestHelper helper) {
        for (BlockPos floor : BlockPos.betweenClosed(0, 0, 0, 5, 0, 5)) {
            helper.setBlock(floor, Blocks.STONE);
        }
        helper.setBlock(CHAIR, ModDecor.BAR_STOOL.get());
        var player = rider(helper, CHAIR.south());
        helper.useBlock(CHAIR, player);
        helper.assertTrue(player.isPassenger(), "the stool is initially clear");
        helper.setBlock(CHAIR.above(), Blocks.STONE);
        helper.runAfterDelay(2, () -> {
            helper.assertFalse(player.isPassenger(), "a newly obstructed seat releases its rider");
            helper.assertTrue(helper.getLevel().noCollision(player), "the rider is outside the new ceiling");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void asymmetricDecorMirrorsItsConnections(GameTestHelper helper) {
        var sofa = ModDecor.VELVET_SOFA.get().defaultBlockState()
                .setValue(VelvetSofaBlock.ARM_LEFT, false).setValue(VelvetSofaBlock.ARM_RIGHT, true);
        var reflected = sofa.mirror(Mirror.FRONT_BACK);
        helper.assertTrue(reflected.getValue(VelvetSofaBlock.ARM_LEFT), "sofa end changes side");
        helper.assertFalse(reflected.getValue(VelvetSofaBlock.ARM_RIGHT), "joined side changes too");
        var bar = ModDecor.BAR_COUNTER.get().defaultBlockState()
                .setValue(BarCounterBlock.SHAPE, CounterShape.INNER_LEFT).mirror(Mirror.LEFT_RIGHT);
        helper.assertTrue(bar.getValue(BarCounterBlock.SHAPE) == CounterShape.INNER_RIGHT,
                "bar corner handedness changes");
        var tube = ModDecor.NEON_TUBE.get().defaultBlockState()
                .setValue(WallConnectingBlock.LEFT, true).setValue(WallConnectingBlock.RIGHT, false)
                .mirror(Mirror.FRONT_BACK);
        helper.assertFalse(tube.getValue(WallConnectingBlock.LEFT), "neon arms mirror");
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var panel = ModDecor.NEON_PANEL_BOLTCRAFT.get().defaultBlockState()
                    .setValue(NeonPanelBlock.FACING, facing).setValue(NeonPanelBlock.PART, 0);
            helper.assertValueEqual(panel.mirror(Mirror.LEFT_RIGHT).getValue(NeonPanelBlock.PART), 4,
                    "reflected panel preserves a valid anchor for " + facing);
        }
        helper.succeed();
    }

    /** NeoForge FakePlayer deliberately refuses riding; use Minecraft's movable mock player instead. */
    private static Player rider(GameTestHelper helper, BlockPos pos) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(pos).getCenter());
        return player;
    }
}
