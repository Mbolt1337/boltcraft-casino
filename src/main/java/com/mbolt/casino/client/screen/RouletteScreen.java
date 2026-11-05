package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.CasinoText;
import com.mbolt.casino.game.RouletteRoundNbt;
import com.mbolt.casino.game.roulette.RouletteBet;
import com.mbolt.casino.game.roulette.RouletteLogic;
import com.mbolt.casino.game.roulette.RouletteRound;
import com.mbolt.casino.menu.RouletteMenu;
import com.mbolt.casino.network.RouletteBetRequest;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compact native casino screen: the complete European field, denominations and a shared scrollable bet list. */
public final class RouletteScreen extends CasinoScreen<RouletteMenu> {
    private static final int WIDTH = 384;
    private static final int HEIGHT = 234;
    private static final int GRID_Y = 46;
    private static final int DOZEN_Y = 100;
    private static final int EVEN_Y = 118;
    private static final int ROW_HEIGHT = 18;
    /** Cream lines of the layout, showing between the cells. */
    private static final ResourceLocation FIELD = CasinoMod.id("roulette/field");
    private static final ResourceLocation CELL_RED = CasinoMod.id("roulette/cell_red");
    private static final ResourceLocation CELL_BLACK = CasinoMod.id("roulette/cell_black");
    private static final ResourceLocation CELL_FELT = CasinoMod.id("roulette/cell_felt");
    private static final ResourceLocation CELL_ZERO = CasinoMod.id("roulette/cell_zero");
    private static final ResourceLocation CELL_HOVER = CasinoMod.id("roulette/cell_hover");
    private static final ResourceLocation RESULT_RED = CasinoMod.id("roulette/result_red");
    private static final ResourceLocation RESULT_BLACK = CasinoMod.id("roulette/result_black");
    private static final ResourceLocation RESULT_ZERO = CasinoMod.id("roulette/result_zero");
    private static final ResourceLocation RESULT_NONE = CasinoMod.id("roulette/result_none");
    private static final ResourceLocation CHIP_MARKER = CasinoMod.id("roulette/chip_marker");
    private static final int RESULT_SIZE = 36;
    private static final int MARKER_SIZE = 5;
    private final List<Button> cells = new ArrayList<>();
    private final List<ChipButton> denominations = new ArrayList<>();
    private static final int[] CHIPS = {1, 5, 10, 50, 100, 500};
    private int stake = 1;
    private int scroll;
    private CompoundTag roundTag = new CompoundTag();
    private RouletteRound round;
    private int fieldLeft = 31;
    private int cellWidth = 21;
    private int sidebar = 298;

