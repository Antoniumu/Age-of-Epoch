package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.map;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.MapRequirement;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class MapIsRarityReq extends MapRequirement {

    public Data data;

    public static record Data(String rar) {
        GearRarity getRarity() {
            return ExileDB.GearRarities().get(rar);
        }
    }

    public MapIsRarityReq(String id, Data data) {
        super(id, id);
        this.data = data;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return MapIsRarityReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.getRarity().coloredName());
    }

    @Override
    public boolean isMapValid(ExileStack stack) {

        return stack.get(StackKeys.MAP).hasAndTrue(x -> x.rar.equals(data.rar));
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Map Rarity Must be %1$s")
                );
    }

}
