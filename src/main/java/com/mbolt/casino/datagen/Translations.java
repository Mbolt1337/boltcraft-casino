package com.mbolt.casino.datagen;

import com.mbolt.casino.CasinoMod;

import java.util.ArrayList;
import java.util.List;

/**
 * Every player-facing string in English and Russian side by side, so the two languages cannot drift apart.
 * {@code %s} placeholders are filled in by the code that builds the component.
 */
final class Translations {
    private static final String ID = CasinoMod.MODID;

    private final List<Entry> entries = new ArrayList<>();

    private Translations() {
        chips();
        names();
        messages();
        screens();
        journal();
        sounds();
        config();
        roulette();
        decor();
        advancements();
        guide();
        message("seat_occupied", "This seat is occupied.", "Это место занято.");
        message("seat_blocked", "There is not enough room for your head or legs.",
                "Для головы или ног недостаточно места.");
        message("tray_full", "This tray already has four servings.", "На подносе уже четыре порции.");
        message("tray_empty_slot", "This spot on the tray is empty.", "В этом месте подноса ничего нет.");
        add("block." + ID + ".dining_chair", "Velvet dining chair", "Бархатный обеденный стул");
        add("block." + ID + ".serving_tray", "Serving tray", "Сервировочный поднос");
        add("block." + ID + ".table_lamp", "Velvet table lamp", "Настольная лампа с абажуром");
        add("block." + ID + ".bottle_shelf", "Bottle shelf", "Полка с бутылками");
        add("block." + ID + ".gilded_trim", "Gilded architectural trim", "Позолоченный архитектурный блок");
        add("block." + ID + ".casino_marquee", "CASINO Marquee Sign", "Вывеска CASINO с лампами");
        add("block." + ID + ".casino_marquee_small", "Small CASINO Marquee Sign", "Малая вывеска CASINO");
        add("block." + ID + ".giant_chip_red", "Giant Red Chip", "Большая красная фишка");
        add("block." + ID + ".giant_chip_blue", "Giant Blue Chip", "Большая синяя фишка");
        add("block." + ID + ".giant_chip_black", "Giant Black Chip", "Большая чёрная фишка");
        add("block." + ID + ".boltcraft_banner", "BoltCraft Banner", "Баннер BoltCraft");
        add("block." + ID + ".gilded_panel", "Gilded Red Panel", "Красная панель в позолоте");
        add("block." + ID + ".gilded_cornice", "Gilded Cornice", "Карниз с позолотой");
        add("block." + ID + ".coffered_ceiling", "Coffered Ceiling", "Кессонный потолок");
        add("block." + ID + ".carpet_stairs", "Carpeted Stairs", "Ступени с ковровой дорожкой");
        add("block." + ID + ".gilded_railing", "Gilded Railing", "Позолоченные перила");
        add("block." + ID + ".gilded_lantern", "Gilded Lantern", "Позолоченный фонарь");
        add("block." + ID + ".onyx_column", "Onyx Column", "Ониксовая колонна");
        add("item." + ID + ".ceramic_cup", "Ceramic cup", "Керамическая чашка");
        add("item." + ID + ".canape", "Casino canape", "Канапе казино");
        add("item." + ID + ".club_sandwich", "Club sandwich", "Клубный сэндвич");
        add("item." + ID + ".berry_tart", "Berry tart", "Ягодный тарт");
        add("item." + ID + ".chocolate_truffle", "Chocolate truffle", "Шоколадный трюфель");
        add("item." + ID + ".berry_fizz", "Berry fizz", "Ягодная газировка");
        add("item." + ID + ".spiced_cocoa", "Spiced cocoa", "Какао с пряностями");
    }

    static List<Entry> all() {
        return new Translations().entries;
    }

    /** Every chip count goes through {@link com.mbolt.casino.CasinoText#chips}, one key per plural form. */
    private void chips() {
        String prefix = "chips." + ID + ".";
        add(prefix + "single", "%s chip", "%s фишка");
        add(prefix + "ends_in_one", "%s chips", "%s фишка");
        add(prefix + "few", "%s chips", "%s фишки");
        add(prefix + "many", "%s chips", "%s фишек");
    }