    public RouletteScreen(RouletteMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    protected void init() {
        imageWidth = Math.min(WIDTH, Math.max(300, width - 8));
        cellWidth = imageWidth < WIDTH ? 16 : 21;
        fieldLeft = imageWidth < WIDTH ? 27 : 31;
        sidebar = fieldLeft + cellWidth * 12 + 15;
        super.init();
        cells.clear();
        denominations.clear();
        cell(RouletteBet.Type.NUMBER, 0, Component.literal("0"), 10, GRID_Y, fieldLeft - 10, 54, CELL_ZERO);
        for (int number = 1; number <= 36; number++) {
            int column = (number - 1) / 3;
            int row = 2 - (number - 1) % 3;
            cell(RouletteBet.Type.NUMBER, number, Component.literal(Integer.toString(number)),
                    fieldLeft + column * cellWidth, GRID_Y + row * 18, cellWidth, 18,
                    RouletteLogic.isRed(number) ? CELL_RED : CELL_BLACK);
        }
        for (int dozen = 1; dozen <= 3; dozen++) {
            cell(RouletteBet.Type.DOZEN, dozen, Component.literal((dozen * 12 - 11) + "–" + dozen * 12),
                    fieldLeft + (dozen - 1) * cellWidth * 4, DOZEN_Y, cellWidth * 4, ROW_HEIGHT, CELL_FELT);
        }
        RouletteBet.Type[] outside = {RouletteBet.Type.LOW, RouletteBet.Type.EVEN, RouletteBet.Type.RED,
                RouletteBet.Type.BLACK, RouletteBet.Type.ODD, RouletteBet.Type.HIGH};
        for (int index = 0; index < outside.length; index++) {
            var type = outside[index];
            var sprite = type == RouletteBet.Type.RED ? CELL_RED
                    : type == RouletteBet.Type.BLACK ? CELL_BLACK : CELL_FELT;
            cell(type, 0, label(type, 0), fieldLeft + index * cellWidth * 2, EVEN_Y, cellWidth * 2, ROW_HEIGHT,
                    sprite);
        }
        for (int index = 0; index < CHIPS.length; index++) {
            int chips = CHIPS[index];
            var button = addButton(Component.literal(Integer.toString(chips)),
                    fieldLeft + index * cellWidth * 2, 150, cellWidth * 2, 18,
                    () -> stake = chips);
            denominations.add(button);
        }
    }

    /** A box of the layout. Each covers its slot but the top left pixel line, where the cream field shows. */
    private void cell(RouletteBet.Type type, int selection, Component label, int x, int y, int width, int height,
                      ResourceLocation sprite) {
        var button = new Button(leftPos + x, topPos + y, width, height, label,
                pressed -> PacketDistributor.sendToServer(
                        new RouletteBetRequest(menu.containerId, type.ordinal(), selection, stake)),
                supplier -> supplier.get()) {
            @Override
            protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                g.blitSprite(sprite, getX() + 1, getY() + 1, getWidth() - 1, getHeight() - 1);
                if (isHoveredOrFocused() && active) {
                    g.blitSprite(CELL_HOVER, getX() + 1, getY() + 1, getWidth() - 1, getHeight() - 1);
                }
                int textColor = active ? CasinoGui.CREAM : CasinoGui.MUTED;
                g.drawCenteredString(font, getMessage(), getX() + getWidth() / 2,
                        getY() + (getHeight() - font.lineHeight) / 2 + 1, textColor);
                boolean placed = round != null && round.snapshot().bets().stream()
                        .anyMatch(bet -> bet.bet().type() == type && bet.bet().selection() == selection);
                if (placed) {
                    g.blitSprite(CHIP_MARKER, getX() + getWidth() - MARKER_SIZE - 1, getY() + 2, MARKER_SIZE,
                            MARKER_SIZE);
                }
            }
        };
        button.setTooltip(Tooltip.create(CasinoText.gui("roulette.bet_hint", label(type, selection),
                type.returnMultiplier())));
        cells.add(addRenderableWidget(button));
    }

    public static Component label(RouletteBet.Type type, int selection) {
        return CasinoText.gui("roulette.type." + type.name().toLowerCase(Locale.ROOT), selection);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        var state = menu.state();
        if (state == null) { return; }
        if (!roundTag.equals(state.round())) {
            roundTag = state.round().copy();
            round = roundTag.isEmpty() ? null : RouletteRoundNbt.load(roundTag);
            scroll = Math.min(scroll, Math.max(0, round == null ? 0 : round.snapshot().bets().size() - 3));
        }
        var phase = round == null ? RouletteRound.Phase.IDLE : round.snapshot().phase();
        boolean betting = phase == RouletteRound.Phase.IDLE || phase == RouletteRound.Phase.BETTING
                || phase == RouletteRound.Phase.COMPLETE || phase == RouletteRound.Phase.CANCELLED;
        boolean beforeDeadline = phase != RouletteRound.Phase.BETTING
                || minecraft.level.getGameTime() < round.snapshot().deadline();
        cells.forEach(button -> button.active = betting && beforeDeadline && state.link().equals("READY")
                && state.balance() >= stake && stake <= state.maxBet());
        for (int index = 0; index < denominations.size(); index++) {
            denominations.get(index).setSelected(CHIPS[index] == stake);
            denominations.get(index).active = CHIPS[index] <= state.maxBet();
        }
    }

