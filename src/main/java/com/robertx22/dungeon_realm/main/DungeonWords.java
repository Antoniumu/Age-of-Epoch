package com.robertx22.dungeon_realm.main;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

public enum DungeonWords implements ITranslated {
    USABLE_ONLY_IN_DUNGEON_REALM("This is only usable in the Dungeon Realm"),
    NEED_HOME_PEARL("You need to have at least one %1$s in your inventory to enter."),
    HOME_PEARL_DESC("Use to Exit the Dungeon Realm"),
    MAP_HAS_UBER_ARENA("- { Area Contains an Uber Boss Portal } -"),
    MAP_COMPLETE_RARITY_UPGRADE("Your Map Exploration is now %1$s"),
    MAP_ITEM_DESC("This item allows you to enter the Dungeon Realm"),
    MAP_ITEM_USE_INFO("Right Click the [Map Device Block] with the map to start it."),
    RELIC_CONTAINER("Map Relics Inventory"),
    RELIC_ITEM_INFO("Relics are placed inside the Map Device"),
    RELIC_ITEM_INFO2("A Relic Key Item opens the Map Device"),
    RELIC_MAX_COUNT("Maximum [%1$s] of this type can be used"),
    CREATIVE_TAB("Dungeon Realm");

    public String name;

    DungeonWords(String name) {
        this.name = name;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return new TranslationBuilder(DungeonMain.MODID).name(ExileTranslation.of(DungeonMain.MODID + ".words." + this.name().toLowerCase(Locale.ROOT), name));
    }

    public MutableComponent get(Object... obj) {
        return getTranslation(TranslationType.NAME).getTranslatedName(obj);
    }

    @Override
    public String GUID() {
        return name();
    }
}
