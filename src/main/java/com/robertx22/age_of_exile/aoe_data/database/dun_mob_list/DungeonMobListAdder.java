package com.robertx22.age_of_exile.aoe_data.database.dun_mob_list;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.world.entity.EntityType;

public class DungeonMobListAdder implements ExileRegistryInit {

    @Override
    public void registerAll() {

        DungeonMobListBuilder.of("mossy")
            .addMob(net.minecraft.world.entity.EntityTypes.SKELETON)
            .addMob(net.minecraft.world.entity.EntityTypes.ZOMBIE)
            .addMob(net.minecraft.world.entity.EntityTypes.PILLAGER, 200)
            .addMob(net.minecraft.world.entity.EntityTypes.STRAY, 200)
            .addMob(net.minecraft.world.entity.EntityTypes.WITCH, 50)

            .addBoss(net.minecraft.world.entity.EntityTypes.WITHER_SKELETON)
            .build();

        DungeonMobListBuilder.of("undead")
            .addMob(net.minecraft.world.entity.EntityTypes.SKELETON)
            .addMob(net.minecraft.world.entity.EntityTypes.ZOMBIE)
            .addMob(net.minecraft.world.entity.EntityTypes.STRAY, 200)

            .addBoss(net.minecraft.world.entity.EntityTypes.WITHER_SKELETON)
            .build();

        DungeonMobListBuilder.of("nether")
            .addMob(net.minecraft.world.entity.EntityTypes.WITHER_SKELETON)
            .addMob(net.minecraft.world.entity.EntityTypes.BLAZE, 200)
            .addMob(net.minecraft.world.entity.EntityTypes.MAGMA_CUBE, 50)

            .addBoss(net.minecraft.world.entity.EntityTypes.WITHER_SKELETON)
            .build();

    }
}
