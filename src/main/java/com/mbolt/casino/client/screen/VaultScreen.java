package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.inventory.ResourceItems;
import com.mbolt.casino.menu.VaultMenu;
import com.mbolt.casino.network.ResourceRow;
import com.mbolt.casino.network.VaultJournalPage;
import com.mbolt.casino.network.VaultJournalRequest;
import com.mbolt.casino.network.VaultState;
import com.mbolt.casino.network.VaultTransfer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** The owner's vault: balances, backing deposits, profit withdrawal and the operation journal. */
public final class VaultScreen extends CasinoScreen<VaultMenu> {
    private static final int WIDTH = 256;
    private static final int HEIGHT = 226;
    private static final int TABS_TOP = 43;
    private static final int CONTENT_TOP = 66;
    private static final int ACTION_ROW = 182;
    private static final int JOURNAL_LINE_HEIGHT = 18;
    private static final int JOURNAL_PADDING = 3;
    private static final int RIGHT_COLUMN = 132;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ResourceList list = new ResourceList(MARGIN, CONTENT_TOP, WIDTH - 2 * MARGIN, 4);
    private AmountField amount;
    private List<ChipButton> actions = List.of();
    private ChipButton resourcesTab;
    private ChipButton journalTab;
    private ChipButton previousPage;
    private ChipButton nextPage;
    private boolean showingJournal;
    @Nullable
    private VaultJournalPage journal;
    private int requestedPage;
    private long ledgerRevisionSeen = -1;

    public VaultScreen(VaultMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        int amountTop = CONTENT_TOP + list.height() + 8;
        amount = addRenderableWidget(new AmountField(font, leftPos + 92, topPos + amountTop, 54, 16));
        actions = List.of(
                addButton(CasinoText.gui("deposit"), MARGIN, ACTION_ROW, 86, 20, () -> transfer(true, false)),
                addButton(CasinoText.gui("all"), MARGIN + 88, ACTION_ROW, 30, 20, () -> transfer(true, true)),
                addButton(CasinoText.gui("withdraw"), 128, ACTION_ROW, 86, 20, () -> transfer(false, false)),
                addButton(CasinoText.gui("all"), 216, ACTION_ROW, 30, 20, () -> transfer(false, true)));
        resourcesTab = addButton(CasinoText.gui("tab_resources"), MARGIN, TABS_TOP, 72, 18, () -> openTab(false));
        journalTab = addButton(CasinoText.gui("tab_journal"), MARGIN + 74, TABS_TOP, 72, 18, () -> openTab(true));
        previousPage = addButton(Component.literal("<"), WIDTH - MARGIN - 40, TABS_TOP, 18, 18,
                () -> requestPage(requestedPage - 1));
        nextPage = addButton(Component.literal(">"), WIDTH - MARGIN - 18, TABS_TOP, 18, 18,
                () -> requestPage(requestedPage + 1));
    }

    /** Called by the payload handler with the page the server sent. */
    public void showJournal(VaultJournalPage page) {
        journal = page;
        requestedPage = page.page();
    }

    @Nullable
    public VaultJournalPage displayedJournal() {
        return journal;
    }

    private void openTab(boolean journalTabOpen) {
        showingJournal = journalTabOpen;
        if (journalTabOpen) {
            requestPage(0);
        }
    }

    private void requestPage(int page) {
        int pageCount = journal == null ? 1 : journal.pageCount();
        requestedPage = Math.clamp(page, 0, Math.max(0, pageCount - 1));
        PacketDistributor.sendToServer(new VaultJournalRequest(menu.containerId, requestedPage));
    }

    private void transfer(boolean deposit, boolean all) {
        int count = all ? 0 : amount.amount();
        if (!list.selected().isEmpty() && (all || count > 0)) {
            PacketDistributor.sendToServer(new VaultTransfer(menu.containerId, list.selected(), count, deposit));
        }
    }

    private List<ResourceRow> rows() {
        VaultState state = menu.state();
        return state == null ? List.of() : state.rows();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        list.update(rows());
        VaultState state = menu.state();
        // A new journal entry while the journal is open refreshes the page being viewed.
        if (state != null && showingJournal && state.ledgerRevision() != ledgerRevisionSeen) {
            ledgerRevisionSeen = state.ledgerRevision();
            requestPage(requestedPage);
        }
        boolean canAct = state != null && !list.selected().isEmpty();
        actions.forEach(button -> {
            button.visible = !showingJournal;
            button.active = canAct;
        });
        amount.visible = !showingJournal;
        resourcesTab.setSelected(!showingJournal);
        journalTab.setSelected(showingJournal);
        int pageCount = journal == null ? 1 : journal.pageCount();
        previousPage.visible = showingJournal;
        nextPage.visible = showingJournal;
        previousPage.active = requestedPage > 0;
        nextPage.active = requestedPage < pageCount - 1;
    }

