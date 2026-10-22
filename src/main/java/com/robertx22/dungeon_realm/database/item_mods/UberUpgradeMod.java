package com.robertx22.dungeon_realm.database.item_mods;

import com.robertx22.dungeon_realm.item.DungeonItemMapData;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class UberUpgradeMod extends MapMod {
    public UberUpgradeMod(String id) {
        super(id, id);
    }

    @Override
    public void modify(DungeonItemMapData map, ItemModificationResult r) {
        map.uber = true;
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public Class<?> getClassForSerialization() {
        return UberUpgradeMod.class;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(DungeonMain.MODID).desc(ExileTranslation.registry(this, "Adds the Uber Arena"));
    }
}
