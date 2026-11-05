package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.entity.VaultLink;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CasinoMod.MODID);

    /** The vault a manager key remembers; casino blocks clicked next are linked to it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<VaultLink>> LINKED_VAULT =
            REGISTER.registerComponentType("linked_vault",
                    builder -> builder.persistent(VaultLink.CODEC).networkSynchronized(VaultLink.STREAM_CODEC));

    private ModDataComponents() {
    }
}
