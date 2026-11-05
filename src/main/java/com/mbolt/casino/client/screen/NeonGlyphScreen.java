package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.decor.Neon;
import com.mbolt.casino.block.decor.NeonGlyph;
import com.mbolt.casino.block.decor.NeonLetterBlock;
import com.mbolt.casino.network.NeonGlyphChoice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Picks the character of a neon letter: every glyph in a grid, lit in the letter's color. Typing a character on
 * the keyboard picks it too, so a sign is spelled letter by letter without the mouse.
 */
public final class NeonGlyphScreen extends Screen {
    private static final int COLUMNS = 12;
    private static final int CELL = 20;
    private static final int GLYPH = 16;
    private static final int MARGIN = 10;
    private static final int TITLE = 14;
    private static final double REACH_SQUARED = 8 * 8;
    /** Cyrillic capitals drawn with the Latin glyph of the same shape. */
    private static final Map<Character, Character> LOOKALIKES = Map.ofEntries(
            Map.entry('А', 'A'), Map.entry('В', 'B'), Map.entry('Е', 'E'), Map.entry('К', 'K'),
            Map.entry('М', 'M'), Map.entry('Н', 'H'), Map.entry('О', 'O'), Map.entry('Р', 'P'),
            Map.entry('С', 'C'), Map.entry('Т', 'T'), Map.entry('Х', 'X'));

    private final BlockPos pos;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public NeonGlyphScreen(BlockPos pos) {
        super(Component.translatable("gui." + CasinoMod.MODID + ".neon_picker"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        int rows = (NeonGlyph.values().length + COLUMNS - 1) / COLUMNS;
        panelWidth = COLUMNS * CELL + 2 * MARGIN;
        panelHeight = rows * CELL + 2 * MARGIN + TITLE;
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
    }

    @Override
    public void tick() {
        var player = Minecraft.getInstance().player;
        if (player == null || letter() == null || player.distanceToSqr(pos.getCenter()) > REACH_SQUARED) {
            onClose();
        }
    }

    @Nullable
    private BlockState letter() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof NeonLetterBlock ? state : null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        BlockState state = letter();
        if (state == null) {
            return;
        }
        graphics.blitSprite(CasinoGui.PANEL, left, top, panelWidth, panelHeight);
        graphics.drawCenteredString(font, title, left + panelWidth / 2, top + MARGIN - 2, CasinoGui.GOLD);
        int rgb = state.getValue(Neon.COLOR).rgb();
        int current = state.getValue(NeonLetterBlock.GLYPH);
        int hovered = cellAt(mouseX, mouseY);
        NeonGlyph[] glyphs = NeonGlyph.values();
        for (int i = 0; i < glyphs.length; i++) {
            int x = cellX(i);
            int y = cellY(i);
            ResourceLocation background = i == current ? CasinoGui.ROW_SELECTED
                    : i == hovered ? CasinoGui.ROW_HOVERED : CasinoGui.INSET;
            graphics.blitSprite(background, x, y, CELL - 2, CELL - 2);
            graphics.setColor((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, 1);
            graphics.blit(CasinoMod.id("textures/block/neon/" + glyphs[i].id() + ".png"),
                    x + (CELL - 2 - GLYPH) / 2, y + (CELL - 2 - GLYPH) / 2, 0, 0, GLYPH, GLYPH, GLYPH, GLYPH);
            graphics.setColor(1, 1, 1, 1);
        }
    }

    private int cellX(int index) {
        return left + MARGIN + index % COLUMNS * CELL;
    }

    private int cellY(int index) {
        return top + MARGIN + TITLE + index / COLUMNS * CELL;
    }

    private int cellAt(double mouseX, double mouseY) {
        for (int i = 0; i < NeonGlyph.values().length; i++) {
            if (mouseX >= cellX(i) && mouseX < cellX(i) + CELL - 2 && mouseY >= cellY(i)
                    && mouseY < cellY(i) + CELL - 2) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            choose(cell);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char typed, int modifiers) {
        char wanted = Character.toUpperCase(typed);
        wanted = LOOKALIKES.getOrDefault(wanted, wanted);
        for (NeonGlyph glyph : NeonGlyph.values()) {
            if (glyph.character() == wanted
                    || String.valueOf(glyph.character()).toUpperCase(Locale.ROOT).charAt(0) == wanted) {
                choose(glyph.ordinal());
                return true;
            }
        }
        return super.charTyped(typed, modifiers);
    }

    private void choose(int glyph) {
        PacketDistributor.sendToServer(new NeonGlyphChoice(pos, glyph));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
