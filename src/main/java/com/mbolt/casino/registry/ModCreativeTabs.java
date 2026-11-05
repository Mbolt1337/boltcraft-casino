package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CasinoMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GAMES = REGISTER.register("games",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + CasinoMod.MODID + ".games"))
                    .icon(ModItems.CHIP_1000::toStack)
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.VAULT);
                        output.accept(ModItems.CASHIER);
                        output.accept(ModItems.SLOT_MACHINE);
                        output.accept(ModItems.ROULETTE_TABLE);
                        output.accept(ModItems.MANAGER_KEY);
                        output.accept(ModItems.GUIDE_BOOK);
                        output.accept(ModItems.CHIP_1);
                        output.accept(ModItems.CHIP_10);
                        output.accept(ModItems.CHIP_100);
                        output.accept(ModItems.CHIP_1000);
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DECOR = REGISTER.register("decor",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + CasinoMod.MODID + ".decor"))
                    .icon(() -> ModDecor.NEON_SIGN_BLOCKS.get(1).get().asItem().getDefaultInstance())
                    .withTabsBefore(GAMES.getKey())
                    .displayItems((parameters, output) -> {
                        ModDecor.all().forEach(output::accept);
                        ModItems.barMenu().forEach(output::accept);
                        output.accept(ModItems.CERAMIC_CUP);
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
