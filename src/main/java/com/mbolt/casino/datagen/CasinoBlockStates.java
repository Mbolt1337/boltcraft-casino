package com.mbolt.casino.datagen;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.CasinoBlock;
import com.mbolt.casino.block.decor.BarCounterBlock;
import com.mbolt.casino.block.decor.CounterShape;
import com.mbolt.casino.block.decor.Neon;
import com.mbolt.casino.block.decor.NeonGlyph;
import com.mbolt.casino.block.decor.NeonLetterBlock;
import com.mbolt.casino.block.decor.NeonPanelBlock;
import com.mbolt.casino.block.decor.StackingBlock;
import com.mbolt.casino.block.decor.TabletopSupport;
import com.mbolt.casino.block.decor.VelvetSofaBlock;
import com.mbolt.casino.block.decor.WallConnectingBlock;
import com.mbolt.casino.registry.ModBlocks;
import com.mbolt.casino.registry.ModDecor;
import com.mbolt.casino.block.decor.BoardBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Block states. GeckoLib blocks get a model with only a particle texture, because their block entity renderer
 * draws them; the cashier is a normal baked model.
 */
final class CasinoBlockStates extends BlockStateProvider {
    CasinoBlockStates(PackOutput output, ExistingFileHelper files) {
        super(output, CasinoMod.MODID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        horizontalBlock(ModBlocks.CASHIER.get(), models().getExistingFile(modLoc("block/cashier")));
        horizontalBlock(ModBlocks.VAULT.get(), particleOnly("vault", "block/vault_particle"));

        ModelFile slotMachine = particleOnly("slot_machine", "block/slot_machine_particle");
        getVariantBuilder(ModBlocks.SLOT_MACHINE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(slotMachine)
                .rotationY(((int) state.getValue(CasinoBlock.FACING).toYRot() + 180) % 360)
                .build());
        ModelFile roulette = particleOnly("roulette_table", "block/roulette_table_particle");
        getVariantBuilder(ModBlocks.ROULETTE_TABLE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(roulette).build());
        neon();
        decor();
        horizontalBlock(ModDecor.DINING_CHAIR.get(), existing("hospitality/dining_chair"));
        horizontalBlock(ModDecor.BOTTLE_SHELF.get(), existing("hospitality/bottle_shelf"));
        tabletop(ModDecor.SERVING_TRAY.get(), "serving_tray");
        tabletop(ModDecor.TABLE_LAMP.get(), "table_lamp");
        simpleBlock(ModDecor.GILDED_TRIM.get(), existing("architecture/gilded_trim"));
        architecture();
    }

    /** Boards turn with their wall; gilded panels pick the frame each face needs from their neighbours. */
    private void architecture() {
        List<DeferredBlock<BoardBlock>> boards = new ArrayList<>(List.of(ModDecor.CASINO_MARQUEE,
                ModDecor.CASINO_MARQUEE_SMALL, ModDecor.BOLTCRAFT_BANNER));
        boards.addAll(ModDecor.GIANT_CHIPS);
        for (var holder : boards) {
            BoardBlock board = holder.get();
            String name = holder.getId().getPath();
            getVariantBuilder(board).forAllStates(state -> {
                int part = state.getValue(BoardBlock.PART);
                return ConfiguredModel.builder()
                        .modelFile(existing("board/" + name + "_" + (part < board.parts() ? part : 0)))
                        .rotationY(turn(state))
                        .build();
            });
        }
        gildedPanel();
        ResourceLocation darkStone = modLoc("block/architecture/dark_stone");
        simpleBlock(ModDecor.GILDED_CORNICE.get(), models().cubeBottomTop("gilded_cornice",
                modLoc("block/architecture/gilded_cornice"), darkStone, darkStone));
        ResourceLocation wood = modLoc("block/decor/dark_wood");
        simpleBlock(ModDecor.COFFERED_CEILING.get(), models().cube("coffered_ceiling",
                        modLoc("block/architecture/coffer"), wood, wood, wood, wood, wood)
                .texture("particle", wood));
        ResourceLocation runner = modLoc("block/architecture/carpet_runner");
        stairsBlock(ModDecor.CARPET_STAIRS.get(), runner, darkStone, runner);
        var railing = getMultipartBuilder(ModDecor.GILDED_RAILING.get());
        railing.part().modelFile(existing("architecture/gilded_railing_post")).addModel().end();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            railing.part().modelFile(existing("architecture/gilded_railing_side")).rotationY(turn(direction))
                    .addModel().condition(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), true).end();
        }
        getVariantBuilder(ModDecor.GILDED_LANTERN.get()).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(existing(state.getValue(LanternBlock.HANGING)
                        ? "architecture/gilded_lantern_hanging" : "architecture/gilded_lantern"))
                .build(), LanternBlock.WATERLOGGED);
        getVariantBuilder(ModDecor.ONYX_COLUMN.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(existing("decor/onyx_column_" + state.getValue(StackingBlock.PART).getSerializedName()))
                .build());
    }

    /**
     * One model per combination of neighbours. Each face shows the gold frame on the edges with no panel beside
     * them, left and right as a viewer facing that face sees them.
     */
    private void gildedPanel() {
        Map<String, ModelFile> made = new HashMap<>();
        getVariantBuilder(ModDecor.GILDED_PANEL.get()).forAllStates(state -> {
            boolean north = state.getValue(PipeBlock.NORTH);
            boolean east = state.getValue(PipeBlock.EAST);
            boolean south = state.getValue(PipeBlock.SOUTH);
            boolean west = state.getValue(PipeBlock.WEST);
            boolean up = state.getValue(PipeBlock.UP);
            boolean down = state.getValue(PipeBlock.DOWN);
            String name = "gilded_panel_" + bits(north, east, south, west, up, down);
            ModelFile model = made.computeIfAbsent(name, key -> models().cube(key,
                            frame(south, west, north, east), frame(north, west, south, east),
                            frame(up, east, down, west), frame(up, west, down, east),
                            frame(up, south, down, north), frame(up, north, down, south))
                    .texture("particle", modLoc("block/architecture/gilded_panel_0000")));
            return ConfiguredModel.builder().modelFile(model).build();
        });
    }

    /** The panel texture with a frame on each side whose neighbour is not a panel. */
    private ResourceLocation frame(boolean top, boolean left, boolean bottom, boolean right) {
        return modLoc("block/architecture/gilded_panel_" + bits(!top, !left, !bottom, !right));
    }

    private static String bits(boolean... values) {
        StringBuilder text = new StringBuilder();
        for (boolean value : values) {
            text.append(value ? '1' : '0');
        }
        return text.toString();
    }

    private void tabletop(Block block, String name) {
        getVariantBuilder(block).forAllStates(state -> {
            int lowering = state.getValue(TabletopSupport.LOWERING);
            String suffix = lowering == 0 ? "" : "_lowered_" + lowering;
            return ConfiguredModel.builder().modelFile(existing("hospitality/" + name + suffix)).build();
        });
    }

    /** Neon models are written by art/neon.py; the blockstates only pick and turn them. */
    private void neon() {
        NeonGlyph[] glyphs = NeonGlyph.values();
        getVariantBuilder(ModDecor.NEON_LETTER.get()).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(existing("neon/letter_" + glyphs[state.getValue(NeonLetterBlock.GLYPH)].id()))
                .rotationY(turn(state))
                .build(), Neon.COLOR);
        wallArms(ModDecor.NEON_TUBE.get(), "neon/tube_", true);
        for (int i = 0; i < ModDecor.NEON_SIGNS.size(); i++) {
            horizontalBlock(ModDecor.NEON_SIGN_BLOCKS.get(i).get(),
                    existing("neon/sign_" + ModDecor.NEON_SIGNS.get(i)));
        }
        for (var panel : List.of(ModDecor.NEON_PANEL_BOLTCRAFT, ModDecor.NEON_PANEL_JACKPOT, ModDecor.NEON_PANEL_BAR,
                ModDecor.NEON_PANEL_VIP)) {
            NeonPanelBlock block = panel.get();
            String name = panel.getId().getPath().substring("neon_".length());
            getVariantBuilder(block).forAllStates(state -> {
                int part = state.getValue(NeonPanelBlock.PART);
                return ConfiguredModel.builder()
                        .modelFile(existing("neon/" + name + "_" + (part < block.width() ? part : 0)))
                        .rotationY(turn(state))
                        .build();
            });
        }
        wallArms(ModDecor.MARQUEE_LIGHTS.get(), "decor/marquee_", false);
    }

    /** Arms of a strip on the wall, each turned with the wall it hangs on; neon tubes also have a middle piece. */
    private void wallArms(Block block, String prefix, boolean center) {
        var builder = getMultipartBuilder(block);
        Map<String, BooleanProperty> arms = Map.of("up", WallConnectingBlock.UP, "down", WallConnectingBlock.DOWN,
                "left", WallConnectingBlock.LEFT, "right", WallConnectingBlock.RIGHT);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int y = turn(facing);
            if (center) {
                builder.part().modelFile(existing(prefix + "center")).rotationY(y).addModel()
                        .condition(HorizontalDirectionalBlock.FACING, facing).end();
            }
            arms.forEach((arm, property) -> builder.part().modelFile(existing(prefix + arm)).rotationY(y).addModel()
                    .condition(HorizontalDirectionalBlock.FACING, facing).condition(property, true).end());
        }
    }

    private void decor() {
        for (int i = 0; i < ModDecor.CARPETS.size(); i++) {
            String name = "casino_carpet_" + ModDecor.CARPETS.get(i);
            ResourceLocation texture = modLoc("block/decor/carpet_" + ModDecor.CARPETS.get(i));
            simpleBlock(ModDecor.CARPET_BLOCKS.get(i).get(), models().carpet(name, texture));
            simpleBlock(ModDecor.CARPET_FULL_BLOCKS.get(i).get(), models().cubeAll(name + "_block", texture));
        }
        for (int i = 0; i < ModDecor.ROPE_COLORS.size(); i++) {
            var builder = getMultipartBuilder(ModDecor.VELVET_ROPES.get(i).get());
            builder.part().modelFile(existing("decor/velvet_rope_post")).addModel().end();
            ModelFile side = existing("decor/velvet_rope_side_" + ModDecor.ROPE_COLORS.get(i));
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                builder.part().modelFile(side).rotationY(turn(direction)).addModel()
                        .condition(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), true).end();
            }
        }
        getVariantBuilder(ModDecor.BAR_COUNTER.get()).forAllStates(state -> {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            CounterShape shape = state.getValue(BarCounterBlock.SHAPE);
            // The right-hand corners are the left-hand ones of the counter turned a quarter clockwise.
            boolean right = shape == CounterShape.OUTER_RIGHT || shape == CounterShape.INNER_RIGHT;
            String model = switch (shape) {
                case STRAIGHT -> "decor/bar_counter";
                case OUTER_LEFT, OUTER_RIGHT -> "decor/bar_counter_outer";
                case INNER_LEFT, INNER_RIGHT -> "decor/bar_counter_inner";
            };
            return ConfiguredModel.builder().modelFile(existing(model))
                    .rotationY(turn(right ? facing.getClockWise() : facing)).build();
        });
        for (var block : List.of(ModDecor.BAR_STOOL, ModDecor.COFFEE_TABLE, ModDecor.CRYSTAL_CHANDELIER,
                ModDecor.GRAND_CHANDELIER, ModDecor.GOLD_PLANTER)) {
            simpleBlock(block.get(), existing("decor/" + block.getId().getPath()));
        }
        for (var block : List.of(ModDecor.GOLD_SKIRTING, ModDecor.WALL_SCONCE)) {
            horizontalBlock(block.get(), existing("decor/" + block.getId().getPath()));
        }
        horizontalBlock(ModDecor.CARD_TABLE.get(), existing("decor/card_table"));

        tiles(ModDecor.MARBLE_TILES.get(), ModDecor.MARBLE_TILE_STAIRS.get(), ModDecor.MARBLE_TILE_SLAB.get(),
                "marble_tiles");
        tiles(ModDecor.CHECKERED_TILES.get(), ModDecor.CHECKERED_TILE_STAIRS.get(),
                ModDecor.CHECKERED_TILE_SLAB.get(), "checkered_tiles");
        simpleBlock(ModDecor.VELVET_WALL_PANEL.get(), models().cubeColumn("velvet_wall_panel",
                modLoc("block/decor/velvet_wall_panel"), modLoc("block/decor/velvet")));
        simpleBlock(ModDecor.WOOD_WALL_PANEL.get(), models().cubeColumn("wood_wall_panel",
                modLoc("block/decor/wood_wall_panel"), modLoc("block/decor/dark_wood")));

        getVariantBuilder(ModDecor.CASINO_COLUMN.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(existing("decor/column_" + state.getValue(StackingBlock.PART).getSerializedName()))
                .build());
        getVariantBuilder(ModDecor.VELVET_CURTAIN.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(existing("decor/velvet_curtain_"
                        + state.getValue(StackingBlock.PART).getSerializedName()))
                .rotationY(turn(state))
                .build());

        var sofa = getMultipartBuilder(ModDecor.VELVET_SOFA.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            sofa.part().modelFile(existing("decor/velvet_sofa")).rotationY(turn(facing)).addModel()
                    .condition(HorizontalDirectionalBlock.FACING, facing).end();
            sofa.part().modelFile(existing("decor/velvet_sofa_arm_left")).rotationY(turn(facing)).addModel()
                    .condition(HorizontalDirectionalBlock.FACING, facing).condition(VelvetSofaBlock.ARM_LEFT, true)
                    .end();
            sofa.part().modelFile(existing("decor/velvet_sofa_arm_right")).rotationY(turn(facing)).addModel()
                    .condition(HorizontalDirectionalBlock.FACING, facing).condition(VelvetSofaBlock.ARM_RIGHT, true)
                    .end();
        }
    }

    private void tiles(Block block, StairBlock stairs, SlabBlock slab, String name) {
        ResourceLocation texture = modLoc("block/decor/" + name);
        simpleBlock(block, models().cubeAll(name, texture));
        stairsBlock(stairs, texture);
        slabBlock(slab, modLoc("block/" + name), texture);
    }

    private ModelFile existing(String path) {
        return models().getExistingFile(modLoc("block/" + path));
    }

    /** Models face north; the blockstate turns them clockwise for east, south and west. */
    private static int turn(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360;
    }

    private static int turn(BlockState state) {
        return turn(state.getValue(HorizontalDirectionalBlock.FACING));
    }

    private ModelFile particleOnly(String name, String texture) {
        return models().getBuilder(name).texture("particle", modLoc(texture));
    }
}
