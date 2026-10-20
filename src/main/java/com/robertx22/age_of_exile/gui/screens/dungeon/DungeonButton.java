package com.robertx22.age_of_exile.gui.screens.dungeon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.dimension.dungeon_data.DungeonData;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class DungeonButton extends ImageButton {

    public static int xSize = 32;
    public static int ySize = 32;

    static ResourceLocation LOC = SlashRef.guiId("dungeon_button");
    static ResourceLocation ICON = SlashRef.guiId("dungeon/icons/nether");

    Minecraft mc = Minecraft.getInstance();

    DungeonData dungeon;

    public DungeonButton(DungeonData dungeon, int xPos, int yPos) {
        super(xPos + 1, yPos + 1, xSize, ySize, 0, 0, ySize + 1, LOC, (button) -> {
        });

        this.dungeon = dungeon;
    }

    @Override
    public void renderButton(PoseStack matrix, int x, int y, float ticks) {
        super.renderButton(matrix, x, y, ticks);

        // icon
        mc.getTextureManager()
            .bind(ICON);
        blit(matrix, this.x, this.y, 0, 0, 32, 32, 32, 32);

    }

    @Override
    public void renderToolTip(PoseStack matrix, int x, int y) {
        if (isInside(x, y)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.addAll(dungeon.getTooltip());
            GuiUtils.renderTooltip(matrix, tooltip, x, y);
        }

    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, xSize, ySize, x, y);
    }

}
