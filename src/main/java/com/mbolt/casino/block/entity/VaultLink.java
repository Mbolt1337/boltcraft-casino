package com.mbolt.casino.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

/**
 * A reference to one particular vault. The identity guards against a different vault placed later at the same
 * position silently taking over the old links.
 */
public record VaultLink(GlobalPos pos, UUID vaultId) {
    public static final Codec<VaultLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("pos").forGetter(VaultLink::pos),
            UUIDUtil.CODEC.fieldOf("vault_id").forGetter(VaultLink::vaultId)
    ).apply(instance, VaultLink::new));

    public static final StreamCodec<ByteBuf, VaultLink> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, VaultLink::pos,
            UUIDUtil.STREAM_CODEC, VaultLink::vaultId,
            VaultLink::new);

    public static VaultLink of(VaultBlockEntity vault) {
        return new VaultLink(GlobalPos.of(vault.getLevel().dimension(), vault.getBlockPos()), vault.identity());
    }

    /** The linked vault when it is in this dimension, its chunk is loaded and it is still the same vault. */
    public Optional<VaultBlockEntity> resolve(ServerLevel level) {
        if (!pos.dimension().equals(level.dimension())) {
            return Optional.empty();
        }
        BlockPos block = pos.pos();
        if (level.getChunkSource().getChunkNow(block.getX() >> 4, block.getZ() >> 4) == null) {
            return Optional.empty();
        }
        if (level.getBlockEntity(block) instanceof VaultBlockEntity vault && vault.identity().equals(vaultId)) {
            return Optional.of(vault);
        }
        return Optional.empty();
    }

    public CompoundTag save() {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
    }

    public static Optional<VaultLink> load(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag).result();
    }
}
