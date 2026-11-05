package com.mbolt.casino.config;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.economy.ExchangeRates;
import com.mbolt.casino.game.slot.SlotMachineLogic;
import com.mbolt.casino.game.slot.SlotSymbol;
import com.mbolt.casino.game.roulette.RouletteLogic;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Server settings, synced to clients. Raw values are validated once on load into {@link #rates()},
 * {@link #slotLogic()} and {@link #slotBets()}; invalid entries are logged and skipped.
 */
public final class CasinoServerConfig {
    private static final int MAX_RATES = 256;
    private static final int MAX_BETS = 8;
    private static final int MAX_BET = 1000;
    private static final int MAX_TABLE_VALUE = 10_000;
    private static final List<Integer> DEFAULT_BETS = List.of(1, 5, 10, 50);

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue PROTECT_BLOCKS;
    public static final ModConfigSpec.IntValue ANNOUNCE_BIG_WIN;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RATES;
    public static final ModConfigSpec.IntValue CASHOUT_FEE_PERCENT;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SLOT_BETS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SLOT_WEIGHTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SLOT_PAYOUTS;
    public static final ModConfigSpec.IntValue SLOT_TWO_CHERRY_PAYOUT;
    public static final ModConfigSpec.IntValue SLOT_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue SLOT_SPIN_DURATION_TICKS;
    public static final ModConfigSpec.IntValue ROULETTE_BETTING_SECONDS;
    public static final ModConfigSpec.IntValue ROULETTE_MAX_BET;
    public static final ModConfigSpec.IntValue ROULETTE_MAX_ROUND_PAYOUT;
    public static final ModConfigSpec.IntValue ROULETTE_SYNC_RADIUS;
    public static final ModConfigSpec.BooleanValue GIVE_GUIDE_ON_FIRST_JOIN;
    public static final ModConfigSpec.BooleanValue SHOW_SUPPORT_PAGE;
    public static final ModConfigSpec.ConfigValue<String> SUPPORT_URL;
    public static final ModConfigSpec.BooleanValue GENERATE_STRUCTURES;

    private static volatile ExchangeRates rates = new ExchangeRates(Map.of());
    private static volatile SlotMachineLogic slotLogic = SlotMachineLogic.defaults();
    private static volatile List<Integer> slotBets = DEFAULT_BETS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder.comment("Enable casino games. Exchanging chips back stays available.")
                .define("enabled", true);
        PROTECT_BLOCKS = builder.comment("Only the owner and operators may break casino blocks.")
                .define("protect_blocks", true);
        ANNOUNCE_BIG_WIN = builder.comment("Wins of at least this many chips are announced in chat nearby.")
                .defineInRange("announce_big_win", 500, 1, 100_000_000);
        builder.pop();

        builder.push("cashier");
        RATES = builder.comment("Chips paid for one item, as \"namespace:item=chips\". Invalid entries are ignored.")
                .defineListAllowEmpty("rates", List.of(
                                "minecraft:iron_ingot=1",
                                "minecraft:gold_ingot=2",
                                "minecraft:emerald=5",
                                "minecraft:diamond=10",
                                "minecraft:netherite_ingot=100"),
                        () -> "minecraft:iron_ingot=1", value -> value instanceof String);
        CASHOUT_FEE_PERCENT = builder.comment("Extra percent charged in chips when cashing out, rounded up.")
                .defineInRange("cashout_fee_percent", 0, 0, 100);
        builder.pop();

        builder.push("slot_machine");
        SLOT_BETS = builder.comment("Bet buttons, 1-" + MAX_BET + ", at most " + MAX_BETS + ".")
                .defineListAllowEmpty("bets", DEFAULT_BETS, () -> 1, value -> value instanceof Integer);
        SLOT_WEIGHTS = builder.comment("Reel weight of each symbol, as \"symbol=weight\".")
                .defineListAllowEmpty("weights", symbolDefaults(true), () -> "cherry=10",
                        value -> value instanceof String);
        SLOT_PAYOUTS = builder.comment("Three-of-a-kind payout in bets, including the bet itself.")
                .defineListAllowEmpty("payouts", symbolDefaults(false), () -> "cherry=4",
                        value -> value instanceof String);
        SLOT_TWO_CHERRY_PAYOUT = builder.comment("Payout in bets for exactly two cherries anywhere.")
                .defineInRange("two_cherry_payout", 2, 0, MAX_TABLE_VALUE);
        SLOT_COOLDOWN_TICKS = builder.comment("Minimum ticks between two spins of one machine.")
                .defineInRange("cooldown_ticks", 60, 1, 1200);
        SLOT_SPIN_DURATION_TICKS = builder.comment("Ticks from pulling the lever to the payout.")
                .defineInRange("spin_duration_ticks", 60, 30, 200);
        builder.pop();

        builder.push("roulette");
        ROULETTE_BETTING_SECONDS = builder.defineInRange("betting_seconds", 20, 5, 120);
        ROULETTE_MAX_BET = builder.defineInRange("max_bet", 500, 1, 1_000_000);
        ROULETTE_MAX_ROUND_PAYOUT = builder.defineInRange("max_round_payout", 20_000, 36, 100_000_000);
        ROULETTE_SYNC_RADIUS = builder.defineInRange("sync_radius_blocks", 32, 16, 128);
        builder.pop();

        builder.push("guide");
        GIVE_GUIDE_ON_FIRST_JOIN = builder.comment("Give every player the guide book once, the first time they join.")
                .define("give_guide_on_first_join", true);
        SHOW_SUPPORT_PAGE = builder.comment("Open the guide book with the page about supporting the author.")
                .define("show_support_page", true);
        SUPPORT_URL = builder.comment("Link on the support page, http or https. Empty hides the page.")
                .define("support_url", "", value -> value instanceof String text
                        && (text.isEmpty() || text.startsWith("https://") || text.startsWith("http://")));
        builder.pop();

        builder.push("world");
        GENERATE_STRUCTURES = builder.comment("Generate abandoned casinos in new chunks. Where and how often is set by",
                        "the datapack: tags has_structure/* and the structure sets.")
                .define("generate_structures", true);
        builder.pop();
        SPEC = builder.build();
    }

    private CasinoServerConfig() {
    }

    public static ExchangeRates rates() {
        return rates;
    }

    public static SlotMachineLogic slotLogic() {
        return slotLogic;
    }

    public static List<Integer> slotBets() {
        return slotBets;
    }

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            refresh();
        }
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            refresh();
        }
    }

    private static void refresh() {
        rates = parseRates();
        slotLogic = parseSlotLogic();
        slotBets = parseBets();
        CasinoMod.LOGGER.info("Casino exchange: {} resources, cashout fee {}%",
                rates.entries().size(), CASHOUT_FEE_PERCENT.get());
        CasinoMod.LOGGER.info("Casino slot machine: RTP {}%, win chance {}%",
                percent(slotLogic.exactRtp()), percent(slotLogic.exactWinProbability()));
        CasinoMod.LOGGER.info("Casino roulette: European 0..36, RTP {}%", percent(RouletteLogic.RTP));
    }

    private static ExchangeRates parseRates() {
        var parsed = ExchangeRates.parse(RATES.get(),
                line -> CasinoMod.LOGGER.warn("Ignoring invalid casino rate: {}", line));
        Map<String, Long> accepted = new LinkedHashMap<>();
        parsed.entries().forEach((item, rate) -> {
            if (accepted.size() < MAX_RATES && isExchangeableItem(item)) {
                accepted.put(item, rate);
            } else {
                CasinoMod.LOGGER.warn("Ignoring unsupported casino currency: {}", item);
            }
        });
        return new ExchangeRates(accepted);
    }

    /** Chips themselves and air can never be exchanged; unknown items are ignored. */
    private static boolean isExchangeableItem(String item) {
        ResourceLocation id = ResourceLocation.parse(item);
        boolean isChip = id.getNamespace().equals(CasinoMod.MODID) && id.getPath().startsWith("chip_");
        return BuiltInRegistries.ITEM.containsKey(id) && !id.getPath().equals("air") && !isChip;
    }

    private static SlotMachineLogic parseSlotLogic() {
        int[] weights = symbolValues(SLOT_WEIGHTS.get(), true);
        int[] payouts = symbolValues(SLOT_PAYOUTS.get(), false);
        try {
            return new SlotMachineLogic(weights, payouts, SLOT_TWO_CHERRY_PAYOUT.get());
        } catch (IllegalArgumentException error) {
            CasinoMod.LOGGER.warn("Invalid slot table ({}); using the default weights and payouts", error.getMessage());
            return SlotMachineLogic.defaults();
        }
    }

    private static List<Integer> parseBets() {
        List<Integer> bets = SLOT_BETS.get().stream()
                .map(Integer.class::cast)
                .filter(bet -> bet > 0 && bet <= MAX_BET)
                .distinct()
                .sorted()
                .limit(MAX_BETS)
                .toList();
        return bets.isEmpty() ? DEFAULT_BETS : bets;
    }

    /** Starts from the defaults and applies every valid "symbol=value" entry over them. */
    private static int[] symbolValues(List<? extends String> entries, boolean weights) {
        SlotSymbol[] symbols = SlotSymbol.values();
        int[] values = Arrays.stream(symbols)
                .mapToInt(symbol -> weights ? symbol.defaultWeight() : symbol.defaultPayout())
                .toArray();
        Set<String> seen = new HashSet<>();
        for (String entry : entries) {
            String[] pair = entry.split("=", -1);
            Integer value = pair.length == 2 ? parseTableValue(pair[1]) : null;
            int index = pair.length == 2 ? symbolIndex(pair[0]) : -1;
            if (value == null || index < 0 || !seen.add(pair[0])) {
                CasinoMod.LOGGER.warn("Ignoring invalid slot setting: {}", entry);
                continue;
            }
            values[index] = value;
        }
        return values;
    }

    private static Integer parseTableValue(String text) {
        try {
            int value = Integer.parseInt(text);
            return value >= 0 && value <= MAX_TABLE_VALUE ? value : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private static int symbolIndex(String id) {
        for (SlotSymbol symbol : SlotSymbol.values()) {
            if (symbol.id().equals(id)) {
                return symbol.ordinal();
            }
        }
        return -1;
    }

    private static List<String> symbolDefaults(boolean weights) {
        return Arrays.stream(SlotSymbol.values())
                .map(symbol -> symbol.id() + "=" + (weights ? symbol.defaultWeight() : symbol.defaultPayout()))
                .toList();
    }

    private static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.3f", fraction * 100);
    }
}
