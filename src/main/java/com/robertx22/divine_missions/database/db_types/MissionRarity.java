package com.robertx22.divine_missions.database.db_types;

import com.robertx22.divine_missions.db_init.RegistryTypes;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.util.FormatUtils;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class MissionRarity implements JsonExileRegistry<MissionRarity>, IAutoGson<MissionRarity> {
    public static MissionRarity SERIALIZER = new MissionRarity();

    public int weight = 1000;
    public String id = "";

    public int min_tasks = 2;
    public int max_tasks = 3;

    public int rank = 0;
    public String format = "";
    public float reward_multi = 1;
    public float diff_multi = 1;

    public ChatFormatting getFormat() {
        return FormatUtils.of(format);
    }

    public Component getTranslated() {
        return Component.translatable(DivineMissions.MODID + ".rarity." + id);
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return RegistryTypes.MISSION_RARITY;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }

    @Override
    public Class<MissionRarity> getClassForSerialization() {
        return MissionRarity.class;
    }
}
