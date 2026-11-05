package com.mbolt.casino.block.decor;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/** The colors neon tubes come in. They tint white tubes drawn in art/neon.py, so the RGB values match it. */
public enum NeonColor implements StringRepresentable {
    PINK("pink", 0xFF3EA5),
    CYAN("cyan", 0x2EE6FF),
    YELLOW("yellow", 0xFFE14D),
    GREEN("green", 0x4DFF88),
    RED("red", 0xFF3B3B),
    PURPLE("purple", 0xB45CFF),
    WHITE("white", 0xF4F4FF);

    private final String name;
    private final int rgb;

    NeonColor(String name, int rgb) {
        this.name = name;
        this.rgb = rgb;
    }

    public int rgb() {
        return rgb;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The neon a dye turns a tube into, or null for dyes with no matching neon. */
    @Nullable
    public static NeonColor fromDye(DyeColor dye) {
        return switch (dye) {
            case PINK -> PINK;
            case LIGHT_BLUE, CYAN -> CYAN;
            case YELLOW -> YELLOW;
            case LIME, GREEN -> GREEN;
            case RED -> RED;
            case PURPLE, MAGENTA -> PURPLE;
            case WHITE -> WHITE;
            default -> null;
        };
    }
}
