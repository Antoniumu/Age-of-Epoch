package com.robertx22.mine_and_slash.vanilla_mc.blocks.bases;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

public class VanillaFuelSlot extends Slot {
    public VanillaFuelSlot(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        var item = stack.getItem(); return item == net.minecraft.world.item.Items.COAL || item == net.minecraft.world.item.Items.CHARCOAL || item == net.minecraft.world.item.Items.LAVA_BUCKET || item == net.minecraft.world.item.Items.BLAZE_ROD;
    }

}
