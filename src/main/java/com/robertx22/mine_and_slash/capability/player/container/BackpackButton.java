package com.robertx22.mine_and_slash.capability.player.container;

import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.OpenBackpackPacket;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.TextUTIL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.Identifier;

import java.util.Arrays;

public class BackpackButton extends ImageButton {

    public static int SX = 16;
    public static int SY = 16;

    Minecraft mc = Minecraft.getInstance();

    public Backpacks.BackpackType type;

    public BackpackButton(Backpacks.BackpackType type, int xPos, int yPos) {
        super(xPos, yPos, SX, SY, 0, 0, SY, Identifier.parse(String.valueOf("empty")), (button) -> {
            Packets.sendToServer(new OpenBackpackPacket(type));
        });
        this.type = type;

    }


    public void renderLegacyUnused(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        setModTooltip();
        super.extractContents(gui, mouseX, mouseY, delta);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        Identifier tex = type.getIcon();
        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, tex, getX(), getY(), SX, SX, SX, SX, SX, SX);

    }

    public void onClick(double pMouseX, double pMouseY) {
        BackpackScreen.iMouseX = Minecraft.getInstance().mouseHandler.xpos();
        BackpackScreen.iMouseY = Minecraft.getInstance().mouseHandler.ypos();
        super.onClick(pMouseX, pMouseY);
    }

    public void setModTooltip() {
        this.setTooltip(Tooltip.create(TextUTIL.mergeList(Arrays.asList(
                this.type.name.locName()
        ))));

    }


}