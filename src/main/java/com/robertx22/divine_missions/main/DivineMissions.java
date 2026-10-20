package com.robertx22.divine_missions.main;
import net.minecraft.network.chat.MutableComponent;

import com.robertx22.forge_compat.common.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class DivineMissions {

    public static boolean RUN_DEV_TOOLS() {
        return false;
    }

    public final static String MODID = "divine_missions";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static MutableComponent ofTranslation(String path) {
        return Component.translatable(MODID + "." + path);
    }

    public static CreativeModeTab CreativeTab = new CreativeModeTab("creative_tab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.SHRINE.get());
        }
    };

}
