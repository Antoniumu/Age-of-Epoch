package com.robertx22.divine_missions.main;

import com.robertx22.forge_compat.client.ItemBlockRenderTypes;
import com.robertx22.forge_compat.client.RenderType;

public class ClientInit {

    public static void onInitializeClient() {

        ItemBlockRenderTypes.setRenderLayer(ModBlocks.SHRINE.get(), RenderType.translucent());

    }
}
