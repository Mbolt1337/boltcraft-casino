package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.menu.CashierMenu;
import com.mbolt.casino.menu.SlotMachineMenu;
import com.mbolt.casino.menu.VaultMenu;
import com.mbolt.casino.menu.RouletteMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER =
            DeferredRegister.create(Registries.MENU, CasinoMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<VaultMenu>> VAULT = REGISTER.register("vault",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new VaultMenu(id, inventory, buf.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<CashierMenu>> CASHIER = REGISTER.register("cashier",
            () -> IMenuTypeExtension.create(
                    (id, inventory, buf) -> new CashierMenu(id, inventory, buf.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<SlotMachineMenu>> SLOT_MACHINE =
            REGISTER.register("slot_machine", () -> IMenuTypeExtension.create(
                    (id, inventory, buf) -> new SlotMachineMenu(id, inventory, buf.readBlockPos())));

    private ModMenus() {
    }

    public static final DeferredHolder<MenuType<?>, MenuType<RouletteMenu>> ROULETTE_TABLE =
            REGISTER.register("roulette_table", () -> IMenuTypeExtension.create(
                    (id, inventory, buf) -> new RouletteMenu(id, inventory, buf.readBlockPos())));
}
