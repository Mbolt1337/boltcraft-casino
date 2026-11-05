package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.HorizontalShapes;
import com.mbolt.casino.block.decor.BarCounterBlock;
import com.mbolt.casino.block.decor.BarStoolBlock;
import com.mbolt.casino.block.decor.BoardBlock;
import com.mbolt.casino.block.decor.DiningChairBlock;
import com.mbolt.casino.block.decor.FacingDecorBlock;
import com.mbolt.casino.block.decor.GildedLanternBlock;
import com.mbolt.casino.block.decor.GildedPanelBlock;
import com.mbolt.casino.block.decor.GildedRailingBlock;
import com.mbolt.casino.block.decor.MarqueeLightsBlock;
import com.mbolt.casino.block.decor.NeonLetterBlock;
import com.mbolt.casino.block.decor.NeonPanelBlock;
import com.mbolt.casino.block.decor.NeonTubeBlock;
import com.mbolt.casino.block.decor.ServingTrayBlock;
import com.mbolt.casino.block.decor.ShapedDecorBlock;
import com.mbolt.casino.block.decor.StackingBlock;
import com.mbolt.casino.block.decor.TableLampBlock;
import com.mbolt.casino.block.decor.VelvetRopeBlock;
import com.mbolt.casino.block.decor.VelvetSofaBlock;
import com.mbolt.casino.block.decor.WallDecorBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * The decor catalogue (PROMPT.md 4.1): signs and atmosphere, then the interior. Everything here is cheap to craft,
 * tagged {@code boltcraft_casino:decor/*} and listed in the decor creative tab in this order.
 */
public final class ModDecor {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CasinoMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CasinoMod.MODID);

    private static final List<DeferredBlock<? extends Block>> ALL = new ArrayList<>();

    /** Neon and bulbs glow at full brightness and light the room like a lantern. */
    private static final int GLOW = 15;

    public static final List<String> NEON_SIGNS = List.of("chip", "seven", "star", "cards", "dice", "crown",
            "cocktail");
    public static final List<String> CARPETS = List.of("burgundy", "navy", "felt", "diamond");
    public static final List<String> ROPE_COLORS = List.of("red", "blue", "green", "black");

    // Wave 1: signs and atmosphere.
    public static final DeferredBlock<NeonLetterBlock> NEON_LETTER = block("neon_letter",
            () -> new NeonLetterBlock(neon()));
    public static final DeferredBlock<NeonTubeBlock> NEON_TUBE = block("neon_tube", () -> new NeonTubeBlock(neon()));
    public static final List<DeferredBlock<WallDecorBlock>> NEON_SIGN_BLOCKS = NEON_SIGNS.stream()
            .map(name -> block("neon_sign_" + name, () -> new WallDecorBlock(neon(), WallDecorBlock.flat(2))))
            .toList();
    public static final DeferredBlock<NeonPanelBlock> NEON_PANEL_BOLTCRAFT = panel("boltcraft", 5);
    public static final DeferredBlock<NeonPanelBlock> NEON_PANEL_JACKPOT = panel("jackpot", 4);
    public static final DeferredBlock<NeonPanelBlock> NEON_PANEL_BAR = panel("bar", 2);
    public static final DeferredBlock<NeonPanelBlock> NEON_PANEL_VIP = panel("vip", 2);
    public static final DeferredBlock<MarqueeLightsBlock> MARQUEE_LIGHTS = block("marquee_lights",
            () -> new MarqueeLightsBlock(props(MapColor.GOLD, SoundType.LANTERN, 0.5f).lightLevel(state -> GLOW)
                    .noOcclusion()));
    public static final List<DeferredBlock<CarpetBlock>> CARPET_BLOCKS = CARPETS.stream()
            .map(name -> block("casino_carpet_" + name, () -> new CarpetBlock(props(carpetColor(name),
                    SoundType.WOOL, 0.1f))))
            .toList();
    public static final List<DeferredBlock<Block>> CARPET_FULL_BLOCKS = CARPETS.stream()
            .map(name -> block("casino_carpet_" + name + "_block",
                    () -> new Block(props(carpetColor(name), SoundType.WOOL, 0.8f))))
            .toList();
    public static final List<DeferredBlock<VelvetRopeBlock>> VELVET_ROPES = ROPE_COLORS.stream()
            .map(color -> block("velvet_rope_" + color,
                    () -> new VelvetRopeBlock(props(MapColor.GOLD, SoundType.METAL, 1.0f).noOcclusion())))
            .toList();
    public static final DeferredBlock<BarCounterBlock> BAR_COUNTER = block("bar_counter",
            () -> new BarCounterBlock(props(MapColor.COLOR_BROWN, SoundType.WOOD, 2.5f).noOcclusion()));
    public static final DeferredBlock<BarStoolBlock> BAR_STOOL = block("bar_stool",
            () -> new BarStoolBlock(props(MapColor.COLOR_RED, SoundType.WOOL, 1.0f).noOcclusion()));

    // Wave 2: the interior.
    public static final DeferredBlock<Block> MARBLE_TILES = block("marble_tiles",
            () -> new Block(stone(MapColor.QUARTZ)));
    public static final DeferredBlock<StairBlock> MARBLE_TILE_STAIRS = block("marble_tile_stairs",
            () -> new StairBlock(MARBLE_TILES.get().defaultBlockState(), stone(MapColor.QUARTZ)));
    public static final DeferredBlock<SlabBlock> MARBLE_TILE_SLAB = block("marble_tile_slab",
            () -> new SlabBlock(stone(MapColor.QUARTZ)));
    public static final DeferredBlock<Block> CHECKERED_TILES = block("checkered_tiles",
            () -> new Block(stone(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<StairBlock> CHECKERED_TILE_STAIRS = block("checkered_tile_stairs",
            () -> new StairBlock(CHECKERED_TILES.get().defaultBlockState(), stone(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<SlabBlock> CHECKERED_TILE_SLAB = block("checkered_tile_slab",
            () -> new SlabBlock(stone(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<Block> VELVET_WALL_PANEL = block("velvet_wall_panel",
            () -> new Block(props(MapColor.COLOR_RED, SoundType.WOOL, 0.8f)));
    public static final DeferredBlock<Block> WOOD_WALL_PANEL = block("wood_wall_panel",
            () -> new Block(props(MapColor.COLOR_BROWN, SoundType.WOOD, 2.0f)));
    public static final DeferredBlock<WallDecorBlock> GOLD_SKIRTING = block("gold_skirting",
            () -> new WallDecorBlock(props(MapColor.COLOR_BROWN, SoundType.WOOD, 1.0f).noOcclusion(),
                    new HorizontalShapes(new double[]{0, 0, 13, 16, 5, 16})));
    public static final DeferredBlock<StackingBlock> CASINO_COLUMN = block("casino_column",
            () -> new StackingBlock(stone(MapColor.QUARTZ).noOcclusion(),
                    new HorizontalShapes(new double[]{1, 0, 1, 15, 16, 15}), false));
    public static final DeferredBlock<ShapedDecorBlock> CRYSTAL_CHANDELIER = block("crystal_chandelier",
            () -> new ShapedDecorBlock(light(SoundType.GLASS), Block.box(3, 1, 3, 13, 16, 13)));
    public static final DeferredBlock<ShapedDecorBlock> GRAND_CHANDELIER = block("grand_chandelier",
            () -> new ShapedDecorBlock(light(SoundType.GLASS), Block.box(5, 0, 5, 11, 16, 11)));
    public static final DeferredBlock<WallDecorBlock> WALL_SCONCE = block("wall_sconce",
            () -> new WallDecorBlock(light(SoundType.LANTERN).lightLevel(state -> 14),
                    new HorizontalShapes(new double[]{5, 3, 8, 11, 15, 16})));
    public static final DeferredBlock<VelvetSofaBlock> VELVET_SOFA = block("velvet_sofa",
            () -> new VelvetSofaBlock(props(MapColor.COLOR_RED, SoundType.WOOL, 0.8f).noOcclusion()));
    public static final DeferredBlock<ShapedDecorBlock> COFFEE_TABLE = block("coffee_table",
            () -> new ShapedDecorBlock(props(MapColor.COLOR_BROWN, SoundType.WOOD, 1.5f).noOcclusion(),
                    Block.box(1, 0, 1, 15, 8, 15)));
    public static final DeferredBlock<FacingDecorBlock> CARD_TABLE = block("card_table",
            () -> new FacingDecorBlock(props(MapColor.COLOR_GREEN, SoundType.WOOD, 2.0f).noOcclusion(),
                    new HorizontalShapes(new double[]{0, 0, 0, 16, 13, 16})));
    public static final DeferredBlock<StackingBlock> VELVET_CURTAIN = block("velvet_curtain",
            () -> new StackingBlock(props(MapColor.COLOR_RED, SoundType.WOOL, 0.5f).noOcclusion().noCollission(),
                    new HorizontalShapes(new double[]{0, 0, 11, 16, 16, 16}), true));
    public static final DeferredBlock<ShapedDecorBlock> GOLD_PLANTER = block("gold_planter",
            () -> new ShapedDecorBlock(props(MapColor.GOLD, SoundType.METAL, 1.0f).noOcclusion(),
                    Block.box(2, 0, 2, 14, 9, 14)));

    // Hospitality: the approved bar and lounge expansion.
    public static final DeferredBlock<DiningChairBlock> DINING_CHAIR = block("dining_chair",
            () -> new DiningChairBlock(props(MapColor.COLOR_RED, SoundType.WOOD, 1.5f).noOcclusion()));
    public static final DeferredBlock<ServingTrayBlock> SERVING_TRAY = block("serving_tray",
            () -> new ServingTrayBlock(props(MapColor.GOLD, SoundType.WOOD, .8f).noOcclusion()));
    public static final DeferredBlock<TableLampBlock> TABLE_LAMP = block("table_lamp",
            () -> new TableLampBlock(light(SoundType.LANTERN).lightLevel(state -> 12)));
    public static final DeferredBlock<WallDecorBlock> BOTTLE_SHELF = block("bottle_shelf",
            () -> new WallDecorBlock(props(MapColor.COLOR_BROWN, SoundType.WOOD, 1.5f).noOcclusion(),
                    WallDecorBlock.flat(7)));

    // Gilded stone cladding: architectural trim, never a source of recoverable gold.
    public static final DeferredBlock<Block> GILDED_TRIM = block("gilded_trim",
            () -> new Block(stone(MapColor.GOLD)));

    // Architecture: the facades and halls of the casino buildings (concepts of 25 October 2026).
    public static final DeferredBlock<BoardBlock> CASINO_MARQUEE = block("casino_marquee",
            () -> new BoardBlock(neon(), 13, 4, 4));
    public static final DeferredBlock<BoardBlock> CASINO_MARQUEE_SMALL = block("casino_marquee_small",
            () -> new BoardBlock(neon(), 9, 3, 4));
    public static final List<String> CHIP_COLORS = List.of("red", "blue", "black");
    public static final List<DeferredBlock<BoardBlock>> GIANT_CHIPS = CHIP_COLORS.stream()
            .map(color -> block("giant_chip_" + color,
                    () -> new BoardBlock(props(MapColor.COLOR_RED, SoundType.STONE, 1.0f).noOcclusion(), 3, 3, 2)))
            .toList();
    public static final DeferredBlock<BoardBlock> BOLTCRAFT_BANNER = block("boltcraft_banner",
            () -> new BoardBlock(props(MapColor.COLOR_RED, SoundType.WOOL, 0.8f).noOcclusion()
                    .lightLevel(state -> 10), 2, 4, 2));
    public static final DeferredBlock<GildedPanelBlock> GILDED_PANEL = block("gilded_panel",
            () -> new GildedPanelBlock(stone(MapColor.COLOR_RED)));
    public static final DeferredBlock<Block> GILDED_CORNICE = block("gilded_cornice",
            () -> new Block(stone(MapColor.COLOR_BLACK)));
    public static final DeferredBlock<Block> COFFERED_CEILING = block("coffered_ceiling",
            () -> new Block(props(MapColor.COLOR_BROWN, SoundType.WOOD, 2.0f)));
    public static final DeferredBlock<StairBlock> CARPET_STAIRS = block("carpet_stairs",
            () -> new StairBlock(Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                    stone(MapColor.COLOR_RED).sound(SoundType.WOOL)));
    public static final DeferredBlock<GildedRailingBlock> GILDED_RAILING = block("gilded_railing",
            () -> new GildedRailingBlock(props(MapColor.GOLD, SoundType.METAL, 2.0f).noOcclusion()));
    public static final DeferredBlock<GildedLanternBlock> GILDED_LANTERN = block("gilded_lantern",
            () -> new GildedLanternBlock(light(SoundType.LANTERN)));
    public static final DeferredBlock<StackingBlock> ONYX_COLUMN = block("onyx_column",
            () -> new StackingBlock(stone(MapColor.COLOR_BLACK).noOcclusion(),
                    new HorizontalShapes(new double[]{1, 0, 1, 15, 16, 15}), false));

    private ModDecor() {
    }

    public static List<DeferredBlock<? extends Block>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static <B extends Block> DeferredBlock<B> block(String name, Supplier<B> factory) {
        DeferredBlock<B> block = BLOCKS.register(name, factory);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        ALL.add(block);
        return block;
    }

    private static DeferredBlock<NeonPanelBlock> panel(String name, int width) {
        return block("neon_panel_" + name, () -> new NeonPanelBlock(neon(), width));
    }

    private static BlockBehaviour.Properties props(MapColor color, SoundType sound, float strength) {
        return BlockBehaviour.Properties.of().mapColor(color).sound(sound).strength(strength);
    }

    private static BlockBehaviour.Properties neon() {
        return props(MapColor.NONE, SoundType.GLASS, 0.3f).lightLevel(state -> GLOW).noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties light(SoundType sound) {
        return props(MapColor.GOLD, sound, 0.5f).lightLevel(state -> GLOW).noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties stone(MapColor color) {
        return props(color, SoundType.STONE, 1.5f).explosionResistance(6).requiresCorrectToolForDrops();
    }

    private static MapColor carpetColor(String pattern) {
        return switch (pattern) {
            case "navy" -> MapColor.COLOR_BLUE;
            case "felt" -> MapColor.COLOR_GREEN;
            case "diamond" -> MapColor.COLOR_BLACK;
            default -> MapColor.COLOR_RED;
        };
    }

    /** The decor's block item, for recipes and the creative tab. */
    public static Item item(DeferredBlock<? extends Block> block) {
        return block.get().asItem();
    }
}
