package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.entity.CashierBlockEntity;
import com.mbolt.casino.block.entity.CasinoPartBlockEntity;
import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.block.entity.ServingTrayBlockEntity;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CasinoMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VaultBlockEntity>> VAULT =
            REGISTER.register("vault",
                    () -> BlockEntityType.Builder.of(VaultBlockEntity::new, ModBlocks.VAULT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CashierBlockEntity>> CASHIER =
            REGISTER.register("cashier",
                    () -> BlockEntityType.Builder.of(CashierBlockEntity::new, ModBlocks.CASHIER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SlotMachineBlockEntity>> SLOT_MACHINE =
            REGISTER.register("slot_machine",
                    () -> BlockEntityType.Builder.of(SlotMachineBlockEntity::new, ModBlocks.SLOT_MACHINE.get())
                            .build(null));

    private ModBlockEntities() {
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RouletteTableBlockEntity>> ROULETTE_TABLE =
            REGISTER.register("roulette_table", () -> BlockEntityType.Builder.of(RouletteTableBlockEntity::new,
                    ModBlocks.ROULETTE_TABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServingTrayBlockEntity>> SERVING_TRAY =
            REGISTER.register("serving_tray", () -> BlockEntityType.Builder.of(ServingTrayBlockEntity::new,
                    ModDecor.SERVING_TRAY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CasinoPartBlockEntity>> CASINO_PART =
            REGISTER.register("casino_part", () -> BlockEntityType.Builder.of(CasinoPartBlockEntity::new,
                    ModBlocks.SLOT_MACHINE.get(), ModBlocks.ROULETTE_TABLE.get()).build(null));
}
