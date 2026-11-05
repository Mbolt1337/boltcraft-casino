package com.mbolt.casino.datagen;

import com.mbolt.casino.CasinoMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** Writes one language file from {@link Translations}. */
final class CasinoLanguage extends LanguageProvider {
    private final boolean russian;

    private CasinoLanguage(PackOutput output, String locale, boolean russian) {
        super(output, CasinoMod.MODID, locale);
        this.russian = russian;
    }

    static CasinoLanguage english(PackOutput output) {
        return new CasinoLanguage(output, "en_us", false);
    }

    static CasinoLanguage russian(PackOutput output) {
        return new CasinoLanguage(output, "ru_ru", true);
    }

    @Override
    protected void addTranslations() {
        for (Translations.Entry entry : Translations.all()) {
            add(entry.key(), russian ? entry.russian() : entry.english());
        }
    }
}
