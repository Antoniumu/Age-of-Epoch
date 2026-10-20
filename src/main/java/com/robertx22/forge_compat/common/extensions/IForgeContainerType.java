package com.robertx22.forge_compat.common.extensions;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import net.minecraft.world.inventory.AbstractContainerMenu;
/**
 * Forge-style container type factory: (windowId, playerInventory, buf) -> menu.
 * Uses MenuType's ExtFactory under the hood.
 */
public class IForgeContainerType {

    public interface ExtFactory<T extends net.minecraft.world.inventory.AbstractContainerMenu> {
        T create(int windowId, Inventory inv, FriendlyByteBuf buf);
    }

    public static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> create(ExtFactory<T> factory) {
        return new MenuType<>((windowId, inv) -> factory.create(windowId, inv, new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer())),
            net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
    }
}
