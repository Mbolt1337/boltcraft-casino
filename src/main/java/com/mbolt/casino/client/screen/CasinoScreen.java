package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoText;
import com.mbolt.casino.menu.CasinoMenu;
import com.mbolt.casino.network.MenuNotice;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** Frame, title and the fading outcome line shared by every casino screen. */
abstract class CasinoScreen<M extends CasinoMenu<?, ?>> extends AbstractContainerScreen<M> {
    protected static final int MARGIN = 10;
    private static final int NOTICE_TICKS = 100;
    private static final int NOTICE_FADE_TICKS = 20;
    private static final int MAX_NOTICE_TICKS = 300;
    private static final String ELLIPSIS = "…";

    protected CasinoScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title);
        imageWidth = width;
        imageHeight = height;
    }

    /** Backgrounds drawn on top of the panel, in screen coordinates. */
    protected abstract void renderPanels(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /** Text and icons, in coordinates relative to the panel corner. */
    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY);

    /** The player's chips at the top right, after a chip. */
    protected void renderBalance(GuiGraphics graphics, long balance) {
        Component text = CasinoText.gui("balance", balance);
        int x = imageWidth - MARGIN - font.width(text);
        graphics.blitSprite(CasinoGui.CHIP_ICON, x - CasinoGui.CHIP_ICON_SIZE - 3, MARGIN - 1,
                CasinoGui.CHIP_ICON_SIZE, CasinoGui.CHIP_ICON_SIZE);
        graphics.drawString(font, text, x, MARGIN - 1, CasinoGui.GOLD, false);
    }

    protected ChipButton addButton(Component label, int x, int y, int width, int height, Runnable action) {
        return addRenderableWidget(new ChipButton(leftPos + x, topPos + y, width, height, label, action));
    }

    /** An explicit ellipsis makes shortened text discoverable; the original remains available in a tooltip. */
    protected String fitText(String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        int available = width - font.width(ELLIPSIS);
        return available < 0 ? "" : font.plainSubstrByWidth(text, available) + ELLIPSIS;
    }

    protected void textTooltip(GuiGraphics graphics, Component text, int mouseX, int mouseY) {
        int maxWidth = Math.max(80, Math.min(300, width - 20));
        graphics.renderTooltip(font, font.split(text, maxWidth), mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blitSprite(CasinoGui.PANEL, leftPos, topPos, imageWidth, imageHeight);
        renderPanels(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, MARGIN, MARGIN - 1, CasinoGui.GOLD, false);
        renderContent(graphics, mouseX, mouseY);
        renderNotice(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        MenuNotice notice = visibleNotice();
        int y = topPos + imageHeight - MARGIN - font.lineHeight + 1;
        boolean hovered = mouseX >= leftPos + MARGIN && mouseX < leftPos + imageWidth - MARGIN
                && mouseY >= y && mouseY < y + font.lineHeight;
        if (notice != null && hovered) {
            textTooltip(graphics, notice.text(), mouseX, mouseY);
        }
    }

    /** Typing into a field must not trigger key bindings such as the inventory key, which closes the screen. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getFocused() instanceof EditBox field && field.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return field.keyPressed(keyCode, scanCode, modifiers) || field.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** The last outcome sent by the server, fading out after a few seconds. */
    private void renderNotice(GuiGraphics graphics) {
        MenuNotice notice = visibleNotice();
        if (notice == null) {
            return;
        }
        long age = minecraft.level.getGameTime() - menu.noticeTick();
        float fade = Math.clamp((noticeDuration(notice) - age) / (float) NOTICE_FADE_TICKS, 0.1f, 1.0f);
        int base = notice.success() ? CasinoGui.SUCCESS : CasinoGui.ERROR;
        int color = FastColor.ARGB32.color((int) (fade * 255), base);
        int width = imageWidth - 2 * MARGIN;
        int y = imageHeight - MARGIN - font.lineHeight + 1;
        String singleLine = notice.text().getString().replace('\n', ' ');
        graphics.drawString(font, fitText(singleLine, width), MARGIN, y, color, false);
    }

    @Nullable
    private MenuNotice visibleNotice() {
        MenuNotice notice = menu.notice();
        if (notice == null || minecraft == null || minecraft.level == null) {
            return null;
        }
        long age = minecraft.level.getGameTime() - menu.noticeTick();
        return age <= noticeDuration(notice) ? notice : null;
    }

    private int noticeDuration(MenuNotice notice) {
        int lines = font.split(notice.text(), imageWidth - 2 * MARGIN).size();
        return Math.min(MAX_NOTICE_TICKS, NOTICE_TICKS + Math.max(0, lines - 1) * 20);
    }
}
