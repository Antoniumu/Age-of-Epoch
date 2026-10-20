package com.robertx22.divine_missions.main;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

public class ClientInit {

    public static void onInitializeClient() {

        ItemBlockRenderTypes.setRenderLayer(ModBlocks.SHRINE.get(), RenderType.translucent());

    }
}
