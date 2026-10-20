package com.robertx22.age_of_exile.uncommon.datasaving;

import com.robertx22.forge_compat.common.capabilities.CapabilityProvider;

import com.robertx22.age_of_exile.capability.ChunkPopulatedCap;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.capability.player.EntitySpellCap;
import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.dimension.dungeon_data.WorldDungeonCap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class Load {

    public static EntitySpellCap.ISpellsCap spells(LivingEntity provider) {
        return CapabilityProvider.get(provider, EntitySpellCap.Data)
            .orElse(null);
    }

    public static ChunkPopulatedCap chunkPopulated(LevelChunk chunk) {
        return CapabilityProvider.get(chunk, ChunkPopulatedCap.Data)
            .orElse(null);
    }

    public static EntityData Unit(Entity entity) {
        return CapabilityProvider.get(entity, EntityData.Data)
            .orElse(new EntityData((LivingEntity) entity));
    }

    public static RPGPlayerData playerRPGData(Player player) {
        return CapabilityProvider.get(player, RPGPlayerData.Data)
            .orElse(null);
    }

    public static WorldDungeonCap dungeonData(Level world) {
        return CapabilityProvider.get(world, WorldDungeonCap.Data)
            .orElse(null);
    }

}
