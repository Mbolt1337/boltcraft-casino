package com.mbolt.casino.block.entity;

import com.mbolt.casino.registry.HospitalityTags;
import com.mbolt.casino.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Four visible, single-serving slots; normal container rules also make hopper transfers safe. */
public final class ServingTrayBlockEntity extends BlockEntity implements Container {
    public static final int SLOTS = 4;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public ServingTrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVING_TRAY.get(), pos, state);
    }

    public static boolean isServing(ItemStack stack) {
        return stack.is(HospitalityTags.BAR_MENU) && stack.has(DataComponents.FOOD);
    }

    public boolean serve(Player player, int slot) {
        ItemStack held = player.getMainHandItem();
        if (!isServing(held)) {
            return false;
        }
        for (int offset = 0; offset < SLOTS; offset++) {
            int target = (slot + offset) % SLOTS;
            if (items.get(target).isEmpty()) {
                items.set(target, held.copyWithCount(1));
                if (!player.hasInfiniteMaterials()) {
                    held.shrink(1);
                }
                setChanged();
                return true;
            }
        }
        return false;
    }

    public boolean take(Player player, int slot) {
        if (items.get(slot).isEmpty()) {
            return false;
        }
        ItemStack serving = removeItem(slot, 1);
        if (!player.getInventory().add(serving)) {
            player.drop(serving, false);
        }
        return true;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && !isServing(stack)) {
            return;
        }
        items.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return items.get(slot).isEmpty() && isServing(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getCenter()) <= 6 * 6;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        loadAdditional(packet.getTag(), registries);
    }
}
