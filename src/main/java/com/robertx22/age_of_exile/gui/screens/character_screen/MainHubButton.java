package com.robertx22.age_of_exile.gui.screens.character_screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.gui.bases.IAlertScreen;
import com.robertx22.age_of_exile.gui.bases.IContainerNamedScreen;
import com.robertx22.age_of_exile.gui.bases.INamedScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.Screen;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;

public class MainHubButton extends ImageButton {

    public static int xSize = 105;
    public static int ySize = 28;
    public static Identifier EXLAMATION_MARK_TEX = Identifier.fromNamespaceAndPath(
        SlashRef.MODID, "textures/gui/main_hub/exclamation_mark.png");

    boolean shouldAlert = false;

    static Identifier buttonLoc = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/buttons.png");

    INamedScreen screen;

    public MainHubButton(INamedScreen screen, int xPos, int yPos) {
        super(xPos, yPos, xSize, ySize, 0, 0, ySize + 1, buttonLoc, (button) -> {
            if (screen instanceof IContainerNamedScreen) {
                IContainerNamedScreen con = (IContainerNamedScreen) screen;
                con.openContainer();
            } else {
                Minecraft.getInstance().gui
                    .setScreen((Screen) screen);
            }
        });

        this.screen = screen;

        if (screen instanceof IAlertScreen) {
            IAlertScreen alert = (IAlertScreen) screen;
            this.shouldAlert = alert.shouldAlert();
        }

    }

    @Override
    public void renderButton(PoseStack matrix, int x, int y, float ticks) {
        super.renderButton(matrix, x, y, ticks);

        RenderUtils.render16Icon(matrix, screen.iconLocation(), this.x + 9, this.y + 6);

        if (shouldAlert) {
            RenderUtils.render16Icon(matrix, EXLAMATION_MARK_TEX, this.x + 5, this.y + 6);
        }

        String str = screen.screenName()
            .translate();

        ChatFormatting color = isHovered() ? ChatFormatting.GREEN : ChatFormatting.GOLD;
        com.robertx22.forge_compat.client.AbstractGui.drawShadow(Minecraft.getInstance().font, matrix,
            str, this.x + 28, this.y + 9, com.robertx22.forge_compat.common.CompatChatFormatting.getColor(color));
    }

}
