package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.block.entity.LinkedBlockEntity.LinkStatus;
import com.mbolt.casino.menu.CashierMenu;
import com.mbolt.casino.network.CashierExchange;
import com.mbolt.casino.network.CashierState;
import com.mbolt.casino.network.ResourceRow;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Exchange resources for chips and back. */
public final class CashierScreen extends CasinoScreen<CashierMenu> {
    private static final int WIDTH = 256;
    private static final int HEIGHT = 232;
    private static final int LIST_TOP = 36;
    private static final int ACTION_ROW = 176;

    private final ResourceList list = new ResourceList(MARGIN, LIST_TOP, WIDTH - 2 * MARGIN, 5);
    private AmountField amount;
    private List<ChipButton> actions = List.of();

    public CashierScreen(CashierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        int amountTop = LIST_TOP + list.height() + 8;
        amount = addRenderableWidget(new AmountField(font, leftPos + 92, topPos + amountTop, 54, 16));
        actions = List.of(
                addButton(CasinoText.gui("to_chips"), MARGIN, ACTION_ROW, 86, 20, () -> exchange(true, false)),
                addButton(CasinoText.gui("all"), MARGIN + 88, ACTION_ROW, 30, 20, () -> exchange(true, true)),
                addButton(CasinoText.gui("to_resources"), 128, ACTION_ROW, 86, 20, () -> exchange(false, false)),
                addButton(CasinoText.gui("all"), 216, ACTION_ROW, 30, 20, () -> exchange(false, true)));
    }

    private List<ResourceRow> rows() {
        CashierState state = menu.state();
        return state == null ? List.of() : state.rows();
    }

    private void exchange(boolean toChips, boolean all) {
        int count = all ? 0 : amount.amount();
        if (!list.selected().isEmpty() && (all || count > 0)) {
            PacketDistributor.sendToServer(new CashierExchange(menu.containerId, list.selected(), count, toChips));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        list.update(rows());
        CashierState state = menu.state();
        boolean ready = state != null && state.status() == LinkStatus.READY && !list.selected().isEmpty();
        actions.forEach(button -> button.active = ready);
    }

    @Override
    protected void renderPanels(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        list.renderBackground(graphics, leftPos, topPos, rows(), mouseX, mouseY);
        amount.renderInset(graphics);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        CashierState state = menu.state();
        if (state == null) {
            return;
        }
        renderBalance(graphics, state.balance());
        Component status = switch (state.status()) {
            case NOT_LINKED -> CasinoText.message("not_linked");
            case VAULT_UNAVAILABLE -> CasinoText.message("vault_unavailable");
            case READY -> CasinoText.gui("fee", state.feePercent());
        };
        int statusColor = state.status() == LinkStatus.READY ? CasinoGui.MUTED : CasinoGui.ERROR;
        graphics.drawString(font, status, MARGIN, 22, statusColor, false);
        list.renderRows(graphics, font, state.rows(), "in_vault");
        int amountTop = LIST_TOP + list.height() + 8;
        graphics.drawString(font, CasinoText.gui("amount"), MARGIN, amountTop + 4, CasinoGui.CREAM, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && list.click(mouseX - leftPos, mouseY - topPos, rows())) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (list.scroll(mouseX - leftPos, mouseY - topPos, scrollY, rows())) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
