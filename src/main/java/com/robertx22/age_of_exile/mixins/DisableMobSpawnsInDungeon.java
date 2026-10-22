package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.simulation.SimulationManager;
import com.robertx22.library_of_exile.dimension.MapDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public class DisableMobSpawnsInDungeon {

    @Inject(method = "checkSpawnRules", at = @At(value = "HEAD"), cancellable = true)
    private static <T extends Entity> void disableCanSpawn(EntityType<T> type, ServerLevelAccessor serverWorldAccess, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> ci) {
        if ((MapDimensions.isMap(serverWorldAccess.getLevel()) || SimulationManager.isSimulationLevel(serverWorldAccess))
            && spawnReason == EntitySpawnReason.NATURAL) {
            ci.setReturnValue(false);
        }
    }
}
