package com.robertx22.ancient_obelisks.database.item_reqs;

import com.robertx22.ancient_obelisks.item.ObeliskItemMapData;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class LessThanXWavesReq extends ObeliskReq {

    public LessThanXWavesReq(String id, Data data) {
        super("less_than_x_waves", id);
        this.data = data;
    }

    public static record Data(int waves) {
    }


    public Data data;

    @Override
    public boolean isValid(ItemStack gear, ObeliskItemMapData data) {
        return this.data.waves > data.maxWaves;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.waves);

    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        String plural = Math.abs(data.waves) > 1 ? "s" : "";
        return TranslationBuilder.of(ObelisksMain.MODID)
                .desc(ExileTranslation.registry(this, "Must have less than %1$s Wave" + plural));
    }

    @Override
    public Class<?> getClassForSerialization() {
        return LessThanXWavesReq.class;
    }
}
