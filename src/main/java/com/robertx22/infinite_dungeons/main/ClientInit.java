package com.robertx22.infinite_dungeons.main;

import com.robertx22.age_of_exile.database.data.spells.entities.renders.MySpriteRenderer;
import com.robertx22.forge_compat.client.ItemBlockRenderTypes;
import com.robertx22.forge_compat.client.RenderType;
import com.robertx22.forge_compat.fml.client.registry.RenderingRegistry;

public class ClientInit {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onInitializeClient() {

        ItemBlockRenderTypes.setRenderLayer(DungeonBlocks.CONTROL_BLOCK.get(), RenderType.translucent());

        RenderingRegistry.registerEntityRenderingHandler(DungeonEntities.TICKING_TNT.get(),
            ctx -> new MySpriteRenderer(ctx));

    }
}
