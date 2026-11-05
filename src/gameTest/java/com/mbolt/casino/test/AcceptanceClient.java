package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.CasinoText;
import com.mbolt.casino.block.SlotMachineBlock;
import com.mbolt.casino.block.entity.CashierBlockEntity;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mbolt.casino.block.entity.VaultBlockEntity;
import com.mbolt.casino.client.ClientSlotEffects;
import com.mbolt.casino.client.screen.SlotMachineScreen;
import com.mbolt.casino.client.screen.VaultScreen;
import com.mbolt.casino.client.sound.ReelSpinSound;
import com.mbolt.casino.config.CasinoClientConfig;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.economy.VaultLedger;
import com.mbolt.casino.game.PendingPayouts;
import com.mbolt.casino.inventory.ChipWallet;
import com.mbolt.casino.menu.SlotMachineMenu;
import com.mbolt.casino.network.VaultState;
import com.mbolt.casino.registry.ModBlocks;
import com.mbolt.casino.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Real-client regressions for the acceptance fixes. Runs only in build/acceptance-client and is never shipped. */
@EventBusSubscriber(modid = CasinoMod.MODID, value = Dist.CLIENT)
public final class AcceptanceClient {
    private static final BlockPos VAULT = new BlockPos(-2, -60, 0);
    private static final BlockPos CASHIER = new BlockPos(0, -60, 0);
    private static final BlockPos MACHINE = new BlockPos(2, -60, 0);
    private static final String DIAMOND = "minecraft:diamond";
    private static final List<String> checks = new ArrayList<>();

    private enum Phase {
        START, SETUP, WAIT_SERVER, OPEN_JOURNAL, WAIT_JOURNAL, UPDATED_JOURNAL, JOURNAL_TOOLTIP,
        OPEN_SLOT, WAIT_NOTICE, NOTICE_TOOLTIP, PARTICLES_OFF, PARTICLES_ON, SPINNING, PAID, QUITTING, DONE
    }

    private static Phase phase = Phase.START;
    private static int ticks;
    private static int totalTicks;
    private static int hoverX = -1;
    private static int hoverY;
    private static boolean renderingTestHover;
    private static PlaySoundEvent playedSound;
    private static VaultState beforeJournalChange;
    private static long expectedEntryTime;
    private static long payout;
    private static int particlesBefore;
    private static String failure;

    private AcceptanceClient() {
    }

    private static boolean active() {
        return Boolean.getBoolean("boltcraft.acceptance");
    }

    @SubscribeEvent
    public static void sound(PlaySoundEvent event) {
        if (active()) {
            playedSound = event;
        }
    }

