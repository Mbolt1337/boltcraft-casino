package com.mbolt.casino.block.decor;

import net.minecraft.util.StringRepresentable;

/** Where a block stands in a vertical run of the same block, for columns and curtains. */
public enum StackPart implements StringRepresentable {
    SINGLE("single"),
    BOTTOM("bottom"),
    MIDDLE("middle"),
    TOP("top");

    private final String name;

    StackPart(String name) {
        this.name = name;
    }

    public static StackPart of(boolean below, boolean above) {
        if (below && above) {
            return MIDDLE;
        }
        return below ? TOP : above ? BOTTOM : SINGLE;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
