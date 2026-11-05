package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Block tags for builders and other mods: {@code boltcraft_casino:decor/<group>} and all of them in one. */
public final class DecorTags {
    public static final List<String> GROUPS = List.of("neon", "lights", "carpets", "floors", "walls", "furniture",
            "architecture");
    public static final TagKey<Block> ALL = TagKey.create(Registries.BLOCK, CasinoMod.id("decor"));

    private DecorTags() {
    }

    public static TagKey<Block> group(String name) {
        return TagKey.create(Registries.BLOCK, CasinoMod.id("decor/" + name));
    }
}
