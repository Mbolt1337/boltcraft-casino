package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Modpacks decide which food and drinks can be served on a tray. */
public final class HospitalityTags {
    public static final TagKey<Item> BAR_MENU = TagKey.create(Registries.ITEM, CasinoMod.id("bar_menu"));

    private HospitalityTags() {
    }
}
