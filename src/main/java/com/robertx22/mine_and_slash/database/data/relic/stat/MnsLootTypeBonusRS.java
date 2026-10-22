package com.robertx22.mine_and_slash.database.data.relic.stat;

import com.robertx22.library_of_exile.database.relic.stat.RelicStat;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;

public class MnsLootTypeBonusRS extends RelicStat {

    public LootType type = LootType.Gear;

    public MnsLootTypeBonusRS(String id, LootType type) {
        super("mns_loot_type_bonus", id);
        this.type = type;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return MnsLootTypeBonusRS.class;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID).name(ExileTranslation.registry(this, type.getName() + " Drop rate %1$s"));
    }
}
