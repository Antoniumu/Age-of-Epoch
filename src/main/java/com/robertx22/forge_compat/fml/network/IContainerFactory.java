package com.robertx22.forge_compat.fml.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface IContainerFactory<T extends AbstractContainerMenu> {
    T create(int windowId, Inventory inv, FriendlyByteBuf data);
}
