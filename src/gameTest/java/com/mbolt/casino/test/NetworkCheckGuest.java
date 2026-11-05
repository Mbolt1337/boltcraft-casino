package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.CasinoText;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.network.RouletteBetRequest;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * The guest of the network check, see {@link NetworkCheckHost}. Keeps connecting to the host until told to quit,
 * and carries out the commands the host sends in system chat: {@code press <label> <n>}, {@code close},
 * {@code shot <name>}, {@code report}, {@code rejoin} and {@code quit}.
 */
@EventBusSubscriber(modid = CasinoMod.MODID, value = Dist.CLIENT)
public final class NetworkCheckGuest {
    /** Time between connection attempts, also how long the guest stays away after {@code rejoin}. */
    private static final int RECONNECT_TICKS = 200;
    private static final String ADDRESS = "localhost:" + NetworkCheckHost.PORT;

    /** Commands wait for the next tick, so that the guest never disconnects while handling a packet. */
    private static final Queue<String[]> commands = new ArrayDeque<>();
    private static boolean prepared;
    private static int offlineTicks;

    private NetworkCheckGuest() {
    }

    private static boolean active() {
        return DevClient.networkRole().equals("guest");
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!active()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            offlineTicks = 0;
            while (!commands.isEmpty()) {
                run(mc, commands.poll());
            }
            return;
        }
        if (mc.screen instanceof ConnectScreen || mc.getOverlay() != null) {
            return;
        }
        if (!prepared) {
            prepared = true;
            DevClient.prepareOptions(mc);
        }
        if (++offlineTicks >= RECONNECT_TICKS) {
            offlineTicks = 0;
            ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(ADDRESS),
                    new ServerData("BoltCraft network check", ADDRESS, ServerData.Type.OTHER), false, null);
        }
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent.System event) {
        String text = event.getMessage().getString();
        if (active() && text.startsWith(NetworkCheckHost.COMMAND_PREFIX)) {
            event.setCanceled(true);
            commands.add(text.substring(NetworkCheckHost.COMMAND_PREFIX.length()).split(" "));
        }
    }

    private static void run(Minecraft mc, String[] command) {
        CasinoMod.LOGGER.info("Network check guest: {}", String.join(" ", command));
        switch (command[0]) {
            case "press" -> DevClient.press(mc, CasinoText.gui(command[1]), Integer.parseInt(command[2]));
            case "close" -> mc.player.closeContainer();
            case "shot" -> DevClient.screenshot(mc, command[1]);
            case "report" -> report(mc);
            case "roulette_bet" -> PacketDistributor.sendToServer(new RouletteBetRequest(
                    mc.player.containerMenu.containerId, 0, Integer.parseInt(command[1]), Integer.parseInt(command[2])));
            case "roulette_report" -> {
                var table = (RouletteTableBlockEntity) mc.level.getBlockEntity(NetworkCheckHost.ROULETTE);
                String seen = table == null || table.round() == null ? "no roulette"
                        : NetworkCheckHost.describeRoulette(table);
                mc.player.connection.sendChat(NetworkCheckHost.REPORT_PREFIX + seen);
            }
            case "rejoin" -> {
                mc.level.disconnect();
                mc.disconnect(new TitleScreen());
            }
            case "quit" -> mc.stop();
            default -> CasinoMod.LOGGER.warn("Unknown network check command {}", String.join(" ", command));
        }
    }

    /** Tells the host, in chat, what this client's own copy of the slot machine shows. */
    private static void report(Minecraft mc) {
        String seen = mc.level.getBlockEntity(NetworkCheckHost.SLOT_MACHINE) instanceof SlotMachineBlockEntity machine
                ? NetworkCheckHost.describe(machine) : "no machine";
        mc.player.connection.sendChat(NetworkCheckHost.REPORT_PREFIX + seen);
    }
}