    @Override
    protected void renderPanels(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (showingJournal) {
            int height = VaultJournalPage.PAGE_SIZE * JOURNAL_LINE_HEIGHT + 2 * JOURNAL_PADDING;
            graphics.blitSprite(CasinoGui.INSET, leftPos + MARGIN - 2, topPos + CONTENT_TOP - 2,
                    WIDTH - 2 * MARGIN + 4, height + 4);
            return;
        }
        list.renderBackground(graphics, leftPos, topPos, rows(), mouseX, mouseY);
        amount.renderInset(graphics);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        VaultState state = menu.state();
        if (state == null) {
            return;
        }
        VaultState.Totals totals = state.totals();
        graphics.drawString(font, CasinoText.gui("assets", totals.assets()), MARGIN, 21, CasinoGui.CREAM, false);
        graphics.drawString(font, CasinoText.gui("outstanding", totals.outstanding()), MARGIN, 31,
                CasinoGui.MUTED, false);
        int freeColor = totals.freeReserve() >= 0 ? CasinoGui.GOLD : CasinoGui.ERROR;
        graphics.drawString(font, CasinoText.gui("free_reserve", totals.freeReserve()), RIGHT_COLUMN, 21,
                freeColor, false);
        graphics.drawString(font, CasinoText.gui("reserved", totals.reserved()), RIGHT_COLUMN, 31,
                CasinoGui.MUTED, false);
        if (showingJournal) {
            renderJournal(graphics);
            return;
        }
        list.renderRows(graphics, font, state.rows(), "in_vault");
        int amountTop = CONTENT_TOP + list.height() + 8;
        graphics.drawString(font, CasinoText.gui("amount"), MARGIN, amountTop + 4, CasinoGui.CREAM, false);
    }

    private void renderJournal(GuiGraphics graphics) {
        if (journal == null) {
            return;
        }
        String pages = (journal.page() + 1) + "/" + journal.pageCount();
        graphics.drawString(font, pages, WIDTH - MARGIN - 46 - font.width(pages), TABS_TOP + 5, CasinoGui.MUTED,
                false);
        if (journal.lines().isEmpty()) {
            graphics.drawString(font, CasinoText.gui("journal_empty"), MARGIN + 4, CONTENT_TOP + JOURNAL_PADDING,
                    CasinoGui.MUTED, false);
        }
        int y = CONTENT_TOP + JOURNAL_PADDING;
        int left = MARGIN + 4;
        int right = WIDTH - MARGIN - 4;
        for (VaultJournalPage.Line line : journal.lines()) {
            String header = TIME.format(Instant.ofEpochMilli(line.time())) + "  " + line.player();
            Component title = CasinoText.gui("journal." + line.type());
            String titleText = fitText(title.getString(), (right - left) * 2 / 3);
            int titleX = right - font.width(titleText);
            graphics.drawString(font, fitText(header, titleX - left - 6), left, y, CasinoGui.MUTED, false);
            graphics.drawString(font, titleText, titleX, y, CasinoGui.GOLD, false);
            graphics.drawString(font, fitText(journalDetails(line).getString(), right - left), left, y + 9,
                    CasinoGui.CREAM, false);
            y += JOURNAL_LINE_HEIGHT;
        }
    }

    private Component journalDetails(VaultJournalPage.Line line) {
        Component item = ResourceItems.item(line.item()).map(Item::getDescription).orElse(Component.empty());
        return CasinoText.gui("journal." + line.type() + ".details", line.count(), item,
                CasinoText.chips(line.value()));
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (!showingJournal || journal == null) {
            return;
        }
        int relativeX = mouseX - leftPos;
        int relativeY = mouseY - topPos - CONTENT_TOP - JOURNAL_PADDING;
        if (relativeX < MARGIN || relativeX >= WIDTH - MARGIN || relativeY < 0) {
            return;
        }
        int index = relativeY / JOURNAL_LINE_HEIGHT;
        if (index < journal.lines().size()) {
            VaultJournalPage.Line line = journal.lines().get(index);
            Component text = Component.literal(TIME.format(Instant.ofEpochMilli(line.time())) + "  " + line.player())
                    .append("\n").append(CasinoText.gui("journal." + line.type()))
                    .append("\n").append(journalDetails(line));
            textTooltip(graphics, text, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!showingJournal && button == 0 && list.click(mouseX - leftPos, mouseY - topPos, rows())) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (showingJournal) {
            requestPage(requestedPage - (int) Math.signum(scrollY));
            return true;
        }
        if (list.scroll(mouseX - leftPos, mouseY - topPos, scrollY, rows())) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