    private void names() {
        add("itemGroup." + ID + ".games", "BoltCraft Casino: Games", "BoltCraft Casino: игры");
        add("block." + ID + ".vault", "Casino Vault", "Сейф казино");
        add("block." + ID + ".cashier", "Cashier", "Касса");
        add("block." + ID + ".slot_machine", "Slot Machine", "Игровой автомат");
        add("item." + ID + ".chip_1", "Chip (1)", "Фишка (1)");
        add("item." + ID + ".chip_10", "Chip (10)", "Фишка (10)");
        add("item." + ID + ".chip_100", "Chip (100)", "Фишка (100)");
        add("item." + ID + ".chip_1000", "Chip (1000)", "Фишка (1000)");
        add("item." + ID + ".chip.value", "Worth %s", "Номинал: %s");
        add("item." + ID + ".chip.value_stack", "Stack: %s", "В стопке: %s");
        add("item." + ID + ".manager_key", "Manager Key", "Ключ управляющего");
        add("item." + ID + ".guide_book", "BoltCraft Casino Guide", "Путеводитель BoltCraft Casino");
    }

    private void messages() {
        message("owner_only", "This belongs to %s.", "Это принадлежит игроку %s.");
        message("vault_unclaimed", "Nobody owns this vault. Claim it with a manager key.",
                "У сейфа нет хозяина. Займите его ключом управляющего.");
        message("vault_claimed", "The vault is yours now. The key remembers it.",
                "Теперь сейф ваш. Ключ его запомнил.");
        message("vault_selected", "The key remembers this vault. Now click a cashier or a game.",
                "Ключ запомнил сейф. Теперь кликните по кассе или игре.");
        message("key_no_vault", "Click your vault with the key first.", "Сначала кликните ключом по своему сейфу.");
        message("key_unsupported", "The key does not work on this block.", "Ключ не работает с этим блоком.");
        message("key_tooltip_empty", "Click a vault to remember it", "Кликните по сейфу, чтобы запомнить его");
        message("key_tooltip_vault", "Vault at %s, %s, %s", "Сейф: %s, %s, %s");
        message("linked", "Linked to the vault.", "Привязано к сейфу.");
        message("busy_relink", "Wait until the current round ends.", "Дождитесь конца текущего раунда.");
        message("not_linked", "Not linked to a vault yet. The owner links it with a manager key.",
                "Не привязано к сейфу. Владелец привязывает ключом управляющего.");
        message("vault_unavailable", "The linked vault is out of reach: it is unloaded or gone.",
                "Сейф недоступен: его чанк не загружен или сейфа больше нет.");
        message("vault_has_liabilities",
                "The vault still backs %s held by players. They must be cashed in first.",
                "Сейф обеспечивает %s на руках у игроков. Сначала их нужно обменять обратно.");
        message("games_disabled", "Games are switched off on this server.", "Игры на сервере выключены.");
        message("invalid_bet", "That bet is not offered here.", "Такой ставки здесь нет.");
        message("game_unclaimed", "Nobody runs this machine yet.", "У этого автомата пока нет хозяина.");
        message("slot_busy", "The reels are still spinning.", "Барабаны ещё крутятся.");
        message("slot_cooldown", "The machine is resetting: %s s.", "Автомат готовится: %s с.");
        message("not_enough_chips", "You need %s more.", "Не хватает: %s.");
        message("insufficient_reserve",
                "The casino cannot cover the top prize of this bet (%s). Try a smaller bet.",
                "Казино не покроет главный выигрыш этой ставки (%s). Попробуйте ставку меньше.");
        message("jackpot", "JACKPOT! %s won %s!", "ДЖЕКПОТ! %s выигрывает %s!");
        message("big_win", "%s won %s!", "%s выигрывает %s!");
        message("unknown_resource", "This cashier does not accept that.", "Касса это не принимает.");
        message("none_carried", "You carry no %s.", "У вас нет: %s.");
        message("not_enough_items", "You are %s short of %s.", "Не хватает %s шт.: %s.");
        message("transfer_limit", "At most %s items per exchange.", "За один раз не больше %s предметов.");
        message("vault_full", "The vault cannot store more of that.", "В сейфе больше нет места для этого.");
        message("exchanged_in", "Exchanged %s %s for %s.", "Обменяно: %s × %s → %s.");
        message("exchanged_out", "Paid %s for %s %s.", "Отдано %s → %s × %s.");
        message("not_in_vault", "The vault has no %s. Pick another resource.",
                "В сейфе нет: %s. Выберите другой ресурс.");
        message("vault_stock_low", "The vault holds only %s %s.", "В сейфе только %s шт.: %s.");
        message("cashier_reserve_low", "Those resources back other players' chips; the cashier cannot pay them out.",
                "Эти ресурсы обеспечивают чужие фишки, касса не может их выдать.");
        message("deposited", "Deposited %s %s as backing.", "Положено в обеспечение: %s × %s.");
        message("withdrawn", "Took %s %s of profit.", "Забрано прибыли: %s × %s.");
        message("profit_backs_chips", "Only %s %s are free profit; the rest backs chips.",
                "Свободной прибыли только %s шт.: %s. Остальное обеспечивает фишки.");
    }

