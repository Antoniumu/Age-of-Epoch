package com.robertx22.ancient_obelisks.database.item_reqs;

import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.network.chat.MutableComponent;

public abstract class BeItemReq extends ItemRequirement {

    public transient String locname;

    public BeItemReq(String serializer, String id, String locname) {
        super(serializer, id);
        this.locname = locname;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return new TranslationBuilder(ObelisksMain.MODID).desc(ExileTranslation.registry(this, locname));
    }

}
