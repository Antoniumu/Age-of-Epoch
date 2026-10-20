package com.robertx22.forge_compat.fml.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

public class NetworkHooks {

    /** Forge's custom entity spawn packet helper. */
    public static Packet<ClientGamePacketListener> getEntitySpawningPacket(Entity entity) {
        return new ClientboundAddEntityPacket(entity, 0, entity.blockPosition());
    }

    public static void openGui(ServerPlayer player, MenuProvider provider) {
        if (provider != null && player != null) {
            player.openMenu(provider);
        }
    }

    public static void openGui(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        openGui(player, provider);
    }

    public static void openGui(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraDataWriter) {
        openGui(player, provider);
    }
}