    private void screens() {
        gui("balance", "Chips: %s", "Фишки: %s");
        gui("rate", "1 = %s", "1 = %s");
        gui("no_rate", "not accepted", "не принимается");
        gui("carried", "You: %s", "У вас: %s");
        gui("in_vault", "Vault: %s", "В сейфе: %s");
        gui("amount", "Amount", "Количество");
        gui("all", "All", "Все");
        gui("to_chips", "To chips", "В фишки");
        gui("to_resources", "To items", "В ресурсы");
        gui("fee", "Cash-out fee: %s%%", "Комиссия вывода: %s%%");
        gui("deposit", "Deposit", "Положить");
        gui("withdraw", "Profit", "Прибыль");
        gui("tab_resources", "Resources", "Ресурсы");
        gui("tab_journal", "Journal", "Журнал");
        gui("assets", "Assets: %s", "Активы: %s");
        gui("outstanding", "Chips out: %s", "Фишек на руках: %s");
        gui("free_reserve", "Free: %s", "Свободно: %s");
        gui("reserved", "In play: %s", "В игре: %s");
        gui("journal_empty", "No operations yet.", "Операций пока нет.");
        gui("spin", "SPIN", "КРУТИТЬ");
        gui("spinning", "SPINNING…", "КРУТИТСЯ…");
        gui("good_luck", "Good luck!", "Удачи!");
        gui("last_win", "You won %s!", "Выигрыш: %s!");
        gui("choose_bet", "Choose a bet and pull the lever", "Выберите ставку и крутите");
        gui("pay_table", "Three in a row, bets:", "Три в ряд, в ставках:");
        gui("two_cherries", "Exactly two cherries anywhere: ×%s", "Ровно две вишни в любом месте: ×%s");
    }

    /** Journal lines receive (count, item, chips). */
    /** Each journal entry has a title next to its time and a line of details under it. */
    private void journal() {
        journalEntry("deposit", "Deposit", "Обеспечение", "%1$s × %2$s", "%1$s × %2$s");
        journalEntry("withdraw", "Profit taken", "Прибыль", "%1$s × %2$s", "%1$s × %2$s");
        journalEntry("exchange_in", "Bought chips", "Обмен в фишки", "%1$s × %2$s → %3$s", "%1$s × %2$s → %3$s");
        journalEntry("exchange_out", "Cashed out", "Обмен в ресурсы", "%3$s → %1$s × %2$s", "%3$s → %1$s × %2$s");
        journalEntry("slot_bet", "Slot bet", "Ставка в автомате", "%3$s", "%3$s");
        journalEntry("slot_win", "Slot win", "Выигрыш в автомате", "%3$s", "%3$s");
        journalEntry("slot_refund", "Slot refund", "Возврат ставки", "%3$s", "%3$s");
    }

    private void journalEntry(String type, String titleEn, String titleRu, String detailsEn, String detailsRu) {
        gui("journal." + type, titleEn, titleRu);
        gui("journal." + type + ".details", detailsEn, detailsRu);
    }

    private void sounds() {
        String prefix = "subtitles." + ID + ".";
        add(prefix + "vault_open", "Vault opens", "Сейф открывается");
        add(prefix + "vault_close", "Vault closes", "Сейф закрывается");
        add(prefix + "chip_clink", "Chips clink", "Звон фишек");
        add(prefix + "lever_pull", "Lever pulled", "Рычаг опущен");
        add(prefix + "reel_spin", "Reels spin", "Барабаны крутятся");
        add(prefix + "reel_stop", "Reel stops", "Барабан остановился");
        add(prefix + "win_small", "Machine pays out", "Автомат платит");
        add(prefix + "win_big", "Big win", "Крупный выигрыш");
        add(prefix + "jackpot", "Jackpot!", "Джекпот!");
        add(prefix + "roulette_spin", "Roulette wheel spins", "Вращение рулетки");
        add(prefix + "ball_drop", "Roulette ball lands", "Шарик рулетки останавливается");
    }

