package com.robertx22.age_of_exile.database.data.tiers.base;

import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.registry.serialization.ISerializable;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Mob;
public class Difficulty implements JsonExileRegistry<Difficulty>, IAutoGson<Difficulty>, IAutoLocName {

    public static ISerializable<Difficulty> SERIALIZER = new Difficulty();

    public String id = "";
    public float hp_multi = 1;
    public float dmg_multi = 1;
    public float stat_multi = 1;

    public float loot_multi = 1;
    public float higher_rar_chance = 0;

    public int req_player_lvl = 0;

    public int deaths_allowed = -1;
    public int death_favor_penalty = 0;

    public int rank = 0;
    public transient String locname = "";

    public static Difficulty fromRank(int therank) {
        return ExileDB.Difficulties()
            .getList()
            .stream()
            .filter(x -> x.rank == therank)
            .findAny()
            .get();

    }

    @Override
    public Class<Difficulty> getClassForSerialization() {
        return Difficulty.class;
    }

    public List<Component> getTooltip() {

        List<Component> list = new ArrayList<>();

        list.add(Component.literal("Сложность: ").append(locName())
            .withStyle(ChatFormatting.DARK_RED));

        list.add(Component.literal(""));

        list.add(Component.literal("Здоровье существ: " + (int) (hp_multi * 100) + "%").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Урон существ: " + (int) (dmg_multi * 100) + "%").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Характеристики существ: " + (int) (stat_multi * 100) + "%").withStyle(ChatFormatting.RED));

        list.add(Component.literal(""));

        list.add(Component.literal("Добыча: " + (int) (loot_multi * 100) + "%").withStyle(ChatFormatting.GOLD));

        return list;
    }

    public Difficulty() {

    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.TIER;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return 1000;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Misc;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".difficulty." + id;
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }
}
