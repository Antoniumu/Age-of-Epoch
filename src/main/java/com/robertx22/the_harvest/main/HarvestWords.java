package com.robertx22.the_harvest.main;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.util.UNICODE;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

public enum HarvestWords implements ITranslated {
    NEW_MAP_GIVEN("The Harvest gifts you with a Map. You can use this map to Activate the Harvest."),

    MAP_ITEM_USE_INFO("Right Click the [Harvest Block] with the map to start it."),
    CREATIVE_TAB("The Harvest"),
    RELIC_MAP(UNICODE.STAR + " Relic"),
    SPAWN_RATE("Mob Spawn Rate: %1$s%%"),
    HARVEST_TIME_REMAINING("%1$s Seconds Remaining"),
    HARVEST_END("The Vines appear to shrink, for now..."),
    RELIC_MAPS_ONLY("This Harvest only Accepts Relic Maps"),
    LAST_WAVE("Last Wave INCOMING!");
    public String name;

    HarvestWords(String name) {
        this.name = name;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return new TranslationBuilder(HarvestMain.MODID).name(ExileTranslation.of(HarvestMain.MODID + ".words." + this.name().toLowerCase(Locale.ROOT), name));
    }

    public MutableComponent get(Object... obj) {
        return getTranslation(TranslationType.NAME).getTranslatedName(obj);
    }

    @Override
    public String GUID() {
        return "";
    }
}
