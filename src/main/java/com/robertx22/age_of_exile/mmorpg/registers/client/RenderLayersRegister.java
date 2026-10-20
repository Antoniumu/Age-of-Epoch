package com.robertx22.age_of_exile.mmorpg.registers.client;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlocks;
import com.robertx22.forge_compat.client.ItemBlockRenderTypes;
import com.robertx22.forge_compat.client.RenderType;

public class RenderLayersRegister {
    public static void setup() {

        ItemBlockRenderTypes.setRenderLayer(SlashBlocks.PORTAL.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(SlashBlocks.BLACK_HOLE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(SlashBlocks.TELEPORTER.get(), RenderType.translucent());

        ItemBlockRenderTypes.setRenderLayer(SlashBlocks.BLUE_TOTEM.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(SlashBlocks.GUARD_TOTEM.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(SlashBlocks.TRAP.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(SlashBlocks.GREEN_TOTEM.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(SlashBlocks.GLYPH.get(), RenderType.cutout());

        ItemBlockRenderTypes.setRenderLayer(SlashBlocks.RUNEWORD.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(SlashBlocks.SPAWNER.get(), RenderType.cutout());

    }
}
