package com.robertx22.age_of_exile.gui.overlays.bar_overlays.types;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.age_of_exile.config.GuiPartConfig;
import com.robertx22.age_of_exile.gui.overlays.BarGuiType;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.PointData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.AbstractGui;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;

public class MineAndSlashBars extends AbstractGui {
    static Identifier BASETEX = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/overlay/base.png");

    static int BAR_HEIGHT = 11;
    static int BAR_WIDTH = 107;
    public static int INNER_BAR_WIDTH = 101;
    static int INNER_BAR_HEIGHT = 5;

    static Minecraft mc = Minecraft.getInstance();

    public static void renderMineAndSlashBar(GuiPartConfig config, BarGuiType type, PoseStack matrix, PointData point, String text, boolean drawText) {

        if (!drawText) {
            com.robertx22.forge_compat.client.AbstractGui.bindTexture(BASETEX);
            blit(matrix, point.x, point.y, BAR_WIDTH, BAR_HEIGHT, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
            float multi = type.getMulti(Load.Unit(mc.player), mc.player);
            int bar = (int) (multi * INNER_BAR_WIDTH);
            com.robertx22.forge_compat.client.AbstractGui.bindTexture(type.getTexture(Load.Unit(mc.player), mc.player));
            blit(matrix, point.x + 2, point.y + 2, bar, INNER_BAR_HEIGHT, 0, 0, bar, INNER_BAR_HEIGHT, INNER_BAR_WIDTH, INNER_BAR_HEIGHT);

            if (config.icon_renderer == GuiPartConfig.IconRenderer.LEFT) {
                com.robertx22.forge_compat.client.AbstractGui.bindTexture(type.getIcon(Load.Unit(mc.player), mc.player));
                blit(matrix, point.x - 10, point.y, 9, 9, 0, 0, 9, 9, 9, 9); // draw icon
            } else if (config.icon_renderer == GuiPartConfig.IconRenderer.RIGHT) {
                com.robertx22.forge_compat.client.AbstractGui.bindTexture(type.getIcon(Load.Unit(mc.player), mc.player));
                blit(matrix, point.x + BAR_WIDTH + 1, point.y, 9, 9, 0, 0, 9, 9, 9, 9); // draw icon
            }

        }

        if (drawText) {

            double scale = 0.9D;

            int width = mc.font.width(text);
            int textX = (int) (point.x - width / 2F + BAR_WIDTH / 2F);
            int textY = point.y + 2 + 3;

            double antiScale = 1.0D / scale;

            RenderSystem.scaled(scale, scale, scale);
            double textWidthMinus = (width * antiScale / 2) - width / 2F;// fixed the centering with this!!!
            double textHeightMinus = 9.0D * scale / 2.0D;
            float xp = (float) ((double) textX + textWidthMinus);
            float yp = (float) ((double) textY - textHeightMinus);
            float xf = (float) ((double) xp * antiScale);
            float yf = (float) ((double) yp * antiScale);

            com.robertx22.forge_compat.client.AbstractGui.drawShadow(mc.font, matrix, text, xf, yf, com.robertx22.forge_compat.common.CompatChatFormatting.getColor(ChatFormatting.WHITE));
            RenderSystem.scaled(antiScale, antiScale, antiScale);
        }
    }
}