    /** Names in NeoForge's config screen. */
    private void config() {
        config("general", "General", "Общие");
        config("enabled", "Games enabled", "Игры включены");
        config("protect_blocks", "Protect casino blocks", "Защита блоков казино");
        config("announce_big_win", "Announce wins from", "Объявлять выигрыши от");
        config("cashier", "Cashier", "Касса");
        config("rates", "Exchange rates", "Курсы обмена");
        config("cashout_fee_percent", "Cash-out fee, %", "Комиссия вывода, %");
        config("slot_machine", "Slot machine", "Игровой автомат");
        config("bets", "Bets", "Ставки");
        config("weights", "Reel weights", "Веса символов");
        config("payouts", "Payouts", "Выплаты");
        config("two_cherry_payout", "Two cherries payout", "Выплата за две вишни");
        config("cooldown_ticks", "Cooldown, ticks", "Пауза, тики");
        config("spin_duration_ticks", "Spin duration, ticks", "Длительность вращения, тики");
        config("sound_volume", "Casino sound volume, %", "Громкость казино, %");
        config("particles", "Payout chips and jackpot particles", "Фишки и частицы выигрышей");
    }

    private void message(String name, String english, String russian) {
        add("message." + ID + "." + name, english, russian);
    }

    private void roulette() {
        add("block." + ID + ".roulette_table", "Roulette table", "Стол рулетки");
        add("entity." + ID + ".croupier", "BoltCraft croupier", "Крупье BoltCraft");
        String croupier = "croupier." + ID + ".";
        add(croupier + "welcome", "Welcome to the table!", "Добро пожаловать за стол!");
        add(croupier + "bets_open", "Place your bets!", "Делайте ваши ставки!");
        add(croupier + "last_bets", "Last bets!", "Последние ставки!");
        add(croupier + "no_more_bets", "No more bets!", "Ставок больше нет!");
        add(croupier + "result_red", "%s, red!", "%s, красное!");
        add(croupier + "result_black", "%s, black!", "%s, чёрное!");
        add(croupier + "result_zero", "Zero!", "Зеро!");
        config("roulette", "Roulette", "Рулетка");
        config("betting_seconds", "Betting time, seconds", "Время ставок, секунд");
        config("max_bet", "Maximum stake", "Максимальная ставка");
        config("max_round_payout", "Maximum round return", "Предел выплат за раунд");
        config("sync_radius_blocks", "Animation radius", "Радиус анимации");
        config("guide", "Guide book", "Книга-путеводитель");
        config("give_guide_on_first_join", "Give the book on first join", "Выдавать книгу при первом входе");
        config("show_support_page", "Support page in the book", "Страница поддержки в книге");
        config("support_url", "Support link", "Ссылка поддержки");
        config("world", "World", "Мир");
        config("generate_structures", "Generate abandoned casinos", "Генерировать заброшенные казино");
        gui("roulette.type.number", "Number %s", "Число %s");
        gui("roulette.type.dozen", "Dozen %s", "Дюжина %s");
        gui("roulette.type.red", "Red", "Красн.");
        gui("roulette.type.black", "Black", "Чёрн.");
        gui("roulette.type.even", "Even", "Чёт");
        gui("roulette.type.odd", "Odd", "Нечёт");
        gui("roulette.type.low", "1–18", "1–18");
        gui("roulette.type.high", "19–36", "19–36");
        gui("roulette.phase.idle", "Place a bet to begin", "Сделайте первую ставку");
        gui("roulette.phase.betting", "Bets are open", "Приём ставок");
        gui("roulette.phase.spinning", "No more bets", "Ставки закрыты");
        gui("roulette.phase.payout", "Paying winnings", "Выплаты");
        gui("roulette.phase.complete", "Round complete · place a new bet", "Раунд завершён · ставьте снова");
        gui("roulette.phase.cancelled", "Round cancelled · stakes returned", "Раунд отменён · ставки возвращены");
        gui("roulette.timer", "%s s", "%s с");
        gui("roulette.result", "Result", "Результат");
        gui("roulette.stake", "Stake", "Ставка");
        gui("roulette.denomination", "Chips per click", "Фишек за клик");
        gui("roulette.shared_bets", "Table bets · scroll for more", "Ставки за столом · прокрутка списка");
        gui("roulette.no_bets", "Choose chips, then click a cell", "Выберите фишки и нажмите клетку");
        gui("roulette.bet_line", "%s · %s · %s", "%s · %s · %s");
        gui("roulette.bet_hint", "%s · returns ×%s, including your stake", "%s · выплата ×%s, включая ставку");
        message("roulette.closed", "Bets are closed for this round.", "Ставки в этом раунде уже закрыты.");
        message("roulette.duplicate", "This bet has already been accepted.", "Эта ставка уже принята.");
        message("roulette.too_many_bets", "This round has reached its bet count limit.", "Лимит числа ставок на раунд.");
        message("roulette.bet_limit", "The stake exceeds the server limit.", "Ставка превышает лимит сервера.");
        message("roulette.round_limit", "This bet exceeds the round payout limit.", "Превышен лимит выплат на раунд.");
        message("roulette.insufficient_chips", "You do not have enough chips.", "У вас недостаточно фишек.");
        message("roulette.insufficient_reserve", "The bank cannot cover this bet.", "В сейфе недостаточно покрытия ставки.");
        message("roulette.invalid_bet", "Invalid bet or cell.", "Недопустимая ставка или клетка.");
        message("roulette.big_win", "%s won %s at roulette!", "%s выигрывает в рулетку: %s!");
        journalEntry("roulette_bet", "Roulette bet", "Ставка в рулетке", "%3$s", "%3$s");
        journalEntry("roulette_win", "Roulette win", "Выигрыш в рулетке", "%3$s", "%3$s");
        journalEntry("roulette_refund", "Roulette refund", "Возврат рулетки", "%3$s", "%3$s");
    }

