package com.robertx22.forge_compat.common.extensions;

import com.robertx22.forge_compat.fml.network.IContainerFactory;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import io.netty.buffer.Unpooled;

public interface IForgeMenuType {
    static <T extends AbstractContainerMenu> MenuType<T> create(IContainerFactory<T> factory) {
        return new ExtendedMenuType<>((syncId, inv, bytes) -> factory.create(syncId, inv, new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes))), ByteBufCodecs.BYTE_ARRAY);
    }
}
