package com.mbolt.casino.menu;

import com.mbolt.casino.block.entity.LinkedBlockEntity.LinkStatus;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.slot.SlotMachineLogic;
import com.mbolt.casino.game.slot.SlotSymbol;
import com.mbolt.casino.inventory.ChipWallet;
import com.mbolt.casino.network.SlotMachineState;
import com.mbolt.casino.network.SlotSpinRequest;
import com.mbolt.casino.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Bet selection and the spin button. The spin itself runs in {@link SlotMachineBlockEntity#spin}. */
public final class SlotMachineMenu extends CasinoMenu<SlotMachineBlockEntity, SlotMachineState> {
    public SlotMachineMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.SLOT_MACHINE.get(), containerId, inventory, pos, SlotMachineBlockEntity.class,
                SlotMachineState.class);
    }

    @Override
    protected SlotMachineState buildState() {
        SlotMachineLogic logic = CasinoServerConfig.slotLogic();
        List<Integer> threeOfAKind = new ArrayList<>();
        for (SlotSymbol symbol : SlotSymbol.values()) {
            threeOfAKind.add(logic.payout(symbol.ordinal()));
        }
        var payTable = new SlotMachineState.PayTable(List.copyOf(threeOfAKind), logic.twoCherryPayout());
        LinkStatus status = blockEntity.hasOwner() ? blockEntity.linkStatus() : LinkStatus.NOT_LINKED;
        return new SlotMachineState(containerId, status, ChipWallet.balance(player),
                CasinoServerConfig.slotBets(), payTable);
    }

    public void spin(SlotSpinRequest request) {
        if (!request.pos().equals(pos) || !tryStartAction()) {
            return;
        }
        Component refusal = blockEntity.spin((ServerPlayer) player, request.bet());
        if (refusal != null) {
            sendNotice(refusal, false);
        } else {
            sendNotice(Component.empty(), true);
        }
        sendStateNow();
    }
}
