package com.robertx22.ancient_obelisks.database.item_mods;

import com.robertx22.ancient_obelisks.item.ObeliskItemMapData;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class AddObeliskSpawnRateMod extends ObeliskMapMod {


    public static record Data(int spawn_rate) {
    }

    public Data data;

    public AddObeliskSpawnRateMod(String id, Data data) {
        super("add_obelisk_spawn_rate", id);
        this.data = data;
    }

    @Override
    public void modifyGear(ItemStack stack, ObeliskItemMapData data, ItemModificationResult r) {
        data.spawn_rate += this.data.spawn_rate();
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.spawn_rate);

    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        String addword = data.spawn_rate > 0 ? "Increase" : "Decrease";
        return TranslationBuilder.of(ObelisksMain.MODID)
                .desc(ExileTranslation.registry(this, addword + " Mob Spawn Rate by %1$s%%"));
    }

    @Override
    public Class<?> getClassForSerialization() {
        return AddObeliskSpawnRateMod.class;
    }
}
