package com.mbolt.casino.client;

import com.mbolt.casino.block.decor.NeonLetterBlock;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.client.screen.NeonGlyphScreen;
import com.mbolt.casino.client.screen.VaultScreen;
import com.mbolt.casino.menu.CasinoMenu;
import com.mbolt.casino.network.MenuNotice;
import com.mbolt.casino.network.SlotSpinResult;
import com.mbolt.casino.network.VaultJournalPage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

/** Applies server payloads on the client thread. */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void menuState(int containerId, Object state) {
        CasinoMenu<?, ?> menu = openMenu(containerId);
        if (menu != null) {
            menu.offerState(state);
        }
    }

    public static void notice(MenuNotice notice) {
        CasinoMenu<?, ?> menu = openMenu(notice.containerId());
        if (menu != null) {
            menu.acceptNotice(notice);
        }
    }

    public static void journalPage(VaultJournalPage page) {
        if (Minecraft.getInstance().screen instanceof VaultScreen screen
                && screen.getMenu().containerId == page.containerId()) {
            screen.showJournal(page);
        }
    }

    public static void spinResult(SlotSpinResult result) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(result.pos()) instanceof SlotMachineBlockEntity machine) {
            machine.applySpin(result);
        }
    }

    public static void openNeonPicker(BlockPos pos) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getBlockState(pos).getBlock() instanceof NeonLetterBlock) {
            Minecraft.getInstance().setScreen(new NeonGlyphScreen(pos));
        }
    }

    @Nullable
    private static CasinoMenu<?, ?> openMenu(int containerId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof CasinoMenu<?, ?> menu
                && menu.containerId == containerId) {
            return menu;
        }
        return null;
    }
}
