package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.advancement.CasinoTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> REGISTER =
            DeferredRegister.create(Registries.TRIGGER_TYPE, CasinoMod.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, CasinoTrigger> CASINO =
            REGISTER.register("casino", CasinoTrigger::new);

    private ModTriggers() {
    }
}
