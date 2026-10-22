package com.robertx22.forge_compat.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

public class ContainerScreen extends net.minecraft.client.gui.screens.inventory.ContainerScreen {
    public ContainerScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    public int getGuiLeft() { return this.leftPos; }
    public int getGuiTop() { return this.topPos; }
    public int getXSize() { return 176; }
    public int getYSize() { return 114 + this.getMenu().getRowCount() * 18; }
}
