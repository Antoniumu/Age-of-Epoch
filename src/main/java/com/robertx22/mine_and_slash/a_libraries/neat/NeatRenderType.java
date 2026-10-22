package com.robertx22.mine_and_slash.a_libraries.neat;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class NeatRenderType {
    public static final Identifier HEALTH_BAR_TEXTURE = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/health_bar_texture.png");
    public static final RenderType BAR_TEXTURE_TYPE = RenderTypes.entityTranslucent(HEALTH_BAR_TEXTURE);
}
