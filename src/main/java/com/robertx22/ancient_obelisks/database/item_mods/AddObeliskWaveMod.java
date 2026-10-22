package com.robertx22.ancient_obelisks.database.item_mods;

import com.robertx22.ancient_obelisks.item.ObeliskItemMapData;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class AddObeliskWaveMod extends ObeliskMapMod {


    public static record Data(int waves) {
    }


    public Data data;

    public AddObeliskWaveMod(String id, Data data) {
        super("add_obelisk_wave", id);
        this.data = data;
    }

    @Override
    public void modifyGear(ItemStack stack, ObeliskItemMapData data, ItemModificationResult r) {
        for (int i = 0; i < this.data.waves(); i++) {
            data.maxWaves++;
            data.addRandomAffix();
        }
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.waves);

    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        String addword = data.waves > 0 ? "Add" : "Remove";
        String plural = Math.abs(data.waves) > 1 ? "s" : "";
        return TranslationBuilder.of(ObelisksMain.MODID)
                .desc(ExileTranslation.registry(this, addword + " %1$s Wave" + plural));
    }

    @Override
    public Class<?> getClassForSerialization() {
        return AddObeliskWaveMod.class;
    }
}
