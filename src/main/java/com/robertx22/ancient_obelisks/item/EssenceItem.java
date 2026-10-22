package com.robertx22.ancient_obelisks.item;

import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import net.minecraft.world.item.Item;

public class EssenceItem extends Item implements ITranslated {

    String name;

    public EssenceItem(String name) {
        super(new Properties().stacksTo(64));
        this.name = name;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(ObelisksMain.MODID)
                .desc(ExileTranslation.item(this, "Essence of Ancient " + name));
    }

    @Override
    public String GUID() {
        return "";
    }
}
