package com.robertx22.ancient_obelisks.database.item_mods;

import com.robertx22.ancient_obelisks.item.ObeliskItemMapData;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class AddObeliskTierMod extends ObeliskMapMod {


    public static record Data(int tier) {
    }

    public Data data;

    public AddObeliskTierMod(String id, Data data) {
        super("add_obelisk_tier", id);
        this.data = data;
    }

    @Override
    public void modifyGear(ItemStack stack, ObeliskItemMapData data, ItemModificationResult r) {
        for (int i = 0; i < this.data.tier; i++) {
            data.tier++;
        }
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.tier);

    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        String addword = data.tier > 0 ? "Add" : "Decrease";
        String plural = Math.abs(data.tier) > 1 ? "s" : "";
        return TranslationBuilder.of(ObelisksMain.MODID)
                .desc(ExileTranslation.registry(this, addword + " %1$s Tier" + plural));
    }

    @Override
    public Class<?> getClassForSerialization() {
        return AddObeliskTierMod.class;
    }
}
