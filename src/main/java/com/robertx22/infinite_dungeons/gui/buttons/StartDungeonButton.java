package com.robertx22.infinite_dungeons.gui.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.infinite_dungeons.gui.PickDungeonScreen;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.infinite_dungeons.packets.StartDungeonPacket;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class StartDungeonButton extends ImageButton {

    public static int SIZE_X = 80;
    public static int SIZE_Y = 16;

    static Identifier ID = MainID.id("textures/gui/start_dungeon.png");

    Minecraft mc = Minecraft.getInstance();

    PickDungeonScreen screen;

    public StartDungeonButton(PickDungeonScreen screen, int xPos, int yPos) {
        super(xPos, yPos, SIZE_X, SIZE_Y, 0, 0, SIZE_Y, ID, (button) -> {
            Packets.sendToServer(new StartDungeonPacket(screen.currentLayout.GUID(), screen.difficulty.GUID()));
            screen.onClose();
        });
        this.screen = screen;

    }

    @Override
    public void renderToolTip(PoseStack matrix, int x, int y) {
        if (isInside(x, y)) {
            List<Component> tooltip = new ArrayList<>();

            GuiUtils.renderTooltip(matrix, tooltip, x, y);

        }
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, SIZE_X, SIZE_Y, x, y);
    }

    @Override
    public void renderButton(PoseStack matrix, int x, int y, float f) {

        super.renderButton(matrix, x, y, f);

        String txt = "Start Dungeon";

        com.robertx22.forge_compat.client.AbstractGui.drawShadow(mc.font, matrix, txt, this.x + (SIZE_X / 2F - (mc.font.width(txt) / 2F)), this.y + 3, com.robertx22.forge_compat.common.CompatChatFormatting.getColor(ChatFormatting.WHITE));
    }

}
