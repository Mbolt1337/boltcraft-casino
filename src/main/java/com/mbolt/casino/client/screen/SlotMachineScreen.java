package com.mbolt.casino.client.screen;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.CasinoText;
import com.mbolt.casino.block.entity.LinkedBlockEntity.LinkStatus;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.game.slot.SlotSymbol;
import com.mbolt.casino.menu.SlotMachineMenu;
import com.mbolt.casino.network.SlotMachineState;
import com.mbolt.casino.network.SlotSpinRequest;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Bets, the spin button, the pay table and three reels that mirror the machine in the world. */
public final class SlotMachineScreen extends CasinoScreen<SlotMachineMenu> {
    private static final int WIDTH = 256;
    private static final int HEIGHT = 234;
    private static final ResourceLocation SYMBOLS = CasinoMod.id("textures/gui/slot_symbols.png");
    /** Gold frame with a dark back that the reels sit in. */
    private static final ResourceLocation BEZEL = CasinoMod.id("bezel");
    private static final ResourceLocation REEL = CasinoMod.id("reel");
    /** Translucent shade over the symbols where the reel curves away. */
    private static final ResourceLocation REEL_SHADE = CasinoMod.id("reel_shade");
    private static final ResourceLocation PAYLINE = CasinoMod.id("payline");
    private static final int SYMBOL_TEXTURE_SIZE = 16;
    private static final int SYMBOL_SCALE = 2;
    private static final int SYMBOL_SIZE = SYMBOL_TEXTURE_SIZE * SYMBOL_SCALE;
    private static final int BEZEL_BORDER = 4;
    private static final int REEL_TOP = 26;
    private static final int REEL_HEIGHT = 56;
    private static final int REEL_WIDTH = 46;
    private static final int REEL_GAP = 4;
    private static final int PAYLINE_HEIGHT = 5;
    private static final int STATUS_ROW = 92;
    private static final int BET_ROW = 104;
    private static final int BET_HEIGHT = 18;
    private static final int SPIN_ROW = 126;
    private static final int SPIN_HEIGHT = 22;
    private static final int DIVIDER_ROW = 152;
    private static final int TABLE_TOP = 157;
    private static final int TABLE_ROW_HEIGHT = 17;
    private static final int TABLE_SYMBOL_STEP = 13;

    private final List<ChipButton> betButtons = new ArrayList<>();
    private List<Integer> shownBets = List.of();
    private int selectedBet;
    private ChipButton spinButton;

    public SlotMachineScreen(SlotMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        betButtons.clear();
        shownBets = List.of();
        spinButton = addButton(CasinoText.gui("spin"), MARGIN, SPIN_ROW, WIDTH - 2 * MARGIN, SPIN_HEIGHT, this::spin);
    }

    private void spin() {
        if (selectedBet > 0) {
            PacketDistributor.sendToServer(new SlotSpinRequest(menu.containerId, menu.pos(), selectedBet));
        }
    }

