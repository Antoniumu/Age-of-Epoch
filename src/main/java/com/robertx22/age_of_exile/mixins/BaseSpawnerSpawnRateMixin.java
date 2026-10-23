package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.gamemaster.GameMaster;
import com.robertx22.age_of_exile.gamemaster.SpawnRateUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/** Applies a player's spawn-rate setting to ordinary hostile block spawners. */
@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerSpawnRateMixin {

    @Unique
    private Object aoe$cachedSpawnData;

    @Unique
    private boolean aoe$spawnDataResolved;

    @Unique
    private boolean aoe$rateControlledSpawner;

    @Unique
    private boolean aoe$excludedSpawnData;

    @Shadow
    private SpawnData nextSpawnData;

    @Shadow
    private int requiredPlayerRange;

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void aoe$applyPlayerSpawnRate(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (!GameMaster.isActive() || level.isClientSide()) {
            return;
        }

        SpawnData spawnData = this.nextSpawnData;
        if (spawnData == null) {
            // Vanilla chooses a spawn entry the first time the spawner runs. Do not guess
            // its type: an unknown entry may be a boss or a player-owned minion.
            return;
        }
        aoe$resolveSpawnData(spawnData);
        if (!aoe$rateControlledSpawner || aoe$excludedSpawnData) {
            return;
        }

        ServerPlayer player = SpawnRateUtils.nearestEligiblePlayer(level,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            Math.max(0, this.requiredPlayerRange));
        if (player != null && !SpawnRateUtils.allowSpawnAttempt(player, level.getRandom())) {
            // Skipping a tick also pauses the vanilla spawn-delay countdown, reducing
            // the spawner's real rate without interfering with its spawn data or mob cap.
            ci.cancel();
        }
    }

    @Unique
    private void aoe$resolveSpawnData(SpawnData spawnData) {
        if (aoe$spawnDataResolved && aoe$cachedSpawnData == spawnData) {
            return;
        }

        aoe$cachedSpawnData = spawnData;
        aoe$spawnDataResolved = true;
        aoe$rateControlledSpawner = false;
        aoe$excludedSpawnData = false;

        try {
            CompoundTag tag = spawnData.getEntityToSpawn();
            aoe$excludedSpawnData = containsExcludedTag(tag.toString());

            String id = tag.getStringOr("id", "");
            if (id.isEmpty()) {
                return;
            }
            Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id));
            aoe$rateControlledSpawner = type.isPresent()
                && SpawnRateUtils.isRateControlledHostile(type.get());
        } catch (Exception ignored) {
            // If malformed spawn data cannot be inspected, leave that spawner alone.
            aoe$rateControlledSpawner = false;
            aoe$excludedSpawnData = true;
        }
    }

    @Unique
    private static boolean containsExcludedTag(String nbt) {
        return nbt.contains("mmorpg_minion")
            || nbt.contains("aoe_tower_boss")
            || nbt.contains("aoe_tower_champion")
            || nbt.contains("aoe_delve_warden")
            || nbt.contains("woe_tower_boss");
    }
}
