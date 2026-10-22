package com.robertx22.mine_and_slash.prophecy.gui;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.prophecy.AcceptProphecyPacket;
import com.robertx22.mine_and_slash.prophecy.ProphecyData;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.TextUTIL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.Identifier;

public class ProphecyButton extends ImageButton {
    static Identifier DEFAULT_ID = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/prophecy/icon.png");

    ProphecyData data;
    Identifier iconTexture;


    public ProphecyButton(ProphecyData data, boolean canTake, int x, int y) {
        super(x, y, 16, 16, 0, 0, 1, DEFAULT_ID, (action) -> {
            if (canTake) {
                Packets.sendToServer(new AcceptProphecyPacket(data.uuid));
                Minecraft.getInstance().gui.setScreen(null);
            }
        });
        this.data = data;
        this.iconTexture = getIconForProphecyType(data);
    }

    private Identifier getIconForProphecyType(ProphecyData data) {
        String iconPath = "textures/gui/prophecy/" + data.start + ".png";

        return Identifier.fromNamespaceAndPath(SlashRef.MODID, iconPath);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(pGuiGraphics, iconTexture, getX(), getY(), 0, 0, 16, 16, 16, 16);

        this.setTooltip(Tooltip.create(TextUTIL.mergeList(data.getTooltip())));

        super.extractWidgetRenderState(pGuiGraphics, pMouseX, pMouseY, pPartialTick);


    }
}