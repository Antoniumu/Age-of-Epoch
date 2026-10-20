package com.robertx22.age_of_exile.gui.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.BaseModificationStation;
import com.robertx22.library_of_exile.utils.CLOC;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class OwnerButton extends ImageButton {

    public static int SIZE_X = 80;
    public static int SIZE_Y = 16;

    static ResourceLocation ID = SlashRef.guiId("owner_button");

    Minecraft mc = Minecraft.getInstance();

    BaseModificationStation tile;

    public OwnerButton(BaseModificationStation tile, int xPos, int yPos) {
        super(xPos, yPos, SIZE_X, SIZE_Y, 0, 0, SIZE_Y, ID, (button) -> {
        });
        this.tile = tile;

    }

    @Override
    public void renderToolTip(PoseStack matrix, int x, int y) {
        if (isInside(x, y)) {
            List<Component> tooltip = new ArrayList<>();

            tooltip.add(Component.literal("This is the owner of the station."));
            tooltip.add(Component.literal("The station can craft things based on the owner's skill levels."));
            tooltip.add(Component.literal("Owner gets experience when things are crafted here."));
            tooltip.add(Component.literal("The station won't work if the owner is offline."));

            GuiUtils.renderTooltip(matrix, tooltip, x, y);

        }
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, SIZE_X, SIZE_Y, x, y);
    }

    @Override
    public void renderButton(PoseStack matrix, int x, int y, float f) {

        super.renderButton(matrix, x, y, f);

        String txt = "";

        Player owner = tile.getOwner();

        if (owner != null) {
            txt = CLOC.translate(owner.getDisplayName());
        } else {

            txt = "No Owner";
        }

        int width = mc.font.width(txt);

        boolean cut = false;

        while (width > (SIZE_X - 10)) {
            cut = true;

            txt = txt.substring(0, txt.length() - 2);

            width = mc.font.width(txt);

        }
        if (cut) {
            txt += "...";
        }

        mc.font.drawShadow(matrix, txt, this.x + (SIZE_X / 2F - (mc.font.width(txt) / 2F)), this.y + 3, ChatFormatting.WHITE.getColor());
    }

}