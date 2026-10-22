package com.robertx22.dungeon_realm.database.uber_arena;


import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapData;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import com.robertx22.forge_compat.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class UberBossArena implements JsonExileRegistry<UberBossArena>, IAutoGson<UberBossArena>, ITranslated {

    public static UberBossArena SERIALIZER = new UberBossArena();

    public String id = "";

    public transient String modid = "";
    public transient String name = "";
    public transient String desc = "";
    public int weight = 1000;


    public SimplePrebuiltMapData structure_data = new SimplePrebuiltMapData(1, "");

    public List<String> possible_bosses = new ArrayList<>();

    public EntityType getRandomBoss() {
        return ForgeRegistries.ENTITY_TYPES.getValue(Identifier.parse(String.valueOf(RandomUtils.randomFromList(possible_bosses))));
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return DungeonDatabase.UBER_BOSS;
    }

    @Override
    public Class<UberBossArena> getClassForSerialization() {
        return UberBossArena.class;
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
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(modid)
                .name(ExileTranslation.registry(this, name))
                .desc(ExileTranslation.registry(this, desc));
    }

    public static UberBossArena createBoss(String id, String name, String modid, String chat, EntityType bossEntity, SimplePrebuiltMapData struc) {
        UberBossArena boss = new UberBossArena();

        boss.id = id;
        boss.modid = modid;
        boss.name = name;
        boss.desc = chat;

        boss.possible_bosses.add(ForgeRegistries.ENTITY_TYPES.getKey(bossEntity).toString());

        boss.structure_data = struc;
        return boss;
    }
}
