package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.CasinoText;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The guide book: two cream pages in a leather cover, turned with the gold arrows, the arrow keys or the mouse
 * wheel. Each section of {@link GuideContent} starts on a new page; recipes are drawn from the recipes the server
 * sends, the rates and the pay table from the server's config.
 */
public final class GuideBookScreen extends Screen {
    private static final int WIDTH = 296;
    private static final int HEIGHT = 188;
    private static final int PAGE_WIDTH = 140;
    private static final int PAGE_HEIGHT = 176;
    private static final int MARGIN = 6;
    private static final int RIGHT_PAGE = 150;
    private static final int TEXT_TOP = 14;
    private static final int TEXT_INSET_OUTER = 13;
    private static final int TEXT_INSET_SPINE = 9;
    static final int TEXT_WIDTH = PAGE_WIDTH - TEXT_INSET_OUTER - TEXT_INSET_SPINE;
    private static final int TEXT_HEIGHT = 140;
    private static final int FOOTER = MARGIN + PAGE_HEIGHT - 15;
    private static final int ARROW_WIDTH = 18;
    private static final int ARROW_HEIGHT = 10;
    private static final int SLOT = 18;
    private static final int SYMBOL = 12;
    private static final int LINE = 10;

    static final int INK = 0xFF3B2416;
    static final int HEADING = 0xFF7A1020;
    static final int MUTED = 0xFF8C7B62;

    private static final ResourceLocation COVER = CasinoMod.id("book_cover");
    private static final ResourceLocation PAGE_LEFT = CasinoMod.id("book_page_left");
    private static final ResourceLocation PAGE_RIGHT = CasinoMod.id("book_page_right");
    private static final ResourceLocation SLOT_SPRITE = CasinoMod.id("book_slot");
    private static final ResourceLocation ARROW = CasinoMod.id("book_arrow");
    private static final ResourceLocation NEXT = CasinoMod.id("book_next");
    private static final ResourceLocation NEXT_LIT = CasinoMod.id("book_next_highlighted");
    private static final ResourceLocation PREVIOUS = CasinoMod.id("book_previous");
    private static final ResourceLocation PREVIOUS_LIT = CasinoMod.id("book_previous_highlighted");
    private static final ResourceLocation BUTTON = CasinoMod.id("button");
    private static final ResourceLocation BUTTON_LIT = CasinoMod.id("button_highlighted");
    private static final ResourceLocation SYMBOLS = CasinoMod.id("textures/gui/slot_symbols.png");

    /** Something laid out on a page, from the top down. */
    sealed interface Block permits Heading, Paragraph, Line, Gap, Recipe, Icons, Rate, Pay, Link {
        int height();
    }

    record Heading(Component text) implements Block {
        public int height() {
            return 16;
        }
    }

    /** Kept on one page when it fits on one; a longer one is broken into lines. */
    record Paragraph(List<FormattedCharSequence> lines) implements Block {
        public int height() {
            return lines.size() * LINE;
        }
    }

    record Line(FormattedCharSequence text) implements Block {
        public int height() {
            return LINE;
        }
    }

    record Gap(int height) implements Block {
    }

    /** A crafting grid, row by row, Ingredient.EMPTY for blank cells. */
    record Recipe(List<Ingredient> grid, ItemStack result) implements Block {
        public int height() {
            return 3 * SLOT + 6;
        }
    }

    record Icons(List<ItemStack> items) implements Block {
        public int height() {
            return SLOT + 2;
        }
    }

    /** One resource and the chips it is worth. */
    record Rate(ItemStack item, long chips) implements Block {
        public int height() {
            return SLOT;
        }
    }

    /** Three of a symbol, or with {@code pair} two cherries, and the payout in bets. */
    record Pay(int symbol, boolean pair, int payout) implements Block {
        public int height() {
            return SYMBOL + 3;
        }
    }

    record Link(Component label, String url) implements Block {
        public int height() {
            return 24;
        }
    }

    private record Placed(Block block, int y) {
    }

    private final List<List<Placed>> pages = new ArrayList<>();
    private int spread;
    private int left;
    private int top;
    @Nullable
    private ItemStack hovered;

