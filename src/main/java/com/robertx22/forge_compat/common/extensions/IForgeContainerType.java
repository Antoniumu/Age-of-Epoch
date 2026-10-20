package com.robertx22.forge_compat.common.extensions;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Forge-style container type factory: (windowId, playerInventory, buf) -> menu.
 * Uses Fabric API's ExtendedMenuType under the hood.
 */
public class IForgeContainerType {

    public interface ExtFactory<T extends AbstractContainerMenu> {
        T create(int windowId, Inventory inv, FriendlyByteBuf buf);
    }

    public static <T extends AbstractContainerMenu> MenuType<T> create(ExtFactory<T> factory) {
        StreamCodec<RegistryFriendlyByteBuf, FriendlyByteBuf> codec = StreamCodec.of(
            (buf, val) -> {
                byte[] bytes = new byte[val.readableBytes()];
                val.getBytes(val.readerIndex(), bytes);
                buf.writeByteArray(bytes);
            },
            buf -> new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(buf.readByteArray()))
        );
        return new ExtendedMenuType<>(factory::create, codec);
    }
}
