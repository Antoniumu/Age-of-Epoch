package com.robertx22.infinite_dungeons.main;
import net.minecraft.network.chat.MutableComponent;

import com.robertx22.forge_compat.common.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

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

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static MutableComponent ofTranslation(String path) {
        return Component.translatable(MODID + "." + path);
    }

}
