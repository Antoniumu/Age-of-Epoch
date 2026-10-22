package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class ClientTextureUtils {

    public static boolean textureExists(Identifier id) {
        return Minecraft.getInstance()
                .getResourceManager().getResource(id).isPresent();
        //       .hasResource(id);
    }

}