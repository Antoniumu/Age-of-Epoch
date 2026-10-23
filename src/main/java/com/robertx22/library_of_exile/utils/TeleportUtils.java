package com.robertx22.library_of_exile.utils;

import com.robertx22.library_of_exile.events.base.StaticServerPlayerTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Set;

public class TeleportUtils {

    public static void teleport(ServerPlayer player, BlockPos pos) {
        teleport(player, pos, player.level().dimension().identifier());
    }

    public static void teleport(ServerPlayer player, BlockPos pos, DimensionType dimension) {
        Identifier key = player.level().registryAccess()
            .lookupOrThrow(Registries.DIMENSION_TYPE)
            .getKey(dimension);
        if (key != null) {
            teleport(player, pos, key);
        }
    }

    public static void teleport(ServerPlayer player, BlockPos pos, Identifier dimension) {
        teleport(player, pos, dimension, true);
    }

    public static void teleport(ServerPlayer player, BlockPos pos, Identifier dimension, Boolean addSafety) {
        try {

            ServerLevel world = player.level().getServer()
                .getLevel(ResourceKey.create(Registries.DIMENSION, dimension));

            if (world == null) {
                System.out.println("No world with id: " + dimension);
                return;
            }

            player.teleportTo(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, Set.of(), 0F, 0F, false);

            if (addSafety) {
                StaticServerPlayerTickEvent.makeSureTeleport(player, pos, dimension);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
