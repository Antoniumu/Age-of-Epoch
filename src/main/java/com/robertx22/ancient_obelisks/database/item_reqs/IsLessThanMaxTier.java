package com.robertx22.ancient_obelisks.database.item_reqs;

import com.robertx22.ancient_obelisks.configs.ObeliskConfig;
import com.robertx22.ancient_obelisks.item.ObeliskItemMapData;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class IsLessThanMaxTier extends ObeliskReq {

    public IsLessThanMaxTier(String id) {
        super("is_less_than_max_tier", id);
    }


    @Override
    public boolean isValid(ItemStack gear, ObeliskItemMapData data) {
        return ObeliskConfig.get().MAX_OBELISK_TIER.get() > data.tier;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(ObeliskConfig.get().MAX_OBELISK_TIER.get());

    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(ObelisksMain.MODID)
                .desc(ExileTranslation.registry(this, "Obelisk Tier must be lower than %1$s"));
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsLessThanMaxTier.class;
    }
}
