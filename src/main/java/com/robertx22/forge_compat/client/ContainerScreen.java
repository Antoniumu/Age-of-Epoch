package com.robertx22.forge_compat.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

public class ContainerScreen extends AbstractContainerScreen<ChestMenu> {
    public ContainerScreen(ChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
}
