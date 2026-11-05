package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.entity.CroupierEntity;
import com.mbolt.casino.entity.SeatEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(Registries.ENTITY_TYPE, CasinoMod.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<CroupierEntity>> CROUPIER =
            REGISTER.register("croupier", () -> EntityType.Builder.of(CroupierEntity::new, MobCategory.MISC)
                    .sized(.5f, 1.95f).clientTrackingRange(8).build(CasinoMod.MODID + ":croupier"));
    public static final DeferredHolder<EntityType<?>, EntityType<SeatEntity>> SEAT =
            REGISTER.register("seat", () -> EntityType.Builder.of(SeatEntity::new, MobCategory.MISC)
                    .sized(.01f, .01f).clientTrackingRange(8).updateInterval(20).noSave().noSummon()
                    .build(CasinoMod.MODID + ":seat"));

    private ModEntities() { }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(CROUPIER.get(), CroupierEntity.attributes().build());
    }
}