    public GuideBookScreen() {
        super(Component.translatable("item." + CasinoMod.MODID + ".guide_book"));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        pages.clear();
        for (List<Block> section : GuideContent.sections(font)) {
            List<Placed> page = new ArrayList<>();
            int y = 0;
            List<Block> blocks = new ArrayList<>();
            for (Block block : section) {
                if (block instanceof Paragraph paragraph && paragraph.height() > TEXT_HEIGHT) {
                    paragraph.lines().forEach(line -> blocks.add(new Line(line)));
                } else {
                    blocks.add(block);
                }
            }
            for (Block block : blocks) {
                if (y + block.height() > TEXT_HEIGHT && !page.isEmpty()) {
                    // A gap that does not fit simply ends the page; it never starts the next one.
                    if (block instanceof Gap) {
                        continue;
                    }
                    pages.add(page);
                    page = new ArrayList<>();
                    y = 0;
                }
                page.add(new Placed(block, y));
                y += block.height();
            }
            pages.add(page);
        }
        spread = Math.min(spread, lastSpread());
    }

    private int lastSpread() {
        return Math.max(0, (pages.size() - 1) / 2);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(COVER, left, top, WIDTH, HEIGHT);
        graphics.blitSprite(PAGE_LEFT, left + MARGIN, top + MARGIN, PAGE_WIDTH, PAGE_HEIGHT);
        graphics.blitSprite(PAGE_RIGHT, left + RIGHT_PAGE, top + MARGIN, PAGE_WIDTH, PAGE_HEIGHT);
        hovered = null;
        for (int side = 0; side < 2; side++) {
            int index = spread * 2 + side;
            if (index >= pages.size()) {
                continue;
            }
            int x = left + (side == 0 ? MARGIN + TEXT_INSET_OUTER : RIGHT_PAGE + TEXT_INSET_SPINE);
            for (Placed placed : pages.get(index)) {
                render(graphics, placed.block(), x, top + MARGIN + TEXT_TOP + placed.y(), mouseX, mouseY);
            }
            String number = Integer.toString(index + 1);
            graphics.drawString(font, number, x + (TEXT_WIDTH - font.width(number)) / 2, top + FOOTER, MUTED,
                    false);
        }
        if (spread > 0) {
            boolean lit = overPrevious(mouseX, mouseY);
            graphics.blitSprite(lit ? PREVIOUS_LIT : PREVIOUS, previousX(), arrowY(), ARROW_WIDTH, ARROW_HEIGHT);
        }
        if (spread < lastSpread()) {
            boolean lit = overNext(mouseX, mouseY);
            graphics.blitSprite(lit ? NEXT_LIT : NEXT, nextX(), arrowY(), ARROW_WIDTH, ARROW_HEIGHT);
        }
        if (hovered != null) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    private void render(GuiGraphics graphics, Block block, int x, int y, int mouseX, int mouseY) {
        switch (block) {
            case Heading heading -> {
                graphics.drawString(font, heading.text(), x + (TEXT_WIDTH - font.width(heading.text())) / 2, y,
                        HEADING, false);
                graphics.blitSprite(CasinoGui.DIVIDER, x + 10, y + 11, TEXT_WIDTH - 20, 2);
            }
            case Paragraph paragraph -> {
                for (int i = 0; i < paragraph.lines().size(); i++) {
                    graphics.drawString(font, paragraph.lines().get(i), x, y + i * LINE, INK, false);
                }
            }
            case Line line -> graphics.drawString(font, line.text(), x, y, INK, false);
            case Gap gap -> {
            }
            case Recipe recipe -> {
                int gridX = x + 4;
                for (int cell = 0; cell < 9; cell++) {
                    int slotX = gridX + cell % 3 * SLOT;
                    int slotY = y + cell / 3 * SLOT;
                    graphics.blitSprite(SLOT_SPRITE, slotX, slotY, SLOT, SLOT);
                    item(graphics, shown(recipe.grid().get(cell)), slotX + 1, slotY + 1, mouseX, mouseY);
                }
                int arrowX = gridX + 3 * SLOT + 7;
                graphics.blitSprite(ARROW, arrowX, y + SLOT + 3, 16, 11);
                int resultX = arrowX + 16 + 8;
                graphics.blitSprite(SLOT_SPRITE, resultX, y + SLOT, SLOT, SLOT);
                item(graphics, recipe.result(), resultX + 1, y + SLOT + 1, mouseX, mouseY);
                graphics.renderItemDecorations(font, recipe.result(), resultX + 1, y + SLOT + 1);
            }
            case Icons icons -> {
                int iconX = x + (TEXT_WIDTH - icons.items().size() * SLOT) / 2;
                for (ItemStack stack : icons.items()) {
                    item(graphics, stack, iconX + 1, y + 1, mouseX, mouseY);
                    iconX += SLOT;
                }
            }
            case Rate rate -> {
                item(graphics, rate.item(), x + 2, y + 1, mouseX, mouseY);
                Component text = Component.literal("= ").append(CasinoText.chips(rate.chips()));
                graphics.drawString(font, text, x + 22, y + 5, INK, false);
            }
            case Pay pay -> {
                int count = pay.pair() ? 2 : 3;
                for (int i = 0; i < count; i++) {
                    graphics.blit(SYMBOLS, x + 2 + i * (SYMBOL + 1), y, SYMBOL, SYMBOL, pay.symbol() * 16, 0, 16, 16,
                            96, 16);
                }
                String payout = "× " + pay.payout();
                graphics.drawString(font, payout, x + TEXT_WIDTH - font.width(payout) - 4, y + 2, INK, false);
            }
            case Link link -> {
                int buttonX = x + 8;
                int buttonWidth = TEXT_WIDTH - 16;
                boolean lit = mouseX >= buttonX && mouseX < buttonX + buttonWidth && mouseY >= y + 2
                        && mouseY < y + 22;
                graphics.blitSprite(lit ? BUTTON_LIT : BUTTON, buttonX, y + 2, buttonWidth, 20);
                graphics.drawCenteredString(font, link.label(), buttonX + buttonWidth / 2, y + 8,
                        lit ? CasinoGui.GOLD : CasinoGui.CREAM);
            }
        }
    }

    private static ItemStack shown(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) {
            return ItemStack.EMPTY;
        }
        // Tags show each of their items in turn, a second apiece.
        return items[(int) (Util.getMillis() / 1000 % items.length)];
    }