    /** Rebuilds the bet buttons whenever the server's bet list changes. */
    private void layoutBets(List<Integer> bets) {
        betButtons.forEach(this::removeWidget);
        betButtons.clear();
        shownBets = bets;
        if (!bets.contains(selectedBet)) {
            selectedBet = bets.isEmpty() ? 0 : bets.getFirst();
        }
        int gap = 4;
        int width = bets.isEmpty() ? 0 : (WIDTH - 2 * MARGIN - gap * (bets.size() - 1)) / bets.size();
        for (int i = 0; i < bets.size(); i++) {
            int bet = bets.get(i);
            int x = MARGIN + i * (width + gap);
            betButtons.add(addButton(Component.literal(Integer.toString(bet)), x, BET_ROW, width, BET_HEIGHT,
                    () -> selectedBet = bet));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        SlotMachineState state = menu.state();
        if (state == null) {
            spinButton.active = false;
            return;
        }
        if (!shownBets.equals(state.bets())) {
            layoutBets(state.bets());
        }
        for (int i = 0; i < betButtons.size(); i++) {
            betButtons.get(i).setSelected(shownBets.get(i) == selectedBet);
        }
        SlotMachineBlockEntity machine = menu.blockEntity();
        boolean spinning = machine != null && machine.spinning();
        spinButton.active = state.status() == LinkStatus.READY && !spinning && state.balance() >= selectedBet;
        spinButton.setMessage(CasinoText.gui(spinning ? "spinning" : "spin"));
    }

    private static int reelsWidth() {
        return SlotMachineBlockEntity.REELS * REEL_WIDTH + (SlotMachineBlockEntity.REELS - 1) * REEL_GAP;
    }

    private int reelLeft(int reel) {
        return (WIDTH - reelsWidth()) / 2 + reel * (REEL_WIDTH + REEL_GAP);
    }

    @Override
    protected void renderPanels(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bezelLeft = leftPos + reelLeft(0) - BEZEL_BORDER;
        int bezelTop = topPos + REEL_TOP - BEZEL_BORDER;
        int bezelWidth = reelsWidth() + 2 * BEZEL_BORDER;
        graphics.blitSprite(BEZEL, bezelLeft, bezelTop, bezelWidth, REEL_HEIGHT + 2 * BEZEL_BORDER);
        SlotMachineBlockEntity machine = menu.blockEntity();
        for (int reel = 0; reel < SlotMachineBlockEntity.REELS; reel++) {
            int left = leftPos + reelLeft(reel);
            int top = topPos + REEL_TOP;
            graphics.blitSprite(REEL, left, top, REEL_WIDTH, REEL_HEIGHT);
            if (machine != null) {
                renderReel(graphics, machine, reel, left, top, partialTick);
            }
            // The shade is translucent; sprites only blend with what is under them while blending is on.
            RenderSystem.enableBlend();
            graphics.blitSprite(REEL_SHADE, left, top, REEL_WIDTH, REEL_HEIGHT);
            RenderSystem.disableBlend();
        }
        int lineTop = topPos + REEL_TOP + (REEL_HEIGHT - PAYLINE_HEIGHT) / 2;
        graphics.blitSprite(PAYLINE, bezelLeft + 1, lineTop, bezelWidth - 2, PAYLINE_HEIGHT);
        graphics.blitSprite(CasinoGui.DIVIDER, leftPos + MARGIN, topPos + DIVIDER_ROW, WIDTH - 2 * MARGIN, 2);
    }

    /** Symbols scroll downward as the reel turns; at rest the chosen symbol sits on the pay line. */
    private void renderReel(GuiGraphics graphics, SlotMachineBlockEntity machine, int reel, int left, int top,
                            float partialTick) {
        int symbols = SlotSymbol.values().length;
        double position = machine.reelAngle(reel, partialTick) / (2 * Math.PI / symbols);
        int centerY = top + REEL_HEIGHT / 2 - SYMBOL_SIZE / 2;
        int symbolX = left + (REEL_WIDTH - SYMBOL_SIZE) / 2;
        graphics.enableScissor(left, top, left + REEL_WIDTH, top + REEL_HEIGHT);
        int nearest = (int) Math.floor(position);
        for (int step = nearest - 1; step <= nearest + 2; step++) {
            int symbol = Math.floorMod(step, symbols);
            int y = centerY + (int) Math.round((position - step) * SYMBOL_SIZE);
            graphics.pose().pushPose();
            graphics.pose().translate(symbolX, y, 0);
            graphics.pose().scale(SYMBOL_SCALE, SYMBOL_SCALE, 1);
            renderSymbol(graphics, symbol, 0, 0);
            graphics.pose().popPose();
        }
        graphics.disableScissor();
    }

    private static void renderSymbol(GuiGraphics graphics, int symbol, int x, int y) {
        int count = SlotSymbol.values().length;
        graphics.blit(SYMBOLS, x, y, symbol * SYMBOL_TEXTURE_SIZE, 0, SYMBOL_TEXTURE_SIZE, SYMBOL_TEXTURE_SIZE,
                count * SYMBOL_TEXTURE_SIZE, SYMBOL_TEXTURE_SIZE);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        SlotMachineState state = menu.state();
        if (state == null) {
            return;
        }
        renderBalance(graphics, state.balance());
        graphics.drawCenteredString(font, statusLine(state), WIDTH / 2, STATUS_ROW, CasinoGui.CREAM);
        renderPayTable(graphics, state.payTable());
    }

    private Component statusLine(SlotMachineState state) {
        SlotMachineBlockEntity machine = menu.blockEntity();
        if (state.status() == LinkStatus.NOT_LINKED) {
            return CasinoText.message("not_linked");
        }
        if (state.status() == LinkStatus.VAULT_UNAVAILABLE) {
            return CasinoText.message("vault_unavailable");
        }
        if (machine != null && machine.spinning()) {
            return CasinoText.gui("good_luck");
        }
        if (machine != null && machine.payout() > 0) {
            return CasinoText.gui("last_win", CasinoText.chips(machine.payout()));
        }
        return CasinoText.gui("choose_bet");
    }

    /** Three of each symbol with its multiplier, two rows of three, then the two-cherries rule. */
    private void renderPayTable(GuiGraphics graphics, SlotMachineState.PayTable table) {
        graphics.drawString(font, CasinoText.gui("pay_table"), MARGIN, TABLE_TOP, CasinoGui.GOLD, false);
        int columnWidth = (WIDTH - 2 * MARGIN) / 3;
        SlotSymbol[] symbols = SlotSymbol.values();
        for (int index = 0; index < symbols.length && index < table.threeOfAKind().size(); index++) {
            int x = MARGIN + (index % 3) * columnWidth;
            int y = TABLE_TOP + 11 + (index / 3) * TABLE_ROW_HEIGHT;
            String multiplier = "×" + table.threeOfAKind().get(index);
            int spaceForSymbols = columnWidth - font.width(multiplier) - SYMBOL_TEXTURE_SIZE - 7;
            int symbolStep = Math.min(TABLE_SYMBOL_STEP, Math.max(4, spaceForSymbols / 2));
            for (int copy = 0; copy < 3; copy++) {
                renderSymbol(graphics, index, x + copy * symbolStep, y);
            }
            int textX = x + 2 * symbolStep + SYMBOL_TEXTURE_SIZE + 3;
            graphics.drawString(font, multiplier, textX, y + 4, CasinoGui.CREAM, false);
        }
        int y = TABLE_TOP + 11 + 2 * TABLE_ROW_HEIGHT + 2;
        graphics.drawString(font, CasinoText.gui("two_cherries", table.twoCherries()), MARGIN, y, CasinoGui.MUTED,
                false);
    }
}
