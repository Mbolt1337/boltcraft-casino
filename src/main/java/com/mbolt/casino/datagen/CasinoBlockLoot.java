package com.mbolt.casino.datagen;

import com.mbolt.casino.block.SlotMachineBlock;
import com.mbolt.casino.block.RouletteTableBlock;
import com.mbolt.casino.block.decor.BoardBlock;
import com.mbolt.casino.block.decor.NeonPanelBlock;
import com.mbolt.casino.registry.ModBlocks;
import com.mbolt.casino.registry.ModDecor;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Casino blocks drop themselves; the slot machine only from its lower half, like a door, a neon board only
 * from its left end. */
final class CasinoBlockLoot extends BlockLootSubProvider {
    CasinoBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.VAULT.get());
        dropSelf(ModBlocks.CASHIER.get());
        add(ModBlocks.ROULETTE_TABLE.get(),
                block -> LootTable.lootTable().withPool(applyExplosionCondition(block,
                        LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(block))
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(RouletteTableBlock.PART, 0))))));
        add(ModBlocks.SLOT_MACHINE.get(),
                block -> createSinglePropConditionTable(block, SlotMachineBlock.HALF, DoubleBlockHalf.LOWER));
        for (var decor : ModDecor.all()) {
            Block block = decor.get();
            if (block instanceof BoardBlock) {
                // A board drops once, from its bottom left part; the other parts break with it.
                add(block, board -> LootTable.lootTable().withPool(applyExplosionCondition(board,
                        LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(board))
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(board)
                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(BoardBlock.PART, 0))))));
            } else if (block instanceof NeonPanelBlock) {
                // A board drops once, from its left end; the other parts break with it.
                add(block, panel -> LootTable.lootTable().withPool(applyExplosionCondition(panel,
                        LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(panel))
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(panel)
                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(NeonPanelBlock.PART, 0))))));
            } else if (block instanceof SlabBlock) {
                add(block, this::createSlabItemTable);
            } else {
                dropSelf(block);
            }
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> blocks = new ArrayList<>(List.of(ModBlocks.VAULT.get(), ModBlocks.CASHIER.get(),
                ModBlocks.SLOT_MACHINE.get(), ModBlocks.ROULETTE_TABLE.get()));
        ModDecor.all().forEach(decor -> blocks.add(decor.get()));
        return blocks;
    }
}