    private void decor() {
        add("itemGroup." + ID + ".decor", "BoltCraft Casino: Decor", "BoltCraft Casino: декор");
        gui("neon_picker", "Pick a character, or type it", "Выберите знак или наберите его");
        block("neon_letter", "Neon Letter", "Неоновая буква");
        block("neon_tube", "Neon Tube", "Неоновая трубка");
        String[][] signs = {{"chip", "Chip", "фишка"}, {"seven", "Seven", "семёрка"}, {"star", "Star", "звезда"},
                {"cards", "Cards", "карты"}, {"dice", "Dice", "кости"}, {"crown", "Crown", "корона"},
                {"cocktail", "Cocktail", "коктейль"}};
        for (String[] sign : signs) {
            block("neon_sign_" + sign[0], "Neon Sign: " + sign[1], "Неоновая вывеска: " + sign[2]);
        }
        for (String[] board : new String[][]{{"boltcraft", "BoltCraft"}, {"jackpot", "Jackpot"}, {"bar", "Bar"},
                {"vip", "VIP"}}) {
            block("neon_panel_" + board[0], "Neon Board: " + board[1], "Неоновое панно «" + board[1] + "»");
        }
        block("marquee_lights", "Marquee Lights", "Бегущие огни");
        String[][] carpets = {{"burgundy", "Burgundy Casino Carpet", "Бордовый ковёр казино", "бордового ковра"},
                {"navy", "Starry Navy Carpet", "Синий звёздный ковёр", "синего звёздного ковра"},
                {"felt", "Green Felt Carpet", "Ковёр из зелёного сукна", "зелёного сукна"},
                {"diamond", "Diamond Pattern Carpet", "Ковёр с ромбами", "ковра с ромбами"}};
        for (String[] carpet : carpets) {
            block("casino_carpet_" + carpet[0], carpet[1], carpet[2]);
            block("casino_carpet_" + carpet[0] + "_block", carpet[1] + " Block", "Блок " + carpet[3]);
        }
        String[][] ropes = {{"red", "Red", "Красный"}, {"blue", "Blue", "Синий"}, {"green", "Green", "Зелёный"},
                {"black", "Black", "Чёрный"}};
        for (String[] rope : ropes) {
            block("velvet_rope_" + rope[0], rope[1] + " Velvet Rope", rope[2] + " бархатный канат");
        }
        block("bar_counter", "Bar Counter", "Барная стойка");
        block("bar_stool", "Bar Stool", "Барный стул");
        block("marble_tiles", "Marble Tiles", "Мраморная плитка");
        block("marble_tile_stairs", "Marble Tile Stairs", "Ступени из мраморной плитки");
        block("marble_tile_slab", "Marble Tile Slab", "Плита из мраморной плитки");
        block("checkered_tiles", "Checkered Tiles", "Шахматная плитка");
        block("checkered_tile_stairs", "Checkered Tile Stairs", "Ступени из шахматной плитки");
        block("checkered_tile_slab", "Checkered Tile Slab", "Плита из шахматной плитки");
        block("velvet_wall_panel", "Velvet Wall Panel", "Бархатная стеновая панель");
        block("wood_wall_panel", "Wood Wall Panel", "Деревянная стеновая панель");
        block("gold_skirting", "Gilded Skirting", "Плинтус с позолотой");
        block("casino_column", "Marble Column", "Мраморная колонна");
        block("crystal_chandelier", "Crystal Chandelier", "Хрустальная люстра");
        block("grand_chandelier", "Grand Chandelier", "Большая люстра");
        block("wall_sconce", "Wall Sconce", "Бра");
        block("velvet_sofa", "Velvet Sofa", "Бархатный диван");
        block("coffee_table", "Coffee Table", "Журнальный столик");
        block("card_table", "Card Table", "Карточный стол");
        block("velvet_curtain", "Velvet Curtain", "Бархатный занавес");
        block("gold_planter", "Gold Planter", "Золотая кадка с пальмой");
    }

