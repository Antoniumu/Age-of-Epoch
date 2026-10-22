package com.robertx22.ancient_obelisks.main;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.util.UNICODE;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

public enum ObeliskWords implements ITranslated {
    MAP_ITEM_USE_INFO("Right Click the [Obelisk Block] with the map to start it."),
    CREATIVE_TAB("Ancient Obelisks"),
    NEW_MAP_GIVEN("The Obelisk gifts you with a Map. You can use this map to Activate the Obelisk."),
    NEW_WAVE_AFFIX("New Wave Affix: %1$s"),
    ITEM_WAVE_X("Wave %1$s"),
    RELIC_MAP(UNICODE.STAR + " Relic"),
    MAP_LOOT_BONUS("Loot Bonus: %1$s%%"),
    SPAWN_RATE("Mob Spawn Rate: %1$s%%"),
    OBELISK_END("The magic of the Obelisk fades"),
    OBELISK_TIER_X("Obelisk Tier: %1$s"),
    WAVE_X_STARTING("Wave %1$s Starting!"),
    RELIC_MAPS_ONLY("This Obelisk only Accepts Relic Maps"),
    LAST_WAVE("Last Wave INCOMING!");
    public String name;

    ObeliskWords(String name) {
        this.name = name;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return new TranslationBuilder(ObelisksMain.MODID).name(ExileTranslation.of(ObelisksMain.MODID + ".words." + this.name().toLowerCase(Locale.ROOT), name));
    }

    public MutableComponent get(Object... obj) {
        return getTranslation(TranslationType.NAME).getTranslatedName(obj);
    }

    @Override
    public String GUID() {
        return name();
    }
}
