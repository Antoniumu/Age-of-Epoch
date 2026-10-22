package com.robertx22.ancient_obelisks.database.item_reqs;

import com.robertx22.ancient_obelisks.item.ObeliskItemMapData;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class ObeliskSpawnRateLimitReq extends ObeliskReq {

    public ObeliskSpawnRateLimitReq(String id, Data data) {
        super("obelisk_spawnrate_cap", id);
        this.data = data;
    }

    public static record Data(int spawnRateCap) {
    }


    public Data data;

    @Override
    public boolean isValid(ItemStack gear, ObeliskItemMapData data) {
        return this.data.spawnRateCap() > data.spawn_rate;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.spawnRateCap());

    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(ObelisksMain.MODID)
                .desc(ExileTranslation.registry(this, "Must have less than %1$s%% Spawn Rate"));
    }

    @Override
    public Class<?> getClassForSerialization() {
        return ObeliskSpawnRateLimitReq.class;
    }
}
