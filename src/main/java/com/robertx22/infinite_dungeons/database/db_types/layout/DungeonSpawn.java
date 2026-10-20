package com.robertx22.infinite_dungeons.database.db_types.layout;

import com.robertx22.infinite_dungeons.block.ControlBlockData;
import com.robertx22.library_of_exile.registry.IWeighted;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.Identifier;
import com.robertx22.forge_compat.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class DungeonSpawn implements IWeighted {

    public String entity_id = "";
    public int weight = 1000;
    public int spawn_only_after_wave = -1;
    public List<String> modifiers = new ArrayList<>();

    public boolean canSpawn(ControlBlockData data) {
        if (spawn_only_after_wave > data.current_wave) {
            return false;
        }
        return true;
    }

    public DungeonSpawn addModifier(String id) {
        this.modifiers.add(id);
        return this;
    }

    public DungeonSpawn(EntityType<?> type, int weight, int spawn_only_after_wave) {
        this.entity_id = ForgeRegistries.ENTITIES.getKey(type)
            .toString();
        this.weight = weight;
        this.spawn_only_after_wave = spawn_only_after_wave;
    }

    public EntityType<?> getEntityType() {
        return ForgeRegistries.ENTITIES.getValue(Identifier.parse(entity_id));
    }

    @Override
    public int Weight() {
        return weight;
    }
}