    /** Supplies a hover to the real screen renderer without moving the user's system cursor. */
    @SubscribeEvent
    public static void render(ScreenEvent.Render.Pre event) {
        if (active() && hoverX >= 0 && !renderingTestHover) {
            event.setCanceled(true);
            renderingTestHover = true;
            try {
                event.getScreen().renderWithTooltip(event.getGuiGraphics(), hoverX, hoverY, event.getPartialTick());
            } finally {
                renderingTestHover = false;
            }
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!active() || phase == Phase.DONE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            totalTicks++;
            if (totalTicks > 4000 && phase != Phase.QUITTING) {
                throw new IllegalStateException("Acceptance timeout in " + phase);
            }
            if (phase == Phase.START && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                if (!mc.options.languageCode.equals("ru_ru")) {
                    mc.options.languageCode = "ru_ru";
                    mc.getLanguageManager().setSelected("ru_ru");
                    mc.reloadResourcePacks();
                    return;
                }
                DevClient.prepareOptions(mc);
                mc.options.renderDistance().set(6);
                DevClient.createFlatWorld(mc, "BoltCraftAcceptance", GameType.SURVIVAL, false);
                moveTo(Phase.SETUP);
            }
            if (phase == Phase.QUITTING && mc.level == null && mc.getSingleplayerServer() == null
                    && mc.screen instanceof TitleScreen) {
                var path = mc.gameDirectory.toPath().resolve("../diagnostics/acceptance-client.txt").normalize();
                Files.createDirectories(path.getParent());
                String result = failure == null ? "PASS: " + checks.size() + " checks" : "FAIL: " + failure;
                Files.writeString(path, result + "\n" + String.join("\n", checks) + "\n");
                CasinoMod.LOGGER.info("CASINO ACCEPTANCE {}", result);
                phase = Phase.DONE;
                mc.stop();
                return;
            }
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null
                    || mc.getOverlay() != null) {
                return;
            }
            ticks++;
            mc.getToasts().clear();
            runPhase(mc);
        } catch (Exception error) {
            failure = error.toString();
            CasinoMod.LOGGER.error("Acceptance client failed in {}", phase, error);
            quit(mc);
        }
    }

    private static void runPhase(Minecraft mc) {
        switch (phase) {
            case SETUP -> {
                if (ticks > 40) {
                    onServer(mc, AcceptanceClient::setup, Phase.OPEN_JOURNAL);
                }
            }
            case OPEN_JOURNAL -> {
                if (ticks > 20 && mc.screen instanceof VaultScreen screen && screen.getMenu().state() != null) {
                    beforeJournalChange = screen.getMenu().state();
                    DevClient.press(mc, CasinoText.gui("tab_journal"), 1);
                    moveTo(Phase.WAIT_JOURNAL);
                }
            }
            case WAIT_JOURNAL -> {
                if (ticks > 20 && mc.screen instanceof VaultScreen screen && screen.displayedJournal() != null) {
                    require(screen.getMenu().state().journalSize() == 200, "the journal starts full");
                    onServer(mc, AcceptanceClient::changeFullJournal, Phase.UPDATED_JOURNAL);
                }
            }
            case UPDATED_JOURNAL -> {
                if (ticks > 20 && mc.screen instanceof VaultScreen screen && screen.displayedJournal() != null
                        && screen.displayedJournal().lines().getFirst().time() == expectedEntryTime) {
                    require(screen.getMenu().state().totals().equals(beforeJournalChange.totals()),
                            "journal refreshes while bank balances stay the same");
                    require(screen.getMenu().state().journalSize() == 200, "journal refreshes at its size limit");
                    DevClient.screenshot(mc, "journal_updated_at_200");
                    hoverX = (screen.width - 256) / 2 + 50;
                    hoverY = (screen.height - 226) / 2 + 72;
                    moveTo(Phase.JOURNAL_TOOLTIP);
                }
            }
            case JOURNAL_TOOLTIP -> {
                if (ticks > 6) {
                    DevClient.screenshot(mc, "journal_full_details");
                    hoverX = -1;
                    onServer(mc, server -> {
                        player(server).closeContainer();
                        DevClient.useBlock(player(server), MACHINE);
                    }, Phase.OPEN_SLOT);
                }
            }
            case OPEN_SLOT -> {
                if (ticks > 20 && mc.screen instanceof SlotMachineScreen screen && screen.getMenu().state() != null) {
                    DevClient.press(mc, Component.literal("50"), 1);
                    DevClient.press(mc, CasinoText.gui("spin"), 1);
                    moveTo(Phase.WAIT_NOTICE);
                }
            }
            case WAIT_NOTICE -> {
                if (ticks > 10 && mc.screen instanceof SlotMachineScreen screen && screen.getMenu().notice() != null) {
                    var notice = screen.getMenu().notice();
                    require(!notice.success(), "an uncovered bet is refused");
                    require(ChipWallet.balance(mc.player) == 100, "a refused bet does not take chips");
                    require(mc.font.split(notice.text(), 236).size() > 1, "the real refusal needs multiple lines");
                    DevClient.screenshot(mc, "notice_shortened");
                    hoverX = (screen.width - 256) / 2 + 80;
                    hoverY = (screen.height - 234) / 2 + 220;
                    moveTo(Phase.NOTICE_TOOLTIP);
                }
            }
            case NOTICE_TOOLTIP -> {
                if (ticks > 6) {
                    DevClient.screenshot(mc, "notice_full_details");
                    hoverX = -1;
                    checkSoundSettings(mc);
                    CasinoClientConfig.PARTICLES.set(false);
                    particlesBefore = Integer.parseInt(mc.particleEngine.countParticles());
                    ClientSlotEffects.pourWinnings(machine(mc), 0, true);
                    moveTo(Phase.PARTICLES_OFF);
                }
            }
            case PARTICLES_OFF -> {
                if (ticks > 4) {
                    require(Integer.parseInt(mc.particleEngine.countParticles()) == particlesBefore,
                            "disabled particles create no decorative chips or sparkles");
                    CasinoClientConfig.PARTICLES.set(true);
                    ClientSlotEffects.pourWinnings(machine(mc), 1, true);
                    moveTo(Phase.PARTICLES_ON);
                }
            }
            case PARTICLES_ON -> {
                if (ticks > 4) {
                    require(Integer.parseInt(mc.particleEngine.countParticles()) > particlesBefore,
                            "enabled particles reach the real particle engine");
                    require(ChipWallet.balance(mc.player) == 100, "visual settings never change chips");
                    DevClient.press(mc, Component.literal("1"), 1);
                    DevClient.press(mc, CasinoText.gui("spin"), 1);
                    moveTo(Phase.SPINNING);
                }
            }
            case SPINNING -> {
                if (ticks > 10 && machine(mc).spinning()) {
                    payout = machine(mc).payout();
                    require(payout == 2, "the controlled spin uses the server's chosen payout");
                    DevClient.screenshot(mc, "slot_spinning");
                    moveTo(Phase.PAID);
                }
            }
            case PAID -> {
                if (ticks > 70 && !machine(mc).spinning()) {
                    require(ChipWallet.balance(mc.player) == 99 + payout, "winning chips reach the client inventory");
                    require(mc.player.containerMenu instanceof SlotMachineMenu menu
                            && menu.notice() == null, "a successful spin clears the previous refusal");
                    DevClient.screenshot(mc, "slot_paid");
                    onServer(mc, server -> {
                        var vault = (VaultBlockEntity) server.overworld().getBlockEntity(VAULT);
                        require(vault.ledger().reserved() == 0, "the bank releases the spin reserve");
                        require(PendingPayouts.get(server).owedTo(player(server).getUUID()) == 0,
                                "delivered winnings are not still owed");
                    }, Phase.QUITTING);
                }
            }
            case QUITTING -> quit(mc);
            default -> { }
        }
    }

    private static void setup(MinecraftServer server) {
        var level = server.overworld();
        var player = player(server);
        DevClient.command(server, "gamerule doDaylightCycle false");
        DevClient.command(server, "time set noon");
        DevClient.command(server, "fill -6 -61 -4 6 -61 3 minecraft:dark_oak_planks");
        level.setBlock(VAULT, ModBlocks.VAULT.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(CASHIER, ModBlocks.CASHIER.get().defaultBlockState(), Block.UPDATE_ALL);
        var state = ModBlocks.SLOT_MACHINE.get().defaultBlockState();
        level.setBlock(MACHINE, state, Block.UPDATE_CLIENTS);
        level.setBlock(MACHINE.above(), state.setValue(SlotMachineBlock.HALF, DoubleBlockHalf.UPPER),
                Block.UPDATE_ALL);
        var vault = (VaultBlockEntity) level.getBlockEntity(VAULT);
        var cashier = (CashierBlockEntity) level.getBlockEntity(CASHIER);
        var machine = (SlotMachineBlockEntity) level.getBlockEntity(MACHINE);
        vault.setOwner(player);
        cashier.setOwner(player);
        cashier.linkTo(vault);
        machine.setOwner(player);
        machine.linkTo(vault);
        var rates = CasinoServerConfig.rates();
        vault.ledger().deposit(DIAMOND, 500, false, rates, player.getUUID(), 0);
        vault.ledger().deposit(DIAMOND, 10, true, rates, player.getUUID(), 1);
        for (int entry = 2; entry < VaultLedger.JOURNAL_LIMIT; entry++) {
            vault.ledger().deposit("minecraft:iron_ingot", 1, false, rates, player.getUUID(), entry);
        }
        vault.ledgerChanged();
        ChipWallet.give(player, 100);
        player.connection.teleport(0.5, -60, -2, 0, 10);
        DevClient.rigNextSpin(result -> result.payout() == 2);
        DevClient.useBlock(player, VAULT);
    }

    private static void changeFullJournal(MinecraftServer server) {
        var vault = (VaultBlockEntity) server.overworld().getBlockEntity(VAULT);
        var player = player(server);
        var rates = CasinoServerConfig.rates();
        long time = System.currentTimeMillis();
        vault.ledger().deposit("minecraft:iron_ingot", 1, false, rates, player.getUUID(), time);
        vault.ledgerChanged();
        vault.ledger().withdrawProfit("minecraft:iron_ingot", 1, rates, player.getUUID(), time + 1);
        vault.ledgerChanged();
        expectedEntryTime = time + 1;
    }

    private static void checkSoundSettings(Minecraft mc) {
        int oldVolume = CasinoClientConfig.SOUND_VOLUME.get();
        try {
            for (int percent : new int[]{100, 50, 0}) {
                CasinoClientConfig.SOUND_VOLUME.set(percent);
                SoundInstance sound = SimpleSoundInstance.forUI(ModSounds.LEVER_PULL.get(), 1.0f, 0.8f);
                mc.getSoundManager().play(sound);
                float expected = sound.getVolume() * percent / 100.0f;
                require(Math.abs(playedSound.getSound().getVolume() - expected) < 0.0001f,
                        "casino playback respects volume " + percent + "%");
            }
            CasinoClientConfig.SOUND_VOLUME.set(50);
            SoundInstance vanilla = SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f);
            mc.getSoundManager().play(vanilla);
            require(playedSound.getSound() == vanilla, "vanilla sounds are untouched");
            var original = new ReelSpinSound(machine(mc));
            mc.getSoundManager().play(original);
            SoundInstance wrapped = playedSound.getSound();
            require(wrapped instanceof TickableSoundInstance, "the reel loop keeps its tick lifecycle");
            CasinoClientConfig.SOUND_VOLUME.set(0);
            require(wrapped.getVolume() == 0, "an existing reel loop responds to muting");
            CasinoClientConfig.SOUND_VOLUME.set(100);
            require(wrapped.getVolume() == original.getVolume(), "the same loop responds to unmuting");
            var ticking = (TickableSoundInstance) wrapped;
            ticking.tick();
            require(ticking.isStopped(), "the wrapper stops when its original sound stops");
        } finally {
            CasinoClientConfig.SOUND_VOLUME.set(oldVolume);
        }
    }

    private static SlotMachineBlockEntity machine(Minecraft mc) {
        return (SlotMachineBlockEntity) mc.level.getBlockEntity(MACHINE);
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void onServer(Minecraft mc, Consumer<MinecraftServer> action, Phase next) {
        moveTo(Phase.WAIT_SERVER);
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            try {
                action.accept(server);
                mc.execute(() -> moveTo(next));
            } catch (Exception error) {
                mc.execute(() -> {
                    failure = error.toString();
                    CasinoMod.LOGGER.error("Acceptance server check failed", error);
                    quit(mc);
                });
            }
        });
    }

    private static void require(boolean condition, String check) {
        if (!condition) {
            throw new IllegalStateException(check);
        }
        checks.add("PASS " + check);
    }

    private static void moveTo(Phase next) {
        phase = next;
        ticks = 0;
    }

    private static void quit(Minecraft mc) {
        hoverX = -1;
        moveTo(Phase.QUITTING);
        if (mc.level != null) {
            mc.level.disconnect();
            mc.disconnect(new TitleScreen());
        }
    }
}
