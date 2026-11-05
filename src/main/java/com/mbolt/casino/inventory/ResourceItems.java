package com.mbolt.casino.inventory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Optional;

/** The ledger keys resources by item id strings; this converts them back to registered items. */
public final class ResourceItems {
    private ResourceItems() {
    }

    public static Optional<Item> item(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return Optional.empty();
        }
        Item item = BuiltInRegistries.ITEM.get(location);
        return item == Items.AIR ? Optional.empty() : Optional.of(item);
    }
}
