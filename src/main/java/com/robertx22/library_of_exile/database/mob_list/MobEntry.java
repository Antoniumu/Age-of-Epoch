package com.robertx22.library_of_exile.database.mob_list;

import com.robertx22.library_of_exile.registry.IWeighted;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import com.robertx22.forge_compat.registries.ForgeRegistries;

public class MobEntry implements IWeighted {

    public int weight;
    public String mob_id;

    public MobEntry(int weight, EntityType mob) {
        this.weight = weight;
        this.mob_id = ForgeRegistries.ENTITY_TYPES.getKey(mob).toString();
    }

    public EntityType getType() {
        return ForgeRegistries.ENTITY_TYPES.getValue(Identifier.parse(String.valueOf(mob_id)));
    }

    @Override
    public int Weight() {
        return weight;
    }
}
