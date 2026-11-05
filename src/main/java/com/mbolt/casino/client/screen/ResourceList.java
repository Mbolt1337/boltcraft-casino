package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.inventory.ResourceItems;
import com.mbolt.casino.network.ResourceRow;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The scrollable list of exchangeable resources in the cashier and vault screens: icon, name, rate, how many the
 * player carries and how many the vault holds. One row is selected and is the target of the action buttons.
 */
final class ResourceList {
    static final int ROW_HEIGHT = 22;
    private static final int ICON_SIZE = 16;
    private static final int TEXT_LEFT = 24;
    private static final int COUNT_COLUMN = 136;

    private final int x;
    private final int y;
    private final int width;
    private final int visibleRows;
    private int scroll;
    private String selected = "";

    /** Coordinates are relative to the screen panel corner. */
    ResourceList(int x, int y, int width, int visibleRows) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.visibleRows = visibleRows;
    }

    int height() {
        return visibleRows * ROW_HEIGHT;
    }

    String selected() {
        return selected;
    }

    /** Keeps a valid selection when the rows change, starting with the first resource the player carries. */
    void update(List<ResourceRow> rows) {
        boolean present = rows.stream().anyMatch(row -> row.item().equals(selected));
        if (!present) {
            selected = rows.stream().filter(row -> row.carried() > 0).findFirst()
                    .or(() -> rows.stream().findFirst())
                    .map(ResourceRow::item)
                    .orElse("");
        }
        scroll = Math.clamp(scroll, 0, maxScroll(rows));
    }

    void renderBackground(GuiGraphics graphics, int left, int top, List<ResourceRow> rows, int mouseX, int mouseY) {
        graphics.blitSprite(CasinoGui.INSET, left + x - 2, top + y - 2, width + 4, height() + 4);
        for (int line = 0; line < visibleRows; line++) {
            int index = scroll + line;
            if (index >= rows.size()) {
                break;
            }
            int rowTop = top + y + line * ROW_HEIGHT;
            boolean hovered = mouseX >= left + x && mouseX < left + x + width
                    && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
            if (rows.get(index).item().equals(selected)) {
                graphics.blitSprite(CasinoGui.ROW_SELECTED, left + x, rowTop, width, ROW_HEIGHT);
            } else if (hovered) {
                graphics.blitSprite(CasinoGui.ROW_HOVERED, left + x, rowTop, width, ROW_HEIGHT);
            }
        }
    }

    /** Draws in panel-relative coordinates; {@code storedLabel} names the vault column. */
    void renderRows(GuiGraphics graphics, Font font, List<ResourceRow> rows, String storedLabel) {
        for (int line = 0; line < visibleRows; line++) {
            int index = scroll + line;
            if (index >= rows.size()) {
                break;
            }
            ResourceRow row = rows.get(index);
            int rowTop = y + line * ROW_HEIGHT;
            ItemStack stack = ResourceItems.item(row.item()).map(ItemStack::new).orElse(ItemStack.EMPTY);
            graphics.renderItem(stack, x + 3, rowTop + (ROW_HEIGHT - ICON_SIZE) / 2);

            Component name = stack.isEmpty() ? Component.literal(row.item()) : stack.getHoverName();
            int nameWidth = COUNT_COLUMN - TEXT_LEFT - 4;
            graphics.drawString(font, font.plainSubstrByWidth(name.getString(), nameWidth), x + TEXT_LEFT,
                    rowTop + 2, CasinoGui.CREAM, false);
            Component rate = row.rate() > 0
                    ? CasinoText.gui("rate", CasinoText.chips(row.rate()))
                    : CasinoText.gui("no_rate");
            graphics.drawString(font, rate, x + TEXT_LEFT, rowTop + 12, CasinoGui.MUTED, false);
            graphics.drawString(font, CasinoText.gui("carried", row.carried()), x + COUNT_COLUMN, rowTop + 2,
                    CasinoGui.CREAM, false);
            graphics.drawString(font, CasinoText.gui(storedLabel, row.stored()), x + COUNT_COLUMN, rowTop + 12,
                    CasinoGui.MUTED, false);
        }
    }

    /** Selects the row under the cursor; coordinates are relative to the panel corner. */
    boolean click(double mouseX, double mouseY, List<ResourceRow> rows) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height()) {
            return false;
        }
        int index = scroll + (int) ((mouseY - y) / ROW_HEIGHT);
        if (index < rows.size()) {
            selected = rows.get(index).item();
            return true;
        }
        return false;
    }

    boolean scroll(double mouseX, double mouseY, double amount, List<ResourceRow> rows) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height()) {
            return false;
        }
        scroll = Math.clamp(scroll - (int) Math.signum(amount), 0, maxScroll(rows));
        return true;
    }

    @Nullable
    ResourceRow selectedRow(List<ResourceRow> rows) {
        return rows.stream().filter(row -> row.item().equals(selected)).findFirst().orElse(null);
    }

    private int maxScroll(List<ResourceRow> rows) {
        return Math.max(0, rows.size() - visibleRows);
    }
}
