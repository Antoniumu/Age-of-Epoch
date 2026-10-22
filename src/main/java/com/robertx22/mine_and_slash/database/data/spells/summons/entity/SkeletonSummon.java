package com.robertx22.mine_and_slash.database.data.spells.summons.entity;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class SkeletonSummon extends SummonEntity {
    public SkeletonSummon(EntityType<? extends net.minecraft.world.entity.TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, EntitySpawnReason pReason, @Nullable SpawnGroupData pSpawnData) {
        var d = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
        this.setItemSlot(EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
        return d;
    }
}
