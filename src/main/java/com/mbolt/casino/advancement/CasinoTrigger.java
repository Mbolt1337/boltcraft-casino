package com.mbolt.casino.advancement;

import com.mbolt.casino.registry.ModTriggers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** {@code boltcraft_casino:casino}: fires when a {@link CasinoEvent} happens to the player. */
public final class CasinoTrigger extends SimpleCriterionTrigger<CasinoTrigger.Instance> {
    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, CasinoEvent event) {
        trigger(player, instance -> instance.event() == event);
    }

    /** The criterion an advancement uses to wait for {@code event}. */
    public static Criterion<Instance> on(CasinoEvent event) {
        return ModTriggers.CASINO.get().createCriterion(new Instance(Optional.empty(), event));
    }

    public record Instance(Optional<ContextAwarePredicate> player, CasinoEvent event)
            implements SimpleCriterionTrigger.SimpleInstance {
        static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                CasinoEvent.CODEC.fieldOf("event").forGetter(Instance::event)
        ).apply(instance, Instance::new));
    }
}
