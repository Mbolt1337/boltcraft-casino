package com.mbolt.casino.menu;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.CasinoText;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.economy.ExchangeRates;
import com.mbolt.casino.economy.VaultLedger;
import com.mbolt.casino.inventory.PlayerItems;
import com.mbolt.casino.inventory.ResourceItems;
import com.mbolt.casino.network.ResourceRow;
import com.mbolt.casino.network.VaultJournalPage;
import com.mbolt.casino.network.VaultJournalRequest;
import com.mbolt.casino.network.VaultState;
import com.mbolt.casino.network.VaultTransfer;
import com.mbolt.casino.registry.ModMenus;
import com.mbolt.casino.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The owner's view of the vault: balances, stored resources, backing deposits, profit and the journal. */
public final class VaultMenu extends CasinoMenu<VaultBlockEntity, VaultState> {
    private static final int MAX_TRANSFER_ITEMS = CashierMenu.MAX_TRANSFER_ITEMS;

    public VaultMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.VAULT.get(), containerId, inventory, pos, VaultBlockEntity.class, VaultState.class);
        if (!player.level().isClientSide && blockEntity != null && blockEntity.isOwner(player)) {
            blockEntity.startViewing();
        }
    }

    @Override
    protected boolean mayView(Player viewer) {
        return blockEntity != null && blockEntity.isOwner(viewer);
    }

    @Override
    public void removed(Player viewer) {
        super.removed(viewer);
        if (!viewer.level().isClientSide && blockEntity != null && blockEntity.isOwner(viewer)) {
            blockEntity.stopViewing();
        }
    }

    @Override
    protected VaultState buildState() {
        VaultLedger ledger = blockEntity.ledger();
        ExchangeRates rates = CasinoServerConfig.rates();
        // Resources whose rate was removed from the config still show up, valued at zero.
        Set<String> items = new LinkedHashSet<>(rates.entries().keySet());
        items.addAll(ledger.resources().keySet());
        List<ResourceRow> rows = new ArrayList<>();
        for (String item : items) {
            long carried = ResourceItems.item(item).map(found -> PlayerItems.count(player, found)).orElse(0L);
            rows.add(new ResourceRow(item, rates.rate(item), carried, ledger.count(item)));
        }
        var totals = new VaultState.Totals(ledger.assetValue(rates), ledger.outstanding(), ledger.reserved(),
                ledger.freeReserve(rates));
        return new VaultState(containerId, totals, List.copyOf(rows), ledger.journal().size(),
                blockEntity.ledgerRevision());
    }

    public void transfer(VaultTransfer request) {
        if (!tryStartAction() || request.amount() < 0) {
            return;
        }
        Optional<Item> item = ResourceItems.item(request.item());
        ExchangeRates rates = CasinoServerConfig.rates();
        Component refusal;
        if (item.isEmpty() || rates.rate(request.item()) == 0) {
            refusal = CasinoText.message("unknown_resource");
        } else if (request.deposit()) {
            refusal = deposit(item.get(), request.item(), request.amount(), rates);
        } else {
            refusal = withdraw(item.get(), request.item(), request.amount(), rates);
        }
        if (refusal != null) {
            sendNotice(refusal, false);
        }
        sendStateNow();
    }

    @Nullable
    private Component deposit(Item item, String id, int requested, ExchangeRates rates) {
        VaultLedger ledger = blockEntity.ledger();
        long carried = PlayerItems.count(player, item);
        long room = VaultLedger.MAX_RESOURCE_COUNT - ledger.count(id);
        long amount = requested > 0 ? requested : Math.min(Math.min(carried, MAX_TRANSFER_ITEMS), room);
        Component name = item.getDescription();
        if (carried == 0) {
            return CasinoText.message("none_carried", name);
        }
        if (amount > carried) {
            return CasinoText.message("not_enough_items", amount - carried, name);
        }
        if (amount > MAX_TRANSFER_ITEMS) {
            return CasinoText.message("transfer_limit", MAX_TRANSFER_ITEMS);
        }
        if (amount > room || amount <= 0) {
            return CasinoText.message("vault_full");
        }
        ledger.deposit(id, amount, false, rates, player.getUUID(), System.currentTimeMillis());
        blockEntity.ledgerChanged();
        PlayerItems.remove(player, item, amount);
        transactionDone(CasinoText.message("deposited", amount, name), id, amount);
        return null;
    }

    @Nullable
    private Component withdraw(Item item, String id, int requested, ExchangeRates rates) {
        VaultLedger ledger = blockEntity.ledger();
        long rate = rates.rate(id);
        long stock = ledger.count(id);
        long byReserve = Math.max(0, ledger.freeReserve(rates)) / rate;
        long amount = requested > 0 ? requested : Math.min(Math.min(stock, MAX_TRANSFER_ITEMS), byReserve);
        Component name = item.getDescription();
        if (stock == 0) {
            return CasinoText.message("not_in_vault", name);
        }
        if (amount > stock) {
            return CasinoText.message("vault_stock_low", stock, name);
        }
        if (amount > MAX_TRANSFER_ITEMS) {
            return CasinoText.message("transfer_limit", MAX_TRANSFER_ITEMS);
        }
        if (amount <= 0 || amount > byReserve) {
            return CasinoText.message("profit_backs_chips", byReserve, name);
        }
        ledger.withdrawProfit(id, amount, rates, player.getUUID(), System.currentTimeMillis());
        blockEntity.ledgerChanged();
        PlayerItems.give((ServerPlayer) player, item, amount);
        transactionDone(CasinoText.message("withdrawn", amount, name), id, amount);
        return null;
    }

    public void sendJournal(VaultJournalRequest request) {
        if (!(player instanceof ServerPlayer server) || !stillValid(player)) {
            return;
        }
        List<VaultLedger.Entry> entries = blockEntity.ledger().journal();
        int pageSize = VaultJournalPage.PAGE_SIZE;
        int pageCount = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        int page = Math.clamp(request.page(), 0, pageCount - 1);
        List<VaultJournalPage.Line> lines = new ArrayList<>();
        for (int offset = 0; offset < pageSize; offset++) {
            int index = entries.size() - 1 - page * pageSize - offset;
            if (index < 0) {
                break;
            }
            VaultLedger.Entry entry = entries.get(index);
            String name = PlayerNames.of(server.server, entry.player());
            lines.add(new VaultJournalPage.Line(entry.time(), name, entry.type(), entry.item(), entry.count(),
                    entry.value()));
        }
        PacketDistributor.sendToPlayer(server, new VaultJournalPage(containerId, page, pageCount, lines));
    }

    private void transactionDone(Component text, String item, long amount) {
        sendNotice(text, true);
        player.level().playSound(null, pos, ModSounds.CHIP_CLINK.get(), SoundSource.BLOCKS, 0.5f, 0.8f);
        CasinoMod.LOGGER.debug("Vault {}: {} x {} by {}", pos, amount, item, player.getGameProfile().getName());
    }
}
