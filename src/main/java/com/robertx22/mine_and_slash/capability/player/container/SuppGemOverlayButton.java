package com.robertx22.mine_and_slash.capability.player.container;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.MaxLinks;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.library_of_exile.utils.TextUTIL;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class SuppGemOverlayButton extends ImageButton {

    public static int BUTTON_SIZE_X = 20;
    public static int BUTTON_SIZE_Y = 19;


    boolean can;

    MaxLinks links;

    public SuppGemOverlayButton(boolean can, MaxLinks links, int xPos, int yPos) {
        super(xPos, yPos, BUTTON_SIZE_X, BUTTON_SIZE_Y, 0, 0, BUTTON_SIZE_Y, SlashRef.guiId("blocked_slot"), (button) -> {
            //Minecraft.getInstance().gui.setScreen(new InvGuiScreen(GuiInventoryGrids.ofSelectableSpells(ClientOnly.getPlayer(), slot)));
        });
        this.can = can;
        this.links = links;
    }

    public void renderLegacyUnused(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {

        List<Component> tooltip = new ArrayList<>();
        if (!can) {
            tooltip.add(Words.LockedSuppGemSlot.locName().withStyle(ChatFormatting.RED));

            tooltip.add(Component.empty());

            if (links == null) {
                tooltip.add(Words.NoSocketedSpell.locName().withStyle(ChatFormatting.YELLOW));

            } else {
                if (links.cappedByLevel) {
                    tooltip.add(Words.IncreaseYourLevel.locName().withStyle(ChatFormatting.YELLOW));
                }
                if (links.cappedBySpellLevel) {
                    tooltip.add(Words.IncreaseSpellLevel.locName().withStyle(ChatFormatting.YELLOW));
                }
            }
        }
        this.setTooltip(Tooltip.create(TextUTIL.mergeList(tooltip)));

        super.extractContents(gui, mouseX, mouseY, delta);
    }


    static Identifier id = SlashRef.guiId("blocked_slot");

    protected boolean clicked(double pMouseX, double pMouseY) {
        return false;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {

        //  super.renderWidget(gui, mouseX, mouseY, delta);

        if (!can) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Words.LockedSuppGemSlot.locName().withStyle(ChatFormatting.RED));
            tooltip.add(Component.empty());
            if (links == null) {
                tooltip.add(Words.NoSocketedSpell.locName().withStyle(ChatFormatting.YELLOW));
            } else {
                if (links.cappedByLevel) tooltip.add(Words.IncreaseYourLevel.locName().withStyle(ChatFormatting.YELLOW));
                if (links.cappedBySpellLevel) tooltip.add(Words.IncreaseSpellLevel.locName().withStyle(ChatFormatting.YELLOW));
            }
            this.setTooltip(Tooltip.create(TextUTIL.mergeList(tooltip)));
            com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, id, getX(), getY(), BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X);
        }
    }


}
