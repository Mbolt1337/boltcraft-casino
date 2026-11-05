package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.entity.CashierBlockEntity;
import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.entity.CroupierEntity;
import com.mbolt.casino.game.CasinoRandom;
import com.mbolt.casino.game.roulette.RouletteRound;
import com.mbolt.casino.network.RouletteBetRequest;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.phys.AABB;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.PendingPayouts;
import com.mbolt.casino.game.SlotRounds;
import com.mbolt.casino.game.slot.SlotMachineLogic;
import com.mbolt.casino.game.slot.SlotSymbol;
import com.mbolt.casino.inventory.ChipWallet;
import com.mbolt.casino.inventory.PlayerItems;
import com.mbolt.casino.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Development check, never shipped: two real clients on one networked world. {@code runClient -PnetworkCheck} is
 * the host: it opens a superflat world to the network on {@link #PORT}. {@code runClient2 -PnetworkCheck} is the
 * guest, {@code Player2}, who joins like any remote player, so everything it does crosses the network encoded.
 *
 * <p>The host builds a casino, tells the guest over system chat which buttons to press (see
 * {@link NetworkCheckGuest}) and checks the outcome on the server: exchanging resources, protection, a spin seen by
 * both players, a win paid after leaving mid-spin and everything surviving a reload of the world. Each check is
 * logged as {@code [network check] PASS} or {@code FAIL} and summed up in {@code run-network/host/network-check.txt}.
 * Neither player is an operator, so the protection rules apply to both.
 */
@EventBusSubscriber(modid = CasinoMod.MODID, value = Dist.CLIENT)
public final class NetworkCheckHost {
    static final int PORT = 25570;
    static final String GUEST = "Player2";
    static final String COMMAND_PREFIX = "[network check] ";
    static final String REPORT_PREFIX = "[report] ";
    private static final String WORLD = "BoltCraftNetwork";
    private static final int FLOOR_Y = -61;
    static final BlockPos VAULT = new BlockPos(-2, FLOOR_Y + 1, 0);
    static final BlockPos CASHIER = new BlockPos(0, FLOOR_Y + 1, 0);
    static final BlockPos SLOT_MACHINE = new BlockPos(2, FLOOR_Y + 1, 0);
    static final BlockPos ROULETTE = new BlockPos(0, FLOOR_Y + 1, 3);
    private static volatile UUID rouletteRoundBeforeReload;
    private static final int GUEST_DIAMONDS = 10;
    private static final long GUEST_CHIPS = 100;
    private static final int BACKING_DIAMONDS = 300;

    private enum Phase { START, PLAYING, RELOADING }

    /**
     * A step runs {@code ticks} after the previous one. Steps that need the guest only count ticks while the guest
     * is online, so they wait for it to join or come back.
     */
    private record Step(String name, int ticks, boolean needsGuest, Consumer<Minecraft> action) {
        static Step onServer(String name, int ticks, boolean needsGuest, Consumer<MinecraftServer> action) {
            return new Step(name, ticks, needsGuest, mc -> {
                MinecraftServer server = mc.getSingleplayerServer();
                server.execute(() -> action.accept(server));
            });
        }

        static Step tellGuest(String name, int ticks, String command) {
            return onServer(name, ticks, true,
                    server -> guest(server).sendSystemMessage(Component.literal(COMMAND_PREFIX + command)));
        }
    }

    private static final List<Step> STEPS = steps();

    private static List<Step> steps() {
        if (Boolean.getBoolean("boltcraft.roulette_network")) {
            return rouletteSteps();
        }
        return List.of(
            Step.onServer("build the casino", 40, true, NetworkCheckHost::buildCasino),
            Step.onServer("guest opens the cashier", 20, true, server -> DevClient.useBlock(guest(server), CASHIER)),
            Step.tellGuest("guest changes every diamond into chips", 20, "press all 1"),
            Step.onServer("check", 20, true, server -> checkGuestWallet(server, GUEST_CHIPS, 0)),
            Step.tellGuest("guest changes every chip back", 0, "press all 2"),
            Step.onServer("check", 20, true, server -> checkGuestWallet(server, 0, GUEST_DIAMONDS)),
            Step.tellGuest("guest buys the chips again", 0, "press all 1"),
            Step.onServer("check", 20, true, server -> checkGuestWallet(server, GUEST_CHIPS, 0)),
            Step.tellGuest("guest closes the cashier", 0, "close"),
            Step.onServer("check protection", 10, true, NetworkCheckHost::checkProtection),
            Step.onServer("guest opens the slot machine", 10, true, NetworkCheckHost::openSlotForAKnownSpin),
            Step.tellGuest("guest pulls the lever", 20, "press spin 1"),
            new Step("host watches the reels", 25, true, NetworkCheckHost::watchReels),
            Step.tellGuest("guest photographs its screen", 0, "shot guest_spinning"),
            Step.onServer("server notes the reels", 60, true, server -> serverReels = describeMachine(server)),
            Step.tellGuest("guest reports its reels", 0, "report"),
            new Step("check that everyone saw the same reels", 20, true, NetworkCheckHost::checkSameReels),
            Step.onServer("rig a win", 0, true, NetworkCheckHost::rigWinForTheGuest),
            Step.tellGuest("guest pulls the lever again", 0, "press spin 1"),
            Step.tellGuest("guest leaves mid-spin", 10, "rejoin"),
            Step.onServer("check the win waits", 80, false, NetworkCheckHost::checkWinIsOwed),
            Step.onServer("check the win is paid on return", 60, true, NetworkCheckHost::checkWinWasPaid),
            Step.onServer("note the casino before the reload", 20, true, NetworkCheckHost::takeSnapshot),
            new Step("save the world and load it again", 10, false, NetworkCheckHost::reloadWorld),
            Step.onServer("check the casino after the reload", 60, true, NetworkCheckHost::checkSnapshot),
            Step.tellGuest("guest quits", 0, "quit"),
            new Step("finish", 40, false, NetworkCheckHost::finish));
    }

    private static List<Step> rouletteSteps() {
        return List.of(
                Step.onServer("build the casino", 40, true, NetworkCheckHost::buildCasino),
                Step.onServer("open the cashier", 20, true, server -> DevClient.useBlock(guest(server), CASHIER)),
                Step.tellGuest("buy chips", 20, "press all 1"),
                Step.onServer("check the guest balance", 20, true, server -> checkGuestWallet(server, 100, 0)),
                Step.tellGuest("close cashier", 0, "close"),
                Step.onServer("build roulette", 20, true, NetworkCheckHost::buildRoulette),
                Step.onServer("open both menus", 60, true, NetworkCheckHost::openRoulette),
                new Step("host bets on zero", 20, true, NetworkCheckHost::hostRouletteBet),
                Step.tellGuest("guest bets on zero", 0, "roulette_bet 0 5"),
                Step.onServer("check shared stakes", 30, true, NetworkCheckHost::checkRouletteStakes),
                Step.tellGuest("guest reports shared stakes", 0, "roulette_report"),
                new Step("check shared client state", 20, true, NetworkCheckHost::checkRouletteClients),
                Step.onServer("wait for chosen result", 370, true, NetworkCheckHost::rigNothing),
                Step.tellGuest("guest reports the result", 0, "roulette_report"),
                new Step("check spinning clients", 20, true, NetworkCheckHost::checkRouletteClients),
                Step.tellGuest("guest leaves before payout", 0, "rejoin"),
                Step.onServer("check roulette pending payout", 150, false, server -> {
                    long owed = PendingPayouts.get(server).owedTo(guestId);
                    check("roulette win waits for the offline guest", owed == 180, "owed " + owed);
                }),
                Step.onServer("check returned guest", 60, true, server -> checkRouletteWallets(server, 275)),
                Step.onServer("open a second roulette round", 70, true, NetworkCheckHost::openRoulette),
                new Step("host bets again", 20, true, NetworkCheckHost::hostRouletteBet),
                Step.tellGuest("guest bets again", 0, "roulette_bet 0 5"),
                Step.onServer("check second round", 30, true, NetworkCheckHost::checkRouletteStakes),
                Step.onServer("note the spinning round", 380, true, server -> {
                    var table = roulette(server);
                    rouletteRoundBeforeReload = table.roundId();
                    check("second round is spinning before restart",
                            table.round().snapshot().phase() == RouletteRound.Phase.SPINNING, describeRoulette(table));
                }),
                new Step("save and reload during roulette spin", 10, false, NetworkCheckHost::reloadWorld),
                Step.onServer("check roulette survived restart", 60, true, server -> {
                    var table = roulette(server);
                    check("restart preserves the chosen roulette round and zero",
                            rouletteRoundBeforeReload.equals(table.roundId()) && table.round().snapshot().number() == 0,
                            describeRoulette(table));
                    checkRouletteWallets(server, 450);
                    int count = server.overworld().getEntitiesOfClass(CroupierEntity.class, new AABB(ROULETTE).inflate(8)).size();
                    check("restart keeps exactly one croupier", count == 1, "croupiers " + count);
                }),
                Step.tellGuest("guest quits", 0, "quit"),
                new Step("finish", 40, false, NetworkCheckHost::finish));
    }

    private static volatile UUID guestId;

    private static void rigNothing(MinecraftServer server) { }

    private static void buildRoulette(MinecraftServer server) {
        var level = server.overworld();
        DevClient.command(server, "fill -6 -61 3 6 -61 7 minecraft:dark_oak_planks");
        var state = ModBlocks.ROULETTE_TABLE.get().defaultBlockState();
        level.setBlock(ROULETTE, state, 2);
        ModBlocks.ROULETTE_TABLE.get().setPlacedBy(level, ROULETTE, state, host(server), ItemStack.EMPTY);
        var vault = (VaultBlockEntity) level.getBlockEntity(VAULT);
        roulette(server).linkTo(vault);
        vault.ledger().deposit("minecraft:diamond", 10, true, CasinoServerConfig.rates(), host(server).getUUID(), 0);
        vault.ledgerChanged();
        ChipWallet.give(host(server), 100);
        guestId = guest(server).getUUID();
    }

    private static RouletteTableBlockEntity roulette(MinecraftServer server) {
        return (RouletteTableBlockEntity) server.overworld().getBlockEntity(ROULETTE);
    }

    private static void openRoulette(MinecraftServer server) {
        host(server).connection.teleport(2, -60, 2, 0, 15);
        guest(server).connection.teleport(-1, -60, 2, 0, 15);
        roulette(server).openMenu(host(server));
        roulette(server).openMenu(guest(server));
    }

    private static void hostRouletteBet(Minecraft mc) {
        PacketDistributor.sendToServer(new RouletteBetRequest(mc.player.containerMenu.containerId, 0, 0, 5));
    }

    private static void checkRouletteStakes(MinecraftServer server) {
        var table = roulette(server);
        check("two real players share two roulette stakes", table.round() != null
                && table.round().snapshot().bets().size() == 2, describeRoulette(table));
        long seed = 0;
        do { CasinoRandom.reseed(++seed); } while (CasinoRandom.nextInt(37) != 0);
        CasinoRandom.reseed(seed);
    }

    static String describeRoulette(RouletteTableBlockEntity table) {
        if (table.round() == null) { return "no round"; }
        var round = table.round().snapshot();
        return "roulette|" + round.phase() + "|" + round.number() + "|" + round.bets().size();
    }

    private static void checkRouletteClients(Minecraft mc) {
        var table = (RouletteTableBlockEntity) mc.level.getBlockEntity(ROULETTE);
        String host = table == null ? "no table" : describeRoulette(table);
        check("host and guest show the same roulette round", host.equals(guestReport),
                "host " + host + "; guest " + guestReport);
    }

    private static void checkRouletteWallets(MinecraftServer server, long expected) {
        long host = ChipWallet.balance(host(server));
        long guest = ChipWallet.balance(guest(server));
        check("both roulette players receive exactly one win", host == expected && guest == expected,
                "host " + host + "; guest " + guest + "; expected " + expected);
        check("roulette has no leftover reservation or guest debt", roulette(server).linkedVault().orElseThrow()
                .ledger().reserved() == 0 && PendingPayouts.get(server).owedTo(guestId) == 0, "settled");
    }

    private static final List<String> results = Collections.synchronizedList(new ArrayList<>());
    private static Phase phase = Phase.START;
    private static boolean published;
    private static int step;
    private static int stepTicks;

    // Written on the server thread, read by later steps.
    private static volatile String expectedReels = "";
    private static volatile String serverReels = "";
    private static volatile String hostReels = "";
    private static volatile String guestReport = "";
    private static volatile long expectedBalance;
    private static volatile long owedWin;
    private static volatile Snapshot snapshot;

    private NetworkCheckHost() {
    }

    private static boolean active() {
        return DevClient.networkRole().equals("host");
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!active()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean atTitle = mc.screen instanceof TitleScreen && mc.getOverlay() == null && mc.level == null;
        if (phase == Phase.START && atTitle) {
            DevClient.prepareOptions(mc);
            DevClient.createFlatWorld(mc, WORLD, GameType.CREATIVE, false);
            phase = Phase.PLAYING;
            return;
        }
        if (phase == Phase.RELOADING && atTitle) {
            mc.createWorldOpenFlows().openWorld(WORLD, () -> check("the world loads again", false, "load failed"));
            phase = Phase.PLAYING;
            return;
        }
        if (phase != Phase.PLAYING || mc.player == null || mc.getSingleplayerServer() == null
                || mc.getOverlay() != null || step >= STEPS.size()) {
            return;
        }
        if (!published) {
            published = true;
            var server = mc.getSingleplayerServer();
            server.setUsesAuthentication(false);
            check("the world opens to the network", server.publishServer(GameType.SURVIVAL, false, PORT),
                    "port " + PORT);
        }
        Step current = STEPS.get(step);
        boolean guestOnline = mc.getConnection() != null && mc.getConnection().getPlayerInfo(GUEST) != null;
        if (current.needsGuest() && !guestOnline) {
            return;
        }
        if (++stepTicks >= current.ticks()) {
            CasinoMod.LOGGER.info("Network check step: {}", current.name());
            stepTicks = 0;
            step++;
            current.action().accept(mc);
        }
    }

    /** The guest answers {@code report} in chat; the answer is kept here and not shown to anyone. */
    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        if (active() && event.getRawText().startsWith(REPORT_PREFIX)) {
            guestReport = event.getRawText().substring(REPORT_PREFIX.length());
            event.setCanceled(true);
        }
    }

    private static void check(String what, boolean passed, String detail) {
        String line = (passed ? "PASS " : "FAIL ") + what + " (" + detail + ")";
        results.add(line);
        CasinoMod.LOGGER.info("[network check] {}", line);
    }

    private static ServerPlayer guest(MinecraftServer server) {
        return server.getPlayerList().getPlayerByName(GUEST);
    }

    private static ServerPlayer host(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> !player.getGameProfile().getName().equals(GUEST))
                .findFirst().orElseThrow();
    }

    /** A vault backed by the host, and a cashier and a slot machine linked to it, all owned by the host. */
    private static void buildCasino(MinecraftServer server) {
        DevClient.command(server, "gamerule doDaylightCycle false");
        DevClient.command(server, "time set noon");
        DevClient.command(server, "fill -6 " + FLOOR_Y + " -6 6 " + FLOOR_Y + " 4 minecraft:dark_oak_planks");
        int y = FLOOR_Y + 1;
        DevClient.command(server, "setblock " + VAULT.getX() + " " + y + " 0 boltcraft_casino:vault[facing=north]");
        DevClient.command(server, "setblock " + CASHIER.getX() + " " + y
                + " 0 boltcraft_casino:cashier[facing=north]");
        DevClient.command(server, "setblock " + SLOT_MACHINE.getX() + " " + y
                + " 0 boltcraft_casino:slot_machine[facing=north,half=lower]");
        DevClient.command(server, "setblock " + SLOT_MACHINE.getX() + " " + (y + 1)
                + " 0 boltcraft_casino:slot_machine[facing=north,half=upper]");
        ServerPlayer host = host(server);
        ServerPlayer guest = guest(server);
        var level = server.overworld();
        if (level.getBlockEntity(VAULT) instanceof VaultBlockEntity vault
                && level.getBlockEntity(CASHIER) instanceof CashierBlockEntity cashier
                && level.getBlockEntity(SLOT_MACHINE) instanceof SlotMachineBlockEntity machine) {
            vault.setOwner(host);
            vault.ledger().deposit("minecraft:diamond", BACKING_DIAMONDS, false, CasinoServerConfig.rates(),
                    host.getUUID(), System.currentTimeMillis());
            vault.ledgerChanged();
            cashier.setOwner(host);
            cashier.linkTo(vault);
            machine.setOwner(host);
            machine.linkTo(vault);
        }
        guest.getInventory().clearContent();
        guest.getInventory().add(new ItemStack(Items.DIAMOND, GUEST_DIAMONDS));
        DevClient.command(server, "tp " + GUEST + " 0.5 " + y + " -1.5 0 20");
        DevClient.command(server, "tp " + host.getGameProfile().getName() + " 5.5 " + (y + 1) + " -4.5 35 15");
    }

    private static void checkGuestWallet(MinecraftServer server, long chips, long diamonds) {
        ServerPlayer guest = guest(server);
        long chipsHeld = ChipWallet.balance(guest);
        long diamondsHeld = PlayerItems.count(guest, Items.DIAMOND);
        check("guest holds " + chips + " chips and " + diamonds + " diamonds",
                chipsHeld == chips && diamondsHeld == diamonds, chipsHeld + " chips, " + diamondsHeld + " diamonds");
    }

    /** The guest is a stranger to the vault; the host owns it but its chips are in the guest's pocket. */
    private static void checkProtection(MinecraftServer server) {
        boolean brokenByGuest = guest(server).gameMode.destroyBlock(VAULT);
        check("a stranger cannot break the vault", !brokenByGuest && vaultStands(server), "broken " + brokenByGuest);
        boolean brokenByOwner = host(server).gameMode.destroyBlock(VAULT);
        check("the owner cannot break a vault whose chips are out", !brokenByOwner && vaultStands(server),
                "broken " + brokenByOwner);
    }

    private static boolean vaultStands(MinecraftServer server) {
        return server.overworld().getBlockState(VAULT).is(ModBlocks.VAULT.get());
    }

    private static void openSlotForAKnownSpin(MinecraftServer server) {
        SlotMachineLogic.Result result = DevClient.rigNextSpin(outcome -> outcome.payout() == 0);
        expectedReels = describe(false, new int[]{result.first(), result.second(), result.third()});
        DevClient.useBlock(guest(server), SLOT_MACHINE);
    }

    private static void watchReels(Minecraft mc) {
        boolean spinning = mc.level.getBlockEntity(SLOT_MACHINE) instanceof SlotMachineBlockEntity machine
                && machine.spinning();
        check("the host sees the guest's reels spin", spinning, "spinning " + spinning);
        DevClient.screenshot(mc, "host_watching");
        if (mc.level.getBlockEntity(SLOT_MACHINE) instanceof SlotMachineBlockEntity machine) {
            hostReels = describe(machine);
        }
    }

    private static void checkSameReels(Minecraft mc) {
        if (mc.level.getBlockEntity(SLOT_MACHINE) instanceof SlotMachineBlockEntity machine) {
            hostReels = describe(machine);
        }
        boolean same = expectedReels.equals(serverReels) && serverReels.equals(hostReels)
                && hostReels.equals(guestReport);
        check("server, host and guest show the reels that were chosen", same, "chosen " + expectedReels
                + "; server " + serverReels + "; host " + hostReels + "; guest " + guestReport);
    }

    private static void rigWinForTheGuest(MinecraftServer server) {
        SlotMachineLogic.Result result = DevClient.rigNextSpin(outcome -> outcome.payout() > 0
                && !SlotRounds.isJackpot(outcome));
        owedWin = result.payout();
        expectedBalance = ChipWallet.balance(guest(server)) - 1 + result.payout();
    }

    private static UUID guestId() {
        return UUIDUtil.createOfflinePlayerUUID(GUEST);
    }

    private static void checkWinIsOwed(MinecraftServer server) {
        long owed = PendingPayouts.get(server).owedTo(guestId());
        check("a win settled while the guest is away waits for them", guest(server) == null && owed == owedWin,
                "owed " + owed + " of " + owedWin);
    }

    private static void checkWinWasPaid(MinecraftServer server) {
        long balance = ChipWallet.balance(guest(server));
        long owed = PendingPayouts.get(server).owedTo(guestId());
        check("the win is paid once the guest is back", balance == expectedBalance && owed == 0,
                "balance " + balance + " of " + expectedBalance + ", still owed " + owed);
    }

    /** What must survive the reload: the vault's books, both links and the guest's chips. */
    private record Snapshot(Map<String, Long> resources, long outstanding, int journal, boolean cashierLinked,
                            boolean machineLinked, long guestChips) {
    }

    private static Snapshot capture(MinecraftServer server) {
        var level = server.overworld();
        if (!(level.getBlockEntity(VAULT) instanceof VaultBlockEntity vault)
                || !(level.getBlockEntity(CASHIER) instanceof CashierBlockEntity cashier)
                || !(level.getBlockEntity(SLOT_MACHINE) instanceof SlotMachineBlockEntity machine)) {
            return null;
        }
        return new Snapshot(Map.copyOf(vault.ledger().resources()), vault.ledger().outstanding(),
                vault.ledger().journal().size(), cashier.linkedVault().isPresent(),
                machine.linkedVault().isPresent(), ChipWallet.balance(guest(server)));
    }

    private static void takeSnapshot(MinecraftServer server) {
        snapshot = capture(server);
        check("the casino is complete before the reload", snapshot != null, String.valueOf(snapshot));
    }

    private static void reloadWorld(Minecraft mc) {
        mc.level.disconnect();
        mc.disconnect(new TitleScreen());
        published = false;
        phase = Phase.RELOADING;
    }

    private static void checkSnapshot(MinecraftServer server) {
        Snapshot after = capture(server);
        check("the casino is the same after the reload", snapshot != null && snapshot.equals(after),
                "before " + snapshot + "; after " + after);
    }

    private static void finish(Minecraft mc) {
        long failed = results.stream().filter(line -> line.startsWith("FAIL")).count();
        String summary = (results.size() - failed) + " passed, " + failed + " failed";
        CasinoMod.LOGGER.info("[network check] {}", summary);
        List<String> report = new ArrayList<>(results);
        report.add(summary);
        try {
            Files.write(mc.gameDirectory.toPath().resolve("network-check.txt"), report);
        } catch (IOException error) {
            CasinoMod.LOGGER.error("Cannot write the network check summary", error);
        }
        mc.stop();
    }

    private static String describeMachine(MinecraftServer server) {
        return server.overworld().getBlockEntity(SLOT_MACHINE) instanceof SlotMachineBlockEntity machine
                ? describe(machine) : "no machine";
    }

    /** One line both clients and the server can produce and compare: whether it spins, and the symbols. */
    static String describe(SlotMachineBlockEntity machine) {
        int[] symbols = new int[SlotMachineBlockEntity.REELS];
        Arrays.setAll(symbols, machine::symbol);
        return describe(machine.spinning(), symbols);
    }

    private static String describe(boolean spinning, int[] symbols) {
        String names = Arrays.stream(symbols).mapToObj(symbol -> SlotSymbol.values()[symbol].name())
                .collect(Collectors.joining(","));
        return (spinning ? "spinning " : "stopped ") + names;
    }
}
