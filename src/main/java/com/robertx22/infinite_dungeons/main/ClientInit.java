package com.robertx22.infinite_dungeons.main;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.Entity;
import com.robertx22.forge_compat.fml.client.registry.IRenderFactory;
import com.robertx22.forge_compat.fml.client.registry.RenderingRegistry;

public class ClientInit {

    public static void onInitializeClient() {

        ItemBlockRenderTypes.setRenderLayer(DungeonBlocks.CONTROL_BLOCK.get(), RenderType.translucent());

        RenderingRegistry.registerEntityRenderingHandler(DungeonEntities.TICKING_TNT.get(),
            (IRenderFactory<Entity>) manager -> new ThrownItemRenderer(manager, Minecraft.getInstance()
                .getItemRenderer()));

    }
}
