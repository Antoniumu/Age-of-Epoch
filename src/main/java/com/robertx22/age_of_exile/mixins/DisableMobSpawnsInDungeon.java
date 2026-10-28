package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.gamemaster.SpawnRateUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
        if (serverWorldAccess instanceof ServerLevel hubLevel
            && com.robertx22.age_of_exile.airship.AirshipHub.protectedAt(hubLevel,pos)
            && (spawnReason==EntitySpawnReason.NATURAL || spawnReason==EntitySpawnReason.CHUNK_GENERATION)) {
            ci.setReturnValue(false);return;
        }
        if (WorldUtils.isMapWorldClass(serverWorldAccess) || WorldUtils.isSimulationWorld(serverWorldAccess)) {
            if (spawnReason == EntitySpawnReason.NATURAL) {
                ci.setReturnValue(false);
                return;
            }
        }

        if (spawnReason == EntitySpawnReason.NATURAL
            && serverWorldAccess instanceof ServerLevel level
            && !SpawnRateUtils.allowNaturalSpawn(level, type, pos, random)) {
            ci.setReturnValue(false);
        }
    }
}
