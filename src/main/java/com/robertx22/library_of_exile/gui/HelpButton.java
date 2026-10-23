package com.robertx22.library_of_exile.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.library_of_exile.utils.GuiUtils;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

import java.util.List;

public class HelpButton extends ImageButton {

    static Identifier ID = Identifier.fromNamespaceAndPath(Ref.MODID, "textures/gui/spell_help.png");
    List<Component> tooltip;

    public HelpButton(List<Component> tooltip, int x, int y) {
        super(x, y, 20, 20, 0, 0, 1, ID,
            action -> {

            });
        this.tooltip = tooltip;
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, 20, 20, x, y);
    }

    @Override
    public void renderToolTip(PoseStack matrices, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY)) {
            GuiUtils.renderTooltip(matrices, tooltip, mouseX, mouseY);
        }
    }
}