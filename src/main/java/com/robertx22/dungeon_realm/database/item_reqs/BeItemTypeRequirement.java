package com.robertx22.dungeon_realm.database.item_reqs;


import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.network.chat.MutableComponent;

public abstract class BeItemTypeRequirement extends ItemRequirement {

    public transient String locname;

    public BeItemTypeRequirement(String id, String locname) {
        super(id, id);
        this.locname = locname;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return new TranslationBuilder(DungeonMain.MODID).desc(ExileTranslation.registry(this, locname));
    }

}
