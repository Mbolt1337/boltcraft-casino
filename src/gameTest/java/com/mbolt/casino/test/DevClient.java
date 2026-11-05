package com.mbolt.casino.test;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.CasinoRandom;
import com.mbolt.casino.game.slot.SlotMachineLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * What the development runs that drive a real client have in common: {@link ArtPreview} and the network check
 * ({@link NetworkCheckHost}, {@link NetworkCheckGuest}). Never shipped.
 */
@EventBusSubscriber(modid = CasinoMod.MODID, value = Dist.CLIENT)
public final class DevClient {
    private DevClient() {
    }

    static boolean preview() {
        return Boolean.getBoolean("boltcraft.preview");
    }

    /** {@code host} or {@code guest} in the network check, empty otherwise. */
    static String networkRole() {
        return System.getProperty("boltcraft.network_check", "");
    }

    private static boolean anyRun() {
        return preview() || !networkRole().isEmpty() || Boolean.getBoolean("boltcraft.acceptance")
                || Boolean.getBoolean("boltcraft.roulette_check") || Boolean.getBoolean("boltcraft.furniture_check")
                || Boolean.getBoolean("boltcraft.worldgen_check") || Boolean.getBoolean("boltcraft.seating_check");
    }

    /** The first launch in a fresh game folder asks about accessibility; dev runs go straight to the title. */
    @SubscribeEvent
    public static void onScreen(ScreenEvent.Opening event) {
        boolean onboarding = event.getNewScreen() != null
                && event.getNewScreen().getClass().getSimpleName().equals("AccessibilityOnboardingScreen");
        if (anyRun() && onboarding) {
            event.setNewScreen(new TitleScreen());
        }
    }

    /** Keeps running in the background, silent, at a fixed GUI scale and field of view. */
    static void prepareOptions(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        mc.options.onboardAccessibility = false;
        mc.options.tutorialStep = TutorialSteps.NONE;
        mc.options.guiScale().set(3);
        mc.options.fov().set(60);
        mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
    }

    /**
     * Replaces any world of that name with a fresh superflat one. Without {@code commands} the player is not an
     * operator, which matters for the casino's protection rules.
     */
    static void createFlatWorld(Minecraft mc, String name, GameType mode, boolean commands) {
        createWorld(mc, name, mode, commands, true);
    }

    static void createNormalWorld(Minecraft mc, String name) {
        createWorld(mc, name, GameType.SPECTATOR, true, false);
    }

    private static void createWorld(Minecraft mc, String name, GameType mode, boolean commands, boolean flat) {
        Path saves = mc.gameDirectory.toPath().resolve("saves").toAbsolutePath().normalize();
        Path world = saves.resolve(name).normalize();
        if (!world.getParent().equals(saves)) {
            throw new IllegalArgumentException("Dev world must be directly inside " + saves);
        }
        deleteWorld(world, saves);
        var settings = new LevelSettings(name, mode, false, Difficulty.PEACEFUL, commands, new GameRules(),
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(731, !flat, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(flat ? WorldPresets.FLAT : WorldPresets.NORMAL)
                        .value().createWorldDimensions(), mc.screen);
    }

    private static void deleteWorld(Path world, Path saves) {
        if (!Files.exists(world)) {
            return;
        }
        try {
            if (!world.toRealPath().getParent().equals(saves.toRealPath())) {
                throw new IOException("Dev world resolves outside its saves folder: " + world);
            }
            try (Stream<Path> files = Files.walk(world)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(file);
                }
            }
        } catch (IOException error) {
            throw new IllegalStateException("Cannot clear the dev world " + world, error);
        }
    }

    static void command(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                command);
    }

    /** Right-clicks a block with an empty hand, which opens a casino block's screen. */
    static void useBlock(ServerPlayer player, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        player.serverLevel().getBlockState(pos).useWithoutItem(player.serverLevel(), player, hit);
    }

    /** Presses the {@code occurrence}-th button (from 1) with this label on the open screen. */
    static void press(Minecraft mc, Component label, int occurrence) {
        if (mc.screen == null) {
            return;
        }
        mc.screen.children().stream()
                .filter(child -> child instanceof AbstractButton button && button.getMessage().equals(label))
                .map(AbstractButton.class::cast)
                .skip(occurrence - 1L)
                .findFirst()
                .ifPresent(AbstractButton::onPress);
    }

    /** Moves the cursor to the window's corner, so no button is hovered and no tooltip covers a screen shot. */
    static void parkCursor(Minecraft mc) {
        GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
    }

    static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                message -> CasinoMod.LOGGER.info("Dev shot {}", name));
    }

    /**
     * Reseeds the game random with the first seed whose next one-chip spin passes {@code wanted}, so that the next
     * pull of a lever gives that outcome. Returns the outcome.
     */
    static SlotMachineLogic.Result rigNextSpin(Predicate<SlotMachineLogic.Result> wanted) {
        SlotMachineLogic logic = CasinoServerConfig.slotLogic();
        long seed = 0;
        SlotMachineLogic.Result result;
        do {
            CasinoRandom.reseed(++seed);
            result = logic.spin(1, CasinoRandom::nextInt);
        } while (!wanted.test(result));
        CasinoRandom.reseed(seed);
        return result;
    }
}
