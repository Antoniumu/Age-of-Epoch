package com.robertx22.ancient_obelisks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ObeliskMapItem extends Item {
    public ObeliskMapItem() {
        super(new Properties().stacksTo(1));
    }


    public static ItemStack blankMap(ItemStack stack, boolean relic) {

    
        var data = new ObeliskItemMapData();

        data.generateInitialWaves();

        data.relic = relic;

        ObeliskItemNbt.OBELISK_MAP.saveTo(stack, data);

        return stack;

    }
}
