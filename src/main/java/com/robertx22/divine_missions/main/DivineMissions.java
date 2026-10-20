package com.robertx22.divine_missions.main;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class DivineMissions {

    public static boolean RUN_DEV_TOOLS() {
        return false;
    }

    public final static String MODID = "divine_missions";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static Component ofTranslation(String path) {
        return Component.translatable(MODID + "." + path);
    }

    public static CreativeModeTab CreativeTab = new CreativeModeTab("creative_tab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.SHRINE.get());
        }
    };

}
