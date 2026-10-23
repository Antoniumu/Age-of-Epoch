package com.robertx22.age_of_exile.dimension.database.dungeon_mob_lists;

import com.robertx22.library_of_exile.registry.IWeighted;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
public class WeightedMobEntry implements IWeighted {

    public int weight = 1000;
    public String id = "";

    public WeightedMobEntry(int weight, EntityType<?> type) {
        this.weight = weight;
        this.id = BuiltInRegistries.ENTITY_TYPE.getKey(type)
            .toString();
    }

    public WeightedMobEntry() {
    }

    @Override
    public int Weight() {
        return weight;
    }
}
