package com.robertx22.the_harvest.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class HarvestMapItem extends Item {
    public HarvestMapItem() {
        super(new Properties().stacksTo(1));
    }


    public static ItemStack blankMap(ItemStack stack, boolean relic) {


        var data = new HarvestItemMapData();

   
        data.relic = relic;

        HarvestItemNbt.HARVEST_MAP.saveTo(stack, data);

        return stack;

    }
}
