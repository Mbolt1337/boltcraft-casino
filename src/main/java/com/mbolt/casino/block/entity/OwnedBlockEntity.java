package com.mbolt.casino.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A casino block that may belong to a player. Blocks placed by players are owned from the start; blocks generated
 * with a structure have no owner until someone claims them with a manager key. Those are fixtures of their
 * building: they work where they stand, but breaking one gives no item, so an abandoned casino cannot be carried
 * off as a pile of free machines.
 */
public abstract class OwnedBlockEntity extends BlockEntity {
    private static final String OWNER_TAG = "Owner";
    private static final String OWNER_NAME_TAG = "OwnerName";
    private static final String FIXTURE_TAG = "Fixture";

    @Nullable
    private UUID owner;
    private String ownerName = "";
    private boolean fixture;

    protected OwnedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean hasOwner() {
        return owner != null;
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    /** True for blocks that came with a generated building. */
    public boolean isFixture() {
        return fixture;
    }

    /** Marks the block as part of a generated building; set while the building's template is made. */
    public void markFixture() {
        fixture = true;
        setChanged();
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        setChanged();
    }

    /** Saves the change and sends the public client state to everyone tracking this chunk. */
    protected void syncToClients() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID(OWNER_TAG, owner);
            tag.putString(OWNER_NAME_TAG, ownerName);
        }
        if (fixture) {
            tag.putBoolean(FIXTURE_TAG, true);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        ownerName = tag.getString(OWNER_NAME_TAG);
        fixture = tag.getBoolean(FIXTURE_TAG);
    }

    /** Public client state only; owner-only data such as the vault ledger never goes into block updates. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeClientState(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClientState(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        readClientState(packet.getTag());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Writes what every nearby client needs for rendering. */
    protected void writeClientState(CompoundTag tag) {
    }

    /** Reads the state written by {@link #writeClientState} on the client. */
    protected void readClientState(CompoundTag tag) {
    }
}
