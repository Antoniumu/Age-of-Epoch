package com.robertx22.divine_missions.main;

import com.robertx22.divine_missions.item.MissionItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import com.robertx22.forge_compat.fml.RegistryObject;

public class ModItems {

    public static RegistryObject<Item> MISSION_ITEM = DivineDeffered.ITEMS.register("mission", () -> new MissionItem());
    public static RegistryObject<Item> SHRINE = DivineDeffered.ITEMS.register("shrine", () -> new BlockItem(ModBlocks.SHRINE.get(), new Item.Properties()));

    public static void init() {

    }
}