    private void advancements() {
        advancement("root", "BoltCraft Casino", "BoltCraft Casino",
                "Chips, games and neon. Played for chips only, never for real money",
                "Фишки, игры и неон. Только на игровые фишки, никаких реальных денег");
        advancement("first_chips", "Pocket Money", "Карманные деньги",
                "Exchange resources for chips at a cashier", "Обменяйте ресурсы на фишки в кассе");
        advancement("first_spin", "Pull the Lever", "Дёрнуть рычаг",
                "Play a slot machine", "Сыграйте на игровом автомате");
        advancement("jackpot", "Triple Seven", "Три семёрки",
                "Hit the jackpot on a slot machine", "Сорвите джекпот на игровом автомате");
        advancement("roulette_bet", "Place Your Bets", "Делайте ваши ставки",
                "Bet at a roulette table", "Сделайте ставку за столом рулетки");
        advancement("straight_up", "Straight Up", "Прямо в номер",
                "Win a bet on a single number", "Выиграйте ставку на одно число");
        advancement("zero", "Zero!", "Зеро!",
                "Win a bet on zero", "Выиграйте ставку на зеро");
        advancement("high_roller", "High Roller", "Хайроллер",
                "Win 1000 chips or more at once", "Выиграйте за раз 1000 фишек или больше");
        advancement("own_vault", "The House", "Своё заведение",
                "Own a casino vault", "Станьте хозяином сейфа казино");
        advancement("link", "Open for Business", "Открыто!",
                "Link a cashier or a game to your vault with the manager key",
                "Привяжите кассу или игру к своему сейфу ключом управляющего");
        advancement("claim_abandoned", "Under New Management", "Новый хозяин",
                "Claim the empty vault of an abandoned casino", "Займите пустой сейф заброшенного казино");
        advancement("neon_letter", "Bright Lights", "Огни большого города",
                "Put up a neon letter", "Повесьте неоновую букву");
        advancement("neon_board", "Name in Lights", "Имя в неоне",
                "Hang a neon board", "Повесьте неоновое панно");
    }

