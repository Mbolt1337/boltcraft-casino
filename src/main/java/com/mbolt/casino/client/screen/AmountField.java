package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

/** A borderless number field drawn on a casino inset; accepts 1-9999. */
final class AmountField extends EditBox {
    private static final int MAX_DIGITS = 4;

    AmountField(Font font, int x, int y, int width, int height) {
        super(font, x + 4, y + (height - font.lineHeight) / 2 + 1, width - 8, font.lineHeight,
                CasinoText.gui("amount"));
        setBordered(false);
        setMaxLength(MAX_DIGITS);
        setFilter(text -> text.matches("[0-9]*"));
        setTextColor(CasinoGui.CREAM);
        setValue("1");
    }

    /** The typed amount, or 0 when the field is empty or zero. */
    int amount() {
        try {
            return getValue().isEmpty() ? 0 : Integer.parseInt(getValue());
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    void renderInset(GuiGraphics graphics) {
        graphics.blitSprite(CasinoGui.INSET, getX() - 4, getY() - 5, getWidth() + 8, getHeight() + 8);
    }
}
