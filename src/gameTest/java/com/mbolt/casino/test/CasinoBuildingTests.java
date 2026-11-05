package com.mbolt.casino.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.CasinoBlock;
import com.mbolt.casino.block.decor.BarCounterBlock;
import com.mbolt.casino.block.decor.Seat;
import com.mbolt.casino.block.decor.ServingTrayBlock;
import com.mbolt.casino.block.decor.TableLampBlock;
import com.mbolt.casino.block.decor.TabletopSupport;
import com.mbolt.casino.block.entity.OwnedBlockEntity;
import com.mbolt.casino.entity.SeatEntity;
import com.mbolt.casino.registry.ModDecor;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@GameTestHolder(CasinoMod.MODID)
@PrefixGameTestTemplate(false)
public final class CasinoBuildingTests {
    private CasinoBuildingTests() {
    }

    @GameTest(template = "grand_hall", timeoutTicks = 200)
    public static void grandHallRoutesNorth(GameTestHelper helper) {
        hall(helper);
    }

    @GameTest(template = "grand_hall", rotationSteps = 1, timeoutTicks = 200)
    public static void grandHallRoutesEast(GameTestHelper helper) {
        hall(helper);
    }

    @GameTest(template = "grand_hall", rotationSteps = 2, timeoutTicks = 200)
    public static void grandHallRoutesSouth(GameTestHelper helper) {
        hall(helper);
    }

    @GameTest(template = "grand_hall", rotationSteps = 3, timeoutTicks = 200)
    public static void grandHallRoutesWest(GameTestHelper helper) {
        hall(helper);
    }

    @GameTest(template = "neon_lounge", timeoutTicks = 200)
    public static void loungeRoutesNorth(GameTestHelper helper) {
        lounge(helper);
    }

    @GameTest(template = "neon_lounge", rotationSteps = 1, timeoutTicks = 200)
    public static void loungeRoutesEast(GameTestHelper helper) {
        lounge(helper);
    }

    @GameTest(template = "neon_lounge", rotationSteps = 2, timeoutTicks = 200)
    public static void loungeRoutesSouth(GameTestHelper helper) {
        lounge(helper);
    }

    @GameTest(template = "neon_lounge", rotationSteps = 3, timeoutTicks = 200)
    public static void loungeRoutesWest(GameTestHelper helper) {
        lounge(helper);
    }

    private static void hall(GameTestHelper helper) {
        verify(helper, "grand_hall");
    }

    private static void lounge(GameTestHelper helper) {
        verify(helper, "neon_lounge");
    }

