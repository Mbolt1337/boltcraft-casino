package com.mbolt.casino.block.decor;

import net.minecraft.util.StringRepresentable;

/**
 * How a bar counter meets its neighbours. Left and right are the counter-clockwise and clockwise sides of its
 * facing: an outer corner has customers on the front and that side too, an inner corner a notch between them.
 */
public enum CounterShape implements StringRepresentable {
    STRAIGHT("straight"),
    OUTER_LEFT("outer_left"),
    OUTER_RIGHT("outer_right"),
    INNER_LEFT("inner_left"),
    INNER_RIGHT("inner_right");

    private final String name;

    CounterShape(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
