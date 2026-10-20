package com.robertx22.forge_compat.common;

import net.minecraft.world.item.ItemStack;

public class CreativeModeTab {
    private final String name;

    public CreativeModeTab(String name) {
        this.name = name;
    }

    public ItemStack makeIcon() {
        return ItemStack.EMPTY;
    }

    public String getRecipeFolderName() {
        return this.name;
    }
}
