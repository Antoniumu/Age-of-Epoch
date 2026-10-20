package com.robertx22.age_of_exile.mmorpg;

import net.minecraft.resources.ResourceLocation;

public class SlashRef {

    public static ResourceLocation id(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }

    public static ResourceLocation guiId(String id) {
        return ResourceLocation.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/" + id + ".png");
    }

    public static final String MODID = "mmorpg";
    public static final String MOD_NAME = "Age of Exile";

    public static String WORLD_OF_EXILE_ID = "world_of_exile";
}
