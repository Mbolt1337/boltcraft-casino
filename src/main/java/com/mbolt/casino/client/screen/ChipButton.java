package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;

/** A button drawn as a casino chip plaque; {@code selected} keeps it lit, used for the chosen bet. */
final class ChipButton extends Button {
    private static final WidgetSprites SPRITES = new WidgetSprites(
            CasinoMod.id("button"), CasinoMod.id("button_disabled"), CasinoMod.id("button_highlighted"));

    private boolean selected;

    ChipButton(int x, int y, int width, int height, Component label, Runnable action) {
        super(x, y, width, height, label, button -> action.run(), DEFAULT_NARRATION);
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean lit = selected || isHoveredOrFocused();
        graphics.blitSprite(SPRITES.get(active, lit), getX(), getY(), getWidth(), getHeight());
        int color = !active ? CasinoGui.MUTED : lit ? CasinoGui.GOLD : CasinoGui.CREAM;
        var font = Minecraft.getInstance().font;
        int textY = getY() + (getHeight() - font.lineHeight) / 2 + 1;
        graphics.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, textY, color);
    }
}
