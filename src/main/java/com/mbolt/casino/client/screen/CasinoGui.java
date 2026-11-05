package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoMod;
import net.minecraft.resources.ResourceLocation;

/** Sprite names and colors shared by every casino screen. Sprites live in {@code textures/gui/sprites/}. */
final class CasinoGui {
    /** Buttoned velvet in a gold frame, nine-sliced to any screen size. */
    static final ResourceLocation PANEL = CasinoMod.id("panel");
    /** Dark steel recess for lists and fields. */
    static final ResourceLocation INSET = CasinoMod.id("inset");
    static final ResourceLocation ROW_SELECTED = CasinoMod.id("row_selected");
    static final ResourceLocation ROW_HOVERED = CasinoMod.id("row_hovered");
    /** Gold rule between parts of a screen, two pixels high. */
    static final ResourceLocation DIVIDER = CasinoMod.id("divider");
    static final ResourceLocation CHIP_ICON = CasinoMod.id("chip_icon");
    static final int CHIP_ICON_SIZE = 8;

    static final int GOLD = 0xFFF2D27A;
    static final int CREAM = 0xFFE8E2C8;
    static final int MUTED = 0xFFB8A29B;
    static final int SUCCESS = 0xFF7FE8A8;
    static final int ERROR = 0xFFF2A08C;

    private CasinoGui() {
    }
}
