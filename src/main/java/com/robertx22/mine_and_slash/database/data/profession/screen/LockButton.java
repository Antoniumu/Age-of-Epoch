package com.robertx22.mine_and_slash.database.data.profession.screen;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import com.robertx22.mine_and_slash.vanilla_mc.packets.LockTogglePacket;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class LockButton extends ImageButton {

    public static int XS = 18;
    public static int YS = 18;


    Minecraft mc = Minecraft.getInstance();
    CraftingStationScreen s;

    public LockButton(int xPos, int yPos, CraftingStationScreen s) {
        super(xPos, yPos, XS, YS, 0, s.getSyncedData().recipe_locked ? 18 : 0, YS, SlashRef.guiId("lockbutton"), (button) -> {
            Packets.sendToServer(new LockTogglePacket(s.getSyncedData().getBlockPos()));
        });
        this.s = s;
    }

    public void renderLegacyUnused(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        setModTooltip();
        super.extractWidgetRenderState(gui, mouseX, mouseY, delta);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        Identifier tex = SlashRef.guiId("lockbutton");
        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, tex, getX(), getY(), 0, s.getSyncedData().recipe_locked ? 18 : 0, 18, 18);

        //super.renderWidget(gui, mouseX, mouseY, delta);
    }

    public void setModTooltip() {
        if (s.getSyncedData().recipe_locked)
            this.setTooltip(Tooltip.create(Gui.STATION_UNLOCK_RECIPE.locName().withStyle(ChatFormatting.DARK_AQUA)));
        else
            this.setTooltip(Tooltip.create(Gui.STATION_LOCK_RECIPE.locName().withStyle(ChatFormatting.DARK_AQUA)));
    }

}
