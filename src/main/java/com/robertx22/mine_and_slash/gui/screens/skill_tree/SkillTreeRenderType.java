package com.robertx22.mine_and_slash.gui.screens.skill_tree;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class SkillTreeRenderType {
    public static RenderType getSkillTreeRenderType(String name, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
