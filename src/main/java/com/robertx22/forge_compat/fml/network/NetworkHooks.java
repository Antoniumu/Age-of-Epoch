package com.robertx22.forge_compat.fml.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

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
        openGui(player, provider, buf -> {
            if (pos != null) {
                buf.writeBlockPos(pos);
            }
        });
    }

    public static void openGui(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraDataWriter) {
        if (provider == null || player == null) {
            return;
        }
        player.openMenu(new ExtendedMenuProvider<FriendlyByteBuf>() {
            @Override
            public FriendlyByteBuf getScreenOpeningData(ServerPlayer sp) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                if (extraDataWriter != null) {
                    extraDataWriter.accept(buf);
                }
                return buf;
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return provider.createMenu(syncId, inv, p);
            }
        });
    }
}
