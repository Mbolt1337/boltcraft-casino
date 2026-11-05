package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.entity.LinkedBlockEntity.LinkStatus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server to the player at a slot machine. Reel state itself comes with the block entity. */
public record SlotMachineState(int containerId, LinkStatus status, long balance, List<Integer> bets,
                               PayTable payTable) implements CustomPacketPayload {
    public static final Type<SlotMachineState> TYPE = new Type<>(CasinoMod.id("slot_machine_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotMachineState> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SlotMachineState::containerId,
            CasinoCodecs.enumCodec(LinkStatus.values()), SlotMachineState::status,
            ByteBufCodecs.VAR_LONG, SlotMachineState::balance,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(16)), SlotMachineState::bets,
            PayTable.CODEC, SlotMachineState::payTable,
            SlotMachineState::new);

    @Override
    public Type<SlotMachineState> type() {
        return TYPE;
    }

    /** Three-of-a-kind multiplier per symbol, in symbol order, and the two-cherry multiplier. */
    public record PayTable(List<Integer> threeOfAKind, int twoCherries) {
        public static final StreamCodec<ByteBuf, PayTable> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(16)), PayTable::threeOfAKind,
                ByteBufCodecs.VAR_INT, PayTable::twoCherries,
                PayTable::new);
    }
}