    @Override
    protected void renderPanels(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The cream lines of the layout: around 0 and the numbers, then under the numbers only.
        int right = leftPos + fieldLeft + 12 * cellWidth + 1;
        graphics.blitSprite(FIELD, leftPos + 10, topPos + GRID_Y, right - leftPos - 10, 3 * ROW_HEIGHT + 1);
        graphics.blitSprite(FIELD, leftPos + fieldLeft, topPos + DOZEN_Y, right - leftPos - fieldLeft,
                EVEN_Y + ROW_HEIGHT + 1 - DOZEN_Y);
        graphics.blitSprite(CasinoGui.INSET, leftPos + 10, topPos + 180, imageWidth - 20, 35);
        renderResult(graphics);
    }

    /** The last number on a disc of its pocket's colour; an empty disc before the first result. */
    private void renderResult(GuiGraphics graphics) {
        var phase = round == null ? RouletteRound.Phase.IDLE : round.snapshot().phase();
        int number = round == null || phase == RouletteRound.Phase.BETTING || phase == RouletteRound.Phase.SPINNING
                ? -1 : round.snapshot().number();
        ResourceLocation disc = number < 0 ? RESULT_NONE : number == 0 ? RESULT_ZERO
                : RouletteLogic.isRed(number) ? RESULT_RED : RESULT_BLACK;
        int x = leftPos + sidebar + 4;
        int y = topPos + GRID_Y + 12;
        graphics.blitSprite(disc, x, y, RESULT_SIZE, RESULT_SIZE);
        if (number >= 0) {
            graphics.pose().pushPose();
            graphics.pose().translate(x + RESULT_SIZE / 2f, y + RESULT_SIZE / 2f - font.lineHeight + 1, 0);
            graphics.pose().scale(2, 2, 1);
            graphics.drawCenteredString(font, Integer.toString(number), 0, 0, CasinoGui.CREAM);
            graphics.pose().popPose();
        }
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        var state = menu.state();
        renderBalance(graphics, state == null ? 0 : state.balance());
        var phase = round == null ? RouletteRound.Phase.IDLE : round.snapshot().phase();
        Component status = CasinoText.gui("roulette.phase." + phase.name().toLowerCase(Locale.ROOT));
        if (state != null && !state.link().equals("READY")) {
            status = CasinoText.message(state.link().equals("NOT_LINKED") ? "not_linked" : "vault_unavailable");
        }
        graphics.drawString(font, fitText(status.getString(), imageWidth - 20), MARGIN, 27, CasinoGui.CREAM, false);
        if (round != null && phase != RouletteRound.Phase.COMPLETE && phase != RouletteRound.Phase.CANCELLED) {
            long seconds = Math.max(0, round.snapshot().deadline() - minecraft.level.getGameTime() + 19) / 20;
            graphics.drawString(font, CasinoText.gui("roulette.timer", seconds), imageWidth - 80, 27, CasinoGui.GOLD,
                    false);
        }
        graphics.drawString(font, CasinoText.gui("roulette.result"), sidebar, GRID_Y, CasinoGui.MUTED, false);
        graphics.drawString(font, CasinoText.gui("roulette.stake"), sidebar, 105, CasinoGui.MUTED, false);
        graphics.drawString(font, Integer.toString(stake), sidebar, 119, CasinoGui.GOLD, false);
        graphics.drawString(font, CasinoText.gui("roulette.denomination"), 10, 140, CasinoGui.MUTED, false);
        graphics.drawString(font, CasinoText.gui("roulette.shared_bets"), 10, 170, CasinoGui.GOLD, false);
        if (round == null || round.snapshot().bets().isEmpty()) {
            graphics.drawString(font, CasinoText.gui("roulette.no_bets"), 14, 185, CasinoGui.MUTED, false);
            return;
        }
        var bets = round.snapshot().bets();
        for (int row = 0; row < 3 && row + scroll < bets.size(); row++) {
            var placed = bets.get(bets.size() - 1 - row - scroll);
            String name = state.names().getString(placed.player().toString());
            Component box = label(placed.bet().type(), placed.bet().selection());
            Component text = CasinoText.gui("roulette.bet_line", name, box,
                    CasinoText.chips(placed.bet().chips()));
            graphics.drawString(font, fitText(text.getString(), imageWidth - 30), 14, 184 + row * 10,
                    CasinoGui.CREAM, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (round != null && mouseY >= topPos + 180 && mouseY < topPos + 215) {
            int last = Math.max(0, round.snapshot().bets().size() - 3);
            scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, last);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
