package com.robertx22.age_of_exile.mmorpg;

import net.minecraft.resources.Identifier;

public class SlashRef {

    public static Identifier id(String id) {
        return Identifier.fromNamespaceAndPath(MODID, id);
    }

    public static Identifier guiId(String id) {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/" + id + ".png");
    }

    public static final String MODID = "mmorpg";
    public static final String MOD_NAME = "Age of Exile";

    public static String WORLD_OF_EXILE_ID = "world_of_exile";
}
