package com.robertx22.infinite_dungeons.main;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class MainID {

    public static boolean RUN_DEV_TOOLS() {
        return false;
    }

    public static final String MODID = "infinite_dungeons";

    public static CreativeModeTab ITEM_GROUP = new CreativeModeTab(MODID + ".creative_tab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(DungeonItems.VOID_KEY.get());
        }
    };

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static Component ofTranslation(String path) {
        return Component.translatable(MODID + "." + path);
    }

}
