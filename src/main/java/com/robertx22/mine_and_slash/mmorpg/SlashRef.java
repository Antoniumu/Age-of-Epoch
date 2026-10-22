package com.robertx22.mine_and_slash.mmorpg;

import com.robertx22.library_of_exile.localization.TranslationKeyBuilder;
import net.minecraft.resources.Identifier;

public class SlashRef {

    public static Identifier id(String id) {
        return Identifier.fromNamespaceAndPath(MODID, id);
    }

    public static Identifier guiId(String id) {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/" + id + ".png");
    }

    public static final String MODID = "mmorpg";
    public static final String MOD_NAME = "Mine and Slash";

    public static TranslationKeyBuilder TRANSLATION_KEY = new TranslationKeyBuilder(MODID);

}
