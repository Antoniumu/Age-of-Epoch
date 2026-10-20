package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SpawnPlacementRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(SpawnPlacementRegistry.class)
public class DisableMobSpawnsInDungeon {

    @Inject(method = "checkSpawnRules(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/ServerLevelAccessor;Lnet/minecraft/entity/MobSpawnType;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)Z", at = @At(value = "HEAD"), cancellable = true)
    private static <T extends Entity> void disableCanSpawn(EntityType<T> type, ServerLevelAccessor serverWorldAccess, MobSpawnType spawnReason, BlockPos pos, Random random, CallbackInfoReturnable<Boolean> ci) {
        if (WorldUtils.isMapWorldClass(serverWorldAccess)) {
            if (spawnReason == MobSpawnType.NATURAL) {
                ci.setReturnValue(false);
            }
        }
    }

}
