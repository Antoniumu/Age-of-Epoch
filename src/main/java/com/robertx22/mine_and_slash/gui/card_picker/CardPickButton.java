package com.robertx22.mine_and_slash.gui.card_picker;

import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public class CardPickButton extends ImageButton {

    public static int SIZE_X = 124;
    public static int SIZE_Y = 191;
    static int iconSize = 34;

    Minecraft mc = Minecraft.getInstance();
    ICard card;

    public CardPickButton(ICard card, int xPos, int yPos) {
        super(xPos, yPos, SIZE_X, SIZE_Y, 0, 0, 0, SlashRef.guiId("pick_card"), (button) -> {
            card.onClick(ClientOnly.getPlayer());
            Minecraft.getInstance().gui.setScreen(null);
        });
        this.card = card;
    }

    public void renderLegacyUnused(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        setModTooltip();
        super.extractContents(gui, mouseX, mouseY, delta);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        if (this.isHovered()) setModTooltip();
        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, this.resourceLocation, getX(), getY(), 0, 0, SIZE_X, SIZE_Y);
        Identifier tex = card.getIcon();
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, tex, getX() + 45, getY() + 38, iconSize, iconSize, iconSize, iconSize, iconSize, iconSize);

        com.robertx22.forge_compat.client.GuiBlitHelper.drawString(gui, mc.font, card.getName(), this.getX() + SIZE_X / 2 - mc.font.width(card.getName()) / 2, getY() + 20, com.robertx22.forge_compat.common.CompatChatFormatting.getColor(ChatFormatting.RED));


        var tip = TextUTIL.mergeList(card.getScreenText(mc.player));

        var split = mc.font.split(tip, 96);

        int max = 0;
        for (FormattedCharSequence c : split) {
            int size = mc.font.width(c);
            if (size > max) {
                max = size;
            }
        }

        gui.textWithWordWrap(mc.font, tip, this.getX() + SIZE_X / 2 - max / 2, getY() + 100, 96, 0xFFFFFF55, false);
    }


    public void setModTooltip() {
        this.setTooltip(Tooltip.create(TextUTIL.mergeList(card.getTooltip(mc.player))));
    }


}