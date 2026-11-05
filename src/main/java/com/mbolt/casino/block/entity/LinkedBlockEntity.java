package com.mbolt.casino.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** A casino block that pays and takes chips through a linked vault: the cashier and every game. */
public abstract class LinkedBlockEntity extends OwnedBlockEntity {
    private static final String LINK_TAG = "VaultLink";

    @Nullable
    private VaultLink link;

    protected LinkedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isLinked() {
        return link != null;
    }

    /** Games refuse a new link while a round is running against the old vault. */
    public boolean canRelink() {
        return true;
    }

    public void linkTo(VaultBlockEntity vault) {
        link = VaultLink.of(vault);
        setChanged();
    }

    /** The linked vault if it is currently reachable; always empty on the client. */
    public Optional<VaultBlockEntity> linkedVault() {
        if (link == null || !(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        return link.resolve(server);
    }

    public LinkStatus linkStatus() {
        if (link == null) {
            return LinkStatus.NOT_LINKED;
        }
        return linkedVault().isPresent() ? LinkStatus.READY : LinkStatus.VAULT_UNAVAILABLE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (link != null) {
            tag.put(LINK_TAG, link.save());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        link = VaultLink.load(tag.getCompound(LINK_TAG)).orElse(null);
    }

    public enum LinkStatus {
        NOT_LINKED,
        VAULT_UNAVAILABLE,
        READY
    }
}
