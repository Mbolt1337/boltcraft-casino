package com.mbolt.casino.network;

import com.mbolt.casino.block.decor.NeonLetterBlock;
import com.mbolt.casino.client.ClientPayloadHandler;
import com.mbolt.casino.menu.CashierMenu;
import com.mbolt.casino.menu.SlotMachineMenu;
import com.mbolt.casino.menu.VaultMenu;
import com.mbolt.casino.menu.RouletteMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;

/**
 * All casino payloads. Server handlers only run when the sender's open menu matches the payload, and the menu
 * re-validates the action. Client handlers are wrapped in lambdas so a dedicated server never loads client code.
 */
public final class CasinoNetwork {
    private static final String PROTOCOL_VERSION = "4";

    private CasinoNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToServer(CashierExchange.TYPE, CashierExchange.CODEC, (payload, context) ->
                onMenu(context, CashierMenu.class, payload.containerId(), menu -> menu.exchange(payload)));
        registrar.playToServer(VaultTransfer.TYPE, VaultTransfer.CODEC, (payload, context) ->
                onMenu(context, VaultMenu.class, payload.containerId(), menu -> menu.transfer(payload)));
        registrar.playToServer(VaultJournalRequest.TYPE, VaultJournalRequest.CODEC, (payload, context) ->
                onMenu(context, VaultMenu.class, payload.containerId(), menu -> menu.sendJournal(payload)));
        registrar.playToServer(SlotSpinRequest.TYPE, SlotSpinRequest.CODEC, (payload, context) ->
                onMenu(context, SlotMachineMenu.class, payload.containerId(), menu -> menu.spin(payload)));
        registrar.playToServer(RouletteBetRequest.TYPE, RouletteBetRequest.CODEC, (payload, context) ->
                onMenu(context, RouletteMenu.class, payload.containerId(), menu -> menu.bet(payload)));
        registrar.playToServer(NeonGlyphChoice.TYPE, NeonGlyphChoice.CODEC, (payload, context) ->
                context.enqueueWork(() -> NeonLetterBlock.choose(context.player(), payload.pos(), payload.glyph())));
        registrar.playToClient(NeonPickerOpen.TYPE, NeonPickerOpen.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.openNeonPicker(payload.pos())));
        registrar.playToClient(RouletteState.TYPE, RouletteState.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.menuState(payload.containerId(), payload)));

        registrar.playToClient(CashierState.TYPE, CashierState.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.menuState(payload.containerId(), payload)));
        registrar.playToClient(VaultState.TYPE, VaultState.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.menuState(payload.containerId(), payload)));
        registrar.playToClient(SlotMachineState.TYPE, SlotMachineState.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.menuState(payload.containerId(), payload)));
        registrar.playToClient(VaultJournalPage.TYPE, VaultJournalPage.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.journalPage(payload)));
        registrar.playToClient(MenuNotice.TYPE, MenuNotice.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.notice(payload)));
        registrar.playToClient(SlotSpinResult.TYPE, SlotSpinResult.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.spinResult(payload)));
    }

    /** Runs {@code action} on the server thread if the sender still has that exact menu open. */
    private static <M extends AbstractContainerMenu> void onMenu(IPayloadContext context, Class<M> menuType,
                                                                 int containerId, Consumer<M> action) {
        context.enqueueWork(() -> {
            AbstractContainerMenu open = context.player().containerMenu;
            if (menuType.isInstance(open) && open.containerId == containerId) {
                action.accept(menuType.cast(open));
            }
        });
    }
}
