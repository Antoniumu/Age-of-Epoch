package com.robertx22.age_of_exile.gui.screens.wiki;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.utils.RenderUtils;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.resources.Identifier;

public class WikiTypeButton extends ImageButton {

    public static int xSize = 20;
    public static int ySize = 20;

    static Identifier buttonLoc = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/clear_button.png");

    Identifier icon;

    WikiType type;

    public WikiTypeButton(WikiScreen screen, WikiType type, int xPos, int yPos) {
        super(xPos, yPos, xSize, ySize, 0, 0, ySize + 1, buttonLoc, (button) -> {
            screen.changeType(type);
        });
        this.type = type;
        this.icon = type.getIconLoc();

    }

    @Override
    public void renderButton(PoseStack matrix, int x, int y, float ticks) {
        super.renderButton(matrix, x, y, ticks);
        RenderUtils.render16Icon(matrix, icon, this.x + 2, this.y + 2);
    }

}

