package com.mbolt.casino.datagen;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.advancement.CasinoEvent;
import com.mbolt.casino.advancement.CasinoTrigger;
import com.mbolt.casino.registry.ModDecor;
import com.mbolt.casino.registry.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.function.Consumer;

/**
 * The casino advancement tab, on a velvet background: money (chips, slots, roulette), the house (vault, links,
 * abandoned casinos) and neon. Titles and descriptions are in {@link Translations}.
 */
final class CasinoAdvancements implements AdvancementProvider.AdvancementGenerator {
    private static final ResourceLocation BACKGROUND = CasinoMod.id("textures/block/decor/velvet.png");

    private Consumer<AdvancementHolder> saver;
    private ExistingFileHelper files;

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver,
                         ExistingFileHelper files) {
        this.saver = saver;
        this.files = files;
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(ModItems.CHIP_1000.get(), title("root"), description("root"), BACKGROUND,
                        AdvancementType.TASK, false, false, false)
                .addCriterion("joined", PlayerTrigger.TriggerInstance.tick())
                .save(saver, CasinoMod.id("casino/root"), files);

        AdvancementHolder chips = event(root, "first_chips", ModItems.CHIP_1.get(), AdvancementType.TASK,
                CasinoEvent.FIRST_CHIPS);
        AdvancementHolder spin = event(chips, "first_spin", ModItems.SLOT_MACHINE.get(), AdvancementType.TASK,
                CasinoEvent.FIRST_SPIN);
        event(spin, "jackpot", ModItems.CHIP_1000.get(), AdvancementType.CHALLENGE, CasinoEvent.JACKPOT);
        AdvancementHolder bet = event(chips, "roulette_bet", ModItems.ROULETTE_TABLE.get(), AdvancementType.TASK,
                CasinoEvent.ROULETTE_BET);
        AdvancementHolder straight = event(bet, "straight_up", ModItems.CHIP_100.get(), AdvancementType.GOAL,
                CasinoEvent.STRAIGHT_UP);
        event(straight, "zero", ModDecor.CARPET_FULL_BLOCKS.get(2).get(), AdvancementType.CHALLENGE,
                CasinoEvent.ZERO);
        event(chips, "high_roller", ModItems.CHIP_1000.get(), AdvancementType.CHALLENGE, CasinoEvent.HIGH_ROLLER);

        AdvancementHolder house = event(root, "own_vault", ModItems.VAULT.get(), AdvancementType.TASK,
                CasinoEvent.OWN_VAULT);
        event(house, "link", ModItems.MANAGER_KEY.get(), AdvancementType.TASK, CasinoEvent.LINK);
        event(house, "claim_abandoned", ModItems.CASHIER.get(), AdvancementType.GOAL, CasinoEvent.CLAIM_ABANDONED);

        AdvancementHolder letter = placed(root, "neon_letter", ModDecor.NEON_LETTER.get(), ModDecor.NEON_LETTER.get());
        placed(letter, "neon_board", ModDecor.NEON_PANEL_BOLTCRAFT.get(), ModDecor.NEON_PANEL_BOLTCRAFT.get(),
                ModDecor.NEON_PANEL_JACKPOT.get(), ModDecor.NEON_PANEL_BAR.get(), ModDecor.NEON_PANEL_VIP.get());
    }

    private AdvancementHolder event(AdvancementHolder parent, String name, ItemLike icon, AdvancementType type,
                                    CasinoEvent event) {
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(icon, title(name), description(name), null, type, true, type != AdvancementType.TASK,
                        false)
                .addCriterion(event.getSerializedName(), CasinoTrigger.on(event))
                .save(saver, CasinoMod.id("casino/" + name), files);
    }

    /** Done when the player places any of the blocks. */
    private AdvancementHolder placed(AdvancementHolder parent, String name, ItemLike icon, Block... blocks) {
        var builder = Advancement.Builder.advancement()
                .parent(parent)
                .display(icon, title(name), description(name), null, AdvancementType.TASK, true, false, false)
                .requirements(AdvancementRequirements.Strategy.OR);
        for (Block block : blocks) {
            builder.addCriterion(BuiltInRegistries.BLOCK.getKey(block).getPath(),
                    ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block));
        }
        return builder.save(saver, CasinoMod.id("casino/" + name), files);
    }

    static Component title(String name) {
        return Component.translatable("advancements." + CasinoMod.MODID + "." + name + ".title");
    }

    static Component description(String name) {
        return Component.translatable("advancements." + CasinoMod.MODID + "." + name + ".description");
    }
}
