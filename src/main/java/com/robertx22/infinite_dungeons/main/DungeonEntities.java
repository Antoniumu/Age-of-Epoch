package com.robertx22.infinite_dungeons.main;

import com.robertx22.forge_compat.fml.RegistryObject;
import com.robertx22.infinite_dungeons.entity.TickingTNTEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class DungeonEntities {

    public static RegistryObject<EntityType<?>> TICKING_TNT = DungeonDeffered.ENTITIES.register("ticking_tnt", () -> {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MainID.MODID, "ticking_tnt"));
        return EntityType.Builder.of(TickingTNTEntity::new, MobCategory.MISC)
            .build(key);
    });

    public static void init() {

    }
}