    private static void verify(GameTestHelper helper, String name) {
        JsonObject plan;
        var planFile = new File(System.getProperty("boltcraft.project_dir"), "art/structures/" + name + ".json");
        try (var reader = Files.newBufferedReader(planFile.toPath())) {
            plan = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        templateMatchesPlan(helper, plan);
        JsonObject markers = plan.getAsJsonObject("markers");
        BlockPos foyer = point(markers.getAsJsonArray("foyer"));
        route(helper, point(markers.getAsJsonArray("entrance")), foyer, "entrance to foyer");
        for (String target : new String[]{"cashier", "roulette", "bar", "bartender"}) {
            route(helper, foyer, point(markers.getAsJsonArray(target)), target);
        }
        BlockPos vault = point(markers.getAsJsonArray("vault"));
        if (markers.has("upper")) {
            BlockPos upper = point(markers.getAsJsonArray("upper"));
            Path stairs = route(helper, foyer, upper, "staircase to VIP");
            JsonArray volume = plan.getAsJsonArray("stair_volume");
            BlockPos min = new BlockPos(volume.get(0).getAsInt(), volume.get(1).getAsInt(), volume.get(2).getAsInt());
            BlockPos max = new BlockPos(volume.get(3).getAsInt(), volume.get(4).getAsInt(), volume.get(5).getAsInt());
            boolean usedStairs = false;
            for (int i = 0; i < stairs.getNodeCount(); i++) {
                var node = stairs.getNode(i);
                BlockPos actual = new BlockPos(node.x, node.y, node.z);
                for (BlockPos step : BlockPos.betweenClosed(min, max)) {
                    usedStairs |= helper.absolutePos(step.above()).equals(actual);
                }
            }
            helper.assertTrue(usedStairs, "VIP route uses the physical staircase");
            for (String target : new String[]{"vault", "balcony", "gallery", "stair_landing"}) {
                route(helper, upper, point(markers.getAsJsonArray(target)), target);
            }
        } else {
            route(helper, foyer, vault, "staff door and vault");
        }
        JsonArray size = plan.getAsJsonArray("size");
        props(helper, size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt());
        helper.succeed();
    }

    private static void templateMatchesPlan(GameTestHelper helper, JsonObject plan) {
        Map<String, BlockState> states = new HashMap<>();
        for (var value : plan.getAsJsonArray("blocks")) {
            JsonArray block = value.getAsJsonArray();
            String serialized = block.get(3).getAsString();
            BlockState expected = states.computeIfAbsent(serialized, state -> {
                try {
                    return BlockStateParser.parseForBlock(helper.getLevel().holderLookup(Registries.BLOCK),
                            state, false).blockState();
                } catch (CommandSyntaxException error) {
                    throw new IllegalArgumentException("Invalid planned block: " + state, error);
                }
            });
            if (expected.isAir()) {
                continue;
            }
            BlockPos relative = new BlockPos(block.get(0).getAsInt(), block.get(1).getAsInt(),
                    block.get(2).getAsInt()).above();
            helper.assertTrue(helper.getBlockState(relative).is(expected.getBlock()),
                    "Stale or incomplete NBT at " + relative + "; plan expects " + serialized);
            if (helper.getLevel().getBlockEntity(helper.absolutePos(relative)) instanceof OwnedBlockEntity owned) {
                helper.assertTrue(owned.isFixture(), "Generated casino game is missing fixture protection");
            }
        }
    }

    private static BlockPos point(JsonArray values) {
        return new BlockPos(values.get(0).getAsInt(), values.get(1).getAsInt(), values.get(2).getAsInt());
    }

    private static Path route(GameTestHelper helper, BlockPos start, BlockPos goal, String label) {
        var walker = EntityType.VILLAGER.create(helper.getLevel());
        BlockPos from = helper.absolutePos(start.above());
        walker.moveTo(from.getX() + .5, from.getY(), from.getZ() + .5, 0, 0);
        walker.setOnGround(true);
        walker.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(128);
        walker.getNavigation().getNodeEvaluator().setCanOpenDoors(true);
        walker.getNavigation().getNodeEvaluator().setCanPassDoors(true);
        walker.getNavigation().setMaxVisitedNodesMultiplier(10);
        Path path = walker.getNavigation().createPath(helper.absolutePos(goal.above()), 0);
        if (path == null || !path.canReach()) {
            CasinoMod.LOGGER.error("Building route {}: from {} to {}, stopped at {}", label, from,
                    helper.absolutePos(goal.above()), path == null ? null : path.getEndNode());
        }
        helper.assertTrue(path != null && path.canReach(), "Minecraft navigation cannot reach " + label);
        return path;
    }

    private static void props(GameTestHelper helper, int width, int height, int depth) {
        var rider = helper.makeMockPlayer(GameType.SURVIVAL);
        for (BlockPos relative : BlockPos.betweenClosed(0, 0, 0, width - 1, height - 1, depth - 1)) {
            var state = helper.getBlockState(relative.above());
            if (state.getBlock() instanceof TableLampBlock || state.getBlock() instanceof ServingTrayBlock) {
                int lowering = TabletopSupport.lowering(helper.getLevel(), helper.absolutePos(relative.above()));
                helper.assertTrue(lowering >= 0, "a prop is missing its supporting surface");
                helper.assertValueEqual(state.getValue(TabletopSupport.LOWERING), lowering,
                        "a prop floats over its supporting surface");
            }
            if (state.getBlock() instanceof Seat) {
                BlockPos pos = helper.absolutePos(relative.above());
                Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                        ? state.getValue(HorizontalDirectionalBlock.FACING) : intendedFacing(helper, pos);
                rider.setPos(pos.getCenter());
                rider.setYRot(facing.toYRot());
                SeatEntity.sit(helper.getLevel(), pos, state, rider);
                helper.assertTrue(rider.isPassenger(), "unusable furniture at " + relative + ": " + state);
                rider.stopRiding();
            }
        }
    }

    private static Direction intendedFacing(GameTestHelper helper, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var target = helper.getLevel().getBlockState(pos.relative(direction, 2));
            if (target.getBlock() instanceof CasinoBlock || target.getBlock() instanceof BarCounterBlock
                    || target.is(ModDecor.CARD_TABLE.get())) {
                return direction;
            }
        }
        return Direction.NORTH;
    }
}