    private void item(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.renderItem(stack, x, y);
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            hovered = stack;
        }
    }

    private int arrowY() {
        return top + FOOTER - 1;
    }

    private int previousX() {
        return left + MARGIN + TEXT_INSET_OUTER - 4;
    }

    private int nextX() {
        return left + RIGHT_PAGE + PAGE_WIDTH - TEXT_INSET_OUTER - ARROW_WIDTH + 4;
    }

    private boolean overPrevious(double mouseX, double mouseY) {
        return spread > 0 && over(mouseX, mouseY, previousX(), arrowY());
    }

    private boolean overNext(double mouseX, double mouseY) {
        return spread < lastSpread() && over(mouseX, mouseY, nextX(), arrowY());
    }

    private static boolean over(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + ARROW_WIDTH && mouseY >= y && mouseY < y + ARROW_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overPrevious(mouseX, mouseY)) {
            turn(-1);
            return true;
        }
        if (overNext(mouseX, mouseY)) {
            turn(1);
            return true;
        }
        Link link = clickedLink(mouseX, mouseY);
        if (link != null) {
            ConfirmLinkScreen.confirmLinkNow(this, link.url());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Nullable
    private Link clickedLink(double mouseX, double mouseY) {
        for (int side = 0; side < 2; side++) {
            int index = spread * 2 + side;
            if (index >= pages.size()) {
                continue;
            }
            int x = left + (side == 0 ? MARGIN + TEXT_INSET_OUTER : RIGHT_PAGE + TEXT_INSET_SPINE) + 8;
            for (Placed placed : pages.get(index)) {
                int y = top + MARGIN + TEXT_TOP + placed.y() + 2;
                if (placed.block() instanceof Link link && mouseX >= x && mouseX < x + TEXT_WIDTH - 16
                        && mouseY >= y && mouseY < y + 20) {
                    return link;
                }
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            turn(scrollY < 0 ? 1 : -1);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            turn(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            turn(-1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Turns to another spread; also used by the art preview. */
    public void turn(int spreads) {
        spread = Math.clamp(spread + spreads, 0, lastSpread());
    }

    /** Lines of a paragraph wrapped to the page. */
    static List<FormattedCharSequence> wrap(Font font, Component text) {
        return font.split(text, TEXT_WIDTH);
    }
}
