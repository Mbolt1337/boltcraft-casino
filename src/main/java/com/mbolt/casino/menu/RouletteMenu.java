package com.mbolt.casino.menu;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.RouletteRounds;
import com.mbolt.casino.game.roulette.RouletteBet;
import com.mbolt.casino.inventory.ChipWallet;
import com.mbolt.casino.network.RouletteBetRequest;
import com.mbolt.casino.network.RouletteState;
import com.mbolt.casino.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

public final class RouletteMenu extends CasinoMenu<RouletteTableBlockEntity, RouletteState> {
    public RouletteMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.ROULETTE_TABLE.get(), id, inventory, pos, RouletteTableBlockEntity.class, RouletteState.class);
    }

    @Override
    protected RouletteState buildState() {
        var names = new CompoundTag();
        if (blockEntity.round() != null) {
            for (var bet : blockEntity.round().snapshot().bets()) {
                names.putString(bet.player().toString(), PlayerNames.of(player.getServer(), bet.player()));
            }
        }
        return new RouletteState(containerId, ChipWallet.balance(player), blockEntity.roundTag(), names,
                CasinoServerConfig.ROULETTE_MAX_BET.get(), blockEntity.linkStatus().name());
    }

    public void bet(RouletteBetRequest request) {
        if (!tryStartAction()) {
            return;
        }
        RouletteBet.Type[] types = RouletteBet.Type.values();
        boolean invalid = request.betType() < 0 || request.betType() >= types.length || request.chips() <= 0;
        if (invalid) {
            sendNotice(CasinoText.message("roulette.invalid_bet"), false);
            return;
        }
        RouletteBet bet;
        try {
            bet = new RouletteBet(types[request.betType()], request.selection(), request.chips());
        } catch (IllegalArgumentException invalidSelection) {
            sendNotice(CasinoText.message("roulette.invalid_bet"), false);
            return;
        }
        var refusal = RouletteRounds.get(player.getServer()).place(blockEntity, (ServerPlayer) player, bet);
        sendNotice(refusal == null ? Component.empty() : refusal, refusal == null);
        sendStateNow();
    }
}
