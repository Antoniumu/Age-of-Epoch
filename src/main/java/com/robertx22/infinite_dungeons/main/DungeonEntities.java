package com.robertx22.infinite_dungeons.main;

import com.robertx22.infinite_dungeons.entity.TickingTNTEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import com.robertx22.forge_compat.fml.RegistryObject;

public class DungeonEntities {

    public static RegistryObject<EntityType<?>> TICKING_TNT = DungeonDeffered.ENTITIES.register("ticking_tnt", ()
        -> EntityType.Builder.of(TickingTNTEntity::new, MobCategory.MISC)
        .build("ticking_tnt"));
    ;

    public static void init() {

    }
}
