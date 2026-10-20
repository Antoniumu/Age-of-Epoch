package com.robertx22.forge_compat.fml.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.world.MenuProvider;
public class NetworkHooks {

    /** Forge's custom entity spawn packet helper. */
    public static Packet<ClientGamePacketListener> getEntitySpawningPacket(Entity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        return entity.getAddEntityPacket(new ServerEntity(level, entity, 0, false));
    }

    public static void openGui(ServerPlayer player, net.minecraft.world.MenuProvider provider) {
        if (provider != null) {
            player.openMenu(provider);
        }
    }
}
