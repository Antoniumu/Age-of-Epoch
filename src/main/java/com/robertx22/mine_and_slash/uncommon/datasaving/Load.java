package com.robertx22.mine_and_slash.uncommon.datasaving;

import com.robertx22.mine_and_slash.capability.chunk.ChunkCap;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.capability.player.PlayerBackpackData;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.capability.world.WorldData;
import com.robertx22.mine_and_slash.maps.MapData;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class Load {

    // todo give a blank one for mobs


    public static EntityData Unit(Entity entity) {
        return com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(entity, EntityData.INSTANCE).orElse(new EntityData((LivingEntity) entity));
    }

    public static PlayerData player(Player player) {
        return com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(player, PlayerData.INSTANCE).orElse(new PlayerData(player));
    }

    public static PlayerBackpackData backpacks(Player player) {
        return com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(player, PlayerBackpackData.INSTANCE).orElse(null);
    }

    public static WorldData worldData(Level l) {
        return com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(l.getServer().overworld(), WorldData.INSTANCE).orElse(null);
    }

    // todo add connected maps
    public static MapData mapAt(Level l, BlockPos pos) {
        try {
            return WorldUtils.ifMapData(l, pos).get();
        } catch (Exception e) {
            return null;
        }
    }

    public static ChunkCap chunkData(LevelChunk c) {
        return com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(c, ChunkCap.INSTANCE).orElseGet(null);
    }


    public static PlayerData playerRPGData(Player player) {
        return player(player);
    }

    public static com.robertx22.mine_and_slash.saveclasses.spells.SpellCastingData spells(LivingEntity entity) {
        if (entity instanceof Player p) {
            return player(p).spellCastingData;
        }
        return new com.robertx22.mine_and_slash.saveclasses.spells.SpellCastingData();
    }
}
