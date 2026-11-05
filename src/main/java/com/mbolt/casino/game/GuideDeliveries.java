package com.mbolt.casino.game;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Remembers, per world, which players already got the guide book, so everyone gets it exactly once on their
 * first join. A lost book is crafted again from a book and a gold nugget.
 */
public final class GuideDeliveries extends SavedData {
    private static final Factory<GuideDeliveries> FACTORY = new Factory<>(GuideDeliveries::new,
            GuideDeliveries::load);
    private static final String PLAYERS = "Players";

    private final Set<UUID> given = new HashSet<>();

    public static GuideDeliveries get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, CasinoMod.MODID + "_guide_given");
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && CasinoServerConfig.GIVE_GUIDE_ON_FIRST_JOIN.get()
                && get(player.server).markGiven(player.getUUID())) {
            ItemStack book = new ItemStack(ModItems.GUIDE_BOOK.get());
            if (!player.getInventory().add(book)) {
                player.drop(book, false);
            }
        }
    }

    /** True the first time a player is marked, false ever after. */
    public boolean markGiven(UUID player) {
        boolean first = given.add(player);
        if (first) {
            setDirty();
        }
        return first;
    }

    private static GuideDeliveries load(CompoundTag tag, HolderLookup.Provider registries) {
        GuideDeliveries deliveries = new GuideDeliveries();
        for (Tag entry : tag.getList(PLAYERS, Tag.TAG_INT_ARRAY)) {
            deliveries.given.add(NbtUtils.loadUUID(entry));
        }
        return deliveries;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        for (UUID player : given) {
            players.add(NbtUtils.createUUID(player));
        }
        tag.put(PLAYERS, players);
        return tag;
    }
}