    /** The guide book, section by section; GuideContent decides where recipes, rates and tables go. */
    private void guide() {
        guide("support", "Support the author", "Поддержать автора",
                "Maxim makes BoltCraft Casino for the BoltCraft modpacks. If you enjoy it, you can support his "
                        + "work through the link below.",
                "BoltCraft Casino делает Максим для сборок BoltCraft. Если мод вам нравится, его работу можно "
                        + "поддержать по ссылке ниже.",
                "Support is voluntary. It gives no chips, no items and no advantage in the game.",
                "Поддержка добровольная. Она не даёт ни фишек, ни предметов, ни преимуществ в игре.");
        add("guide." + ID + ".support.link", "Open the link", "Открыть ссылку");
        guide("about", "About the mod", "О моде",
                "A casino that players build and run themselves. The owner sets up a vault, fills it with "
                        + "resources and opens a cashier and games. Guests change resources for chips, play, and "
                        + "change their winnings back.",
                "Казино, которое игроки строят и держат сами. Владелец ставит сейф, кладёт в него ресурсы и "
                        + "открывает кассу и игры. Гости меняют ресурсы на фишки, играют и меняют выигрыш обратно.",
                "Every chip in a pocket is backed by resources in a vault, so no casino can print money out of "
                        + "thin air.",
                "Каждая фишка на руках обеспечена ресурсами в сейфе, поэтому казино не может напечатать деньги "
                        + "из воздуха.",
                "Chips are play money only. Nothing won or lost here ever leaves the Minecraft world.",
                "Фишки — только игровая валюта. Всё, что здесь выигрывают и проигрывают, остаётся в мире "
                        + "Minecraft.");
        guide("cashier", "Chips and the cashier", "Фишки и касса",
                "The cashier changes resources for chips and back at the server's rates. The resources go into "
                        + "the vault the cashier is linked to.",
                "Касса меняет ресурсы на фишки и обратно по курсам сервера. Ресурсы уходят в сейф, к которому "
                        + "привязана касса.",
                "Rates on this server, for one item:",
                "Курсы на этом сервере, за один предмет:",
                "Chips cannot be crafted. They come as 1, 10, 100 and 1000, and the cashier gives change in the "
                        + "largest chips it can.",
                "Фишки не крафтятся. Номиналы — 1, 10, 100 и 1000; сдачу касса выдаёт самыми крупными "
                        + "фишками.");
        guide("vault", "The vault and the key", "Сейф и ключ",
                "The vault is the casino's bank: winnings are paid from the resources inside. Only its owner can "
                        + "open it.",
                "Сейф — банк казино: выигрыши выплачиваются из ресурсов в нём. Открыть его может только "
                        + "владелец.",
                "The manager key ties it all together: click your vault with it, then a cashier or a game.",
                "Ключ управляющего связывает всё вместе: кликните им по своему сейфу, а затем по кассе или "
                        + "игре.",
                "A vault cannot be broken while players still hold its chips. They have to be changed back first.",
                "Сейф нельзя сломать, пока у игроков на руках есть его фишки: сначала их нужно обменять "
                        + "обратно.");
        guide("slot_machine", "The slot machine", "Игровой автомат",
                "Pick a bet and pull the lever. Three of a kind pay by the table, and so do two cherries.",
                "Выберите ставку и дёрните рычаг. Три одинаковых символа платят по таблице, две вишни — тоже.",
                "Payouts on this server, in bets:",
                "Выплаты на этом сервере, в ставках:",
                "Over many spins the machine returns %s of the chips played.",
                "За много вращений автомат возвращает %s поставленных фишек.");
        guide("roulette", "Roulette", "Рулетка",
                "European roulette with 37 pockets, 0 to 36. Several players bet at one table until the croupier "
                        + "calls \"No more bets!\".",
                "Европейская рулетка на 37 лунок, от 0 до 36. За одним столом ставят несколько игроков, пока "
                        + "крупье не скажет «Ставок больше нет!».",
                "Payouts include the stake: a number ×36, a dozen ×3, red, black, even, odd, 1-18 and 19-36 ×2.",
                "Выплаты с учётом ставки: число — ×36, дюжина — ×3, красное, чёрное, чёт, нечет, 1–18 и "
                        + "19–36 — ×2.",
                "One bet is at most %s chips.",
                "Одна ставка — не больше %s фишек.");
        guide("decor", "Decor", "Декор",
                "Everything for your own hall: neon letters and signs, boards, marquee lights, carpets, tiles, "
                        + "columns, chandeliers, velvet furniture and a bar. It is cheap and waits in the "
                        + "\"BoltCraft Casino: Decor\" tab.",
                "Всё для своего зала: неоновые буквы и вывески, панно, бегущие огни, ковры, плитка, колонны, "
                        + "люстры, бархатная мебель и бар. Декор дешёвый и лежит во вкладке «BoltCraft Casino: "
                        + "декор».",
                "A neon letter asks for its character when it is placed; just type it. A dye changes the "
                        + "color of the neon.",
                "Неоновая буква спрашивает знак при установке — его можно просто набрать на клавиатуре. "
                        + "Краситель меняет цвет неона.",
                "Right-click a sofa, bar stool or dining chair with an empty main hand to sit. Press Shift to stand. "
                        + "Leave room in front of the seat for your legs.",
                "Нажмите ПКМ по дивану, барному или обеденному стулу с пустой основной рукой, чтобы сесть. "
                        + "Нажмите Shift, чтобы встать. Оставьте перед сиденьем место для ног.");
        guide("hospitality", "Food and service", "Еда и сервировка",
                "Make canapes, sandwiches, berry tarts, chocolate truffles, berry fizz and spiced cocoa. "
                        + "They restore food; drinks return an empty bottle or ceramic cup.",
                "Готовьте канапе, сэндвичи, ягодные тарталетки, шоколадные трюфели, ягодный шипучий напиток "
                        + "и пряное какао. Они насыщают; напитки возвращают пустую бутылку или керамическую чашку.",
                "Put a serving tray or lamp on a counter, card table or low coffee table. A tray holds four "
                        + "individual servings. Right-click with food to add one; use an empty hand to take one.",
                "Поставьте поднос или лампу на стойку, карточный или низкий журнальный столик. Поднос вмещает "
                        + "четыре порции. ПКМ с едой кладёт одну порцию, с пустой рукой — забирает одну.",
                "Shift-right-click a tray with an empty hand to pick it up with its servings dropped separately. "
                        + "The bar_menu item tag lets modpacks choose which foods can be served.",
                "Shift + ПКМ пустой рукой снимает поднос, его содержимое выпадает отдельно. Тег предметов "
                        + "bar_menu позволяет сборке выбрать продукты для сервировки.");
        guide("world", "Casinos in the world", "Казино в мире",
                "Abandoned casinos stand in plains, deserts, savannas and badlands: a small neon lounge, and "
                        + "more rarely a grand hall on two floors.",
                "В равнинах, пустынях, саваннах и пустошах стоят заброшенные казино: небольшой неоновый лаунж и "
                        + "реже — большой зал на два этажа.",
                "Their vaults are empty and belong to no one. Click one with a manager key and it is yours; "
                        + "then link the cashier and the machines, fill the vault, and the casino is open again.",
                "Их сейфы пусты и ничьи. Кликните по сейфу ключом управляющего — и он ваш; затем привяжите "
                        + "кассу и автоматы, наполните сейф, и казино снова работает.",
                "Their chests hold decor, gold and sometimes a manager key, but never chips.",
                "В их сундуках бывают декор, золото, иногда ключ управляющего — но никогда не фишки.");
        guide("authors", "Authors", "Авторы",
                "BoltCraft Casino is part of the BoltCraft modpack family. Idea and direction: Maxim "
                        + "(Mbolt1337).",
                "BoltCraft Casino — часть экосистемы сборок BoltCraft. Идея и руководство — Максим "
                        + "(Mbolt1337).",
                "Every model, texture and sound was made for this mod.",
                "Все модели, текстуры и звуки сделаны специально для этого мода.",
                "Thank you for playing!",
                "Спасибо, что играете!");
    }

    /** A guide section: its title and its paragraphs, English and Russian in turn. */
    private void guide(String section, String englishTitle, String russianTitle, String... paragraphs) {
        String prefix = "guide." + ID + "." + section;
        add(prefix + ".title", englishTitle, russianTitle);
        for (int i = 0; i < paragraphs.length; i += 2) {
            add(prefix + "." + (i / 2 + 1), paragraphs[i], paragraphs[i + 1]);
        }
    }

    private void advancement(String name, String englishTitle, String russianTitle, String englishDescription,
                             String russianDescription) {
        String prefix = "advancements." + ID + "." + name;
        add(prefix + ".title", englishTitle, russianTitle);
        add(prefix + ".description", englishDescription, russianDescription);
    }

    private void block(String name, String english, String russian) {
        add("block." + ID + "." + name, english, russian);
    }

    private void gui(String name, String english, String russian) {
        add("gui." + ID + "." + name, english, russian);
    }

    private void config(String name, String english, String russian) {
        add(ID + ".configuration." + name, english, russian);
    }

    private void add(String key, String english, String russian) {
        entries.add(new Entry(key, english, russian));
    }

    record Entry(String key, String english, String russian) {
    }
}
