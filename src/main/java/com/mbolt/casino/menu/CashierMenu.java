package com.mbolt.casino.menu;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.CasinoText;
import com.mbolt.casino.advancement.CasinoEvent;
import com.mbolt.casino.block.entity.CashierBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.economy.ExchangeRates;
import com.mbolt.casino.economy.VaultLedger;
import com.mbolt.casino.inventory.ChipWallet;
import com.mbolt.casino.inventory.PlayerItems;
import com.mbolt.casino.inventory.ResourceItems;
import com.mbolt.casino.network.CashierExchange;
import com.mbolt.casino.network.CashierState;
import com.mbolt.casino.network.ResourceRow;
import com.mbolt.casino.registry.ModMenus;
import com.mbolt.casino.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Resources to chips and back at the rates from the server config, paid from the linked vault. */
public final class CashierMenu extends CasinoMenu<CashierBlockEntity, CashierState> {
    /** One exchange never moves more than 36 stacks; a player can repeat it. */
    public static final int MAX_TRANSFER_ITEMS = 2304;
    private static final long MAX_ISSUE_CHIPS = 1_000_000;

    public CashierMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.CASHIER.get(), containerId, inventory, pos, CashierBlockEntity.class, CashierState.class);
    }

    @Override
    protected CashierState buildState() {
        Optional<VaultBlockEntity> vault = blockEntity.linkedVault();
        List<ResourceRow> rows = new ArrayList<>();
        CasinoServerConfig.rates().entries().forEach((item, rate) -> {
            long carried = ResourceItems.item(item).map(found -> PlayerItems.count(player, found)).orElse(0L);
            long stored = vault.map(found -> found.ledger().count(item)).orElse(0L);
            rows.add(new ResourceRow(item, rate, carried, stored));
        });
        return new CashierState(containerId, blockEntity.linkStatus(), ChipWallet.balance(player),
                CasinoServerConfig.CASHOUT_FEE_PERCENT.get(), List.copyOf(rows));
    }

    public void exchange(CashierExchange request) {
        if (!tryStartAction() || request.amount() < 0) {
            return;
        }
        ServerPlayer server = (ServerPlayer) player;
        Optional<VaultBlockEntity> vault = blockEntity.linkedVault();
        Optional<Item> item = ResourceItems.item(request.item());
        ExchangeRates rates = CasinoServerConfig.rates();
        Component refusal;
        if (vault.isEmpty()) {
            refusal = CasinoText.message(blockEntity.isLinked() ? "vault_unavailable" : "not_linked");
        } else if (item.isEmpty() || rates.rate(request.item()) == 0) {
            refusal = CasinoText.message("unknown_resource");
        } else if (request.toChips()) {
            refusal = buyChips(server, vault.get(), item.get(), request.item(), request.amount(), rates);
        } else {
            refusal = sellChips(server, vault.get(), item.get(), request.item(), request.amount(), rates);
        }
        if (refusal != null) {
            sendNotice(refusal, false);
        } else if (request.toChips()) {
            CasinoEvent.FIRST_CHIPS.award(server);
        }
        sendStateNow();
    }

    @Nullable
    private Component buyChips(ServerPlayer server, VaultBlockEntity vault, Item item, String id, int requested,
                               ExchangeRates rates) {
        VaultLedger ledger = vault.ledger();
        long rate = rates.rate(id);
        long carried = PlayerItems.count(player, item);
        long room = VaultLedger.MAX_RESOURCE_COUNT - ledger.count(id);
        long amount = requested > 0 ? requested
                : Math.min(Math.min(carried, MAX_TRANSFER_ITEMS), Math.min(MAX_ISSUE_CHIPS / rate, room));
        Component name = item.getDescription();
        if (carried == 0) {
            return CasinoText.message("none_carried", name);
        }
        if (amount > carried) {
            return CasinoText.message("not_enough_items", amount - carried, name);
        }
        if (amount > MAX_TRANSFER_ITEMS || amount * rate > MAX_ISSUE_CHIPS) {
            return CasinoText.message("transfer_limit", MAX_TRANSFER_ITEMS);
        }
        if (amount > room || amount <= 0) {
            return CasinoText.message("vault_full");
        }
        long chips = rates.value(id, amount);
        ledger.deposit(id, amount, true, rates, player.getUUID(), System.currentTimeMillis());
        vault.ledgerChanged();
        PlayerItems.remove(player, item, amount);
        ChipWallet.give(server, chips);
        transactionDone(CasinoText.message("exchanged_in", amount, name, CasinoText.chips(chips)), id, amount);
        return null;
    }

    @Nullable
    private Component sellChips(ServerPlayer server, VaultBlockEntity vault, Item item, String id, int requested,
                                ExchangeRates rates) {
        VaultLedger ledger = vault.ledger();
        int fee = CasinoServerConfig.CASHOUT_FEE_PERCENT.get();
        long rate = rates.rate(id);
        long stock = ledger.count(id);
        long balance = ChipWallet.balance(player);
        Component name = item.getDescription();
        if (stock == 0) {
            return CasinoText.message("not_in_vault", name);
        }
        long amount = requested > 0 ? requested
                : Math.min(Math.min(stock, MAX_TRANSFER_ITEMS), affordable(balance, rate, fee));
        if (amount <= 0) {
            long oneItem = ExchangeRates.cashoutCost(rate, fee);
            return CasinoText.message("not_enough_chips", CasinoText.chips(oneItem - balance));
        }
        if (amount > MAX_TRANSFER_ITEMS) {
            return CasinoText.message("transfer_limit", MAX_TRANSFER_ITEMS);
        }
        if (amount > stock) {
            return CasinoText.message("vault_stock_low", stock, name);
        }
        long cost = ExchangeRates.cashoutCost(rates.value(id, amount), fee);
        if (cost > balance) {
            return CasinoText.message("not_enough_chips", CasinoText.chips(cost - balance));
        }
        if (!ledger.reserveCoversRedemption(id, amount, fee, rates)) {
            return CasinoText.message("cashier_reserve_low");
        }
        ledger.redeem(id, amount, fee, rates, player.getUUID(), System.currentTimeMillis());
        vault.ledgerChanged();
        ChipWallet.take(server, cost);
        PlayerItems.give(server, item, amount);
        transactionDone(CasinoText.message("exchanged_out", CasinoText.chips(cost), amount, name), id, amount);
        return null;
    }

    private void transactionDone(Component text, String item, long amount) {
        sendNotice(text, true);
        player.level().playSound(null, pos, ModSounds.CHIP_CLINK.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        CasinoMod.LOGGER.debug("Cashier {}: {} x {} by {}", pos, amount, item, player.getGameProfile().getName());
    }

    /** Largest item count whose cashout cost, fee included, the player can pay. */
    private static long affordable(long chips, long rate, int fee) {
        long low = 0;
        long high = Math.min(MAX_TRANSFER_ITEMS, chips / rate);
        while (low < high) {
            long middle = (low + high + 1) / 2;
            if (ExchangeRates.cashoutCost(middle * rate, fee) <= chips) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }
}
