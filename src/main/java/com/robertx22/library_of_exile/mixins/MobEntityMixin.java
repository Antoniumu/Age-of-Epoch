package com.robertx22.library_of_exile.mixins;

import com.robertx22.library_of_exile.components.EntityInfoComponent;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobEntityMixin {

    @Inject(method = "finalizeSpawn(Lnet/minecraft/world/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/entity/MobSpawnType;Lnet/minecraft/entity/SpawnGroupData;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/entity/SpawnGroupData;", at = @At("HEAD"))
    private void hook(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, SpawnGroupData entityData, CompoundTag entityTag, CallbackInfoReturnable<SpawnGroupData> ci) {

        try {
            LivingEntity en = (LivingEntity) (Object) this;
            EntityInfoComponent.IEntityInfo info = EntityInfoComponent.get(en);
            info.setSpawnReasonOnCreate(spawnReason);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
