package com.robertx22.infinite_dungeons.gui.buttons.shop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.infinite_dungeons.database.db_types.DungeonDifficulty;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;

public class ShopDifficultyButton extends ImageButton {

    public static int SIZE_X = 20;
    public static int SIZE_Y = 20;

    DungeonDifficulty diff;

    public ShopDifficultyButton(DungeonDifficulty diff, int x, int y) {
        super(x, y, SIZE_X, SIZE_Y, 0, 0, 0, ResourceLocation.parse(""), button -> {

        });
        this.diff = diff;
    }

    @Override
    public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager()
            .bind(diff.getIconLoc());
        blit(matrices, this.x, this.y, 0, 0, SIZE_X, SIZE_Y, SIZE_X, SIZE_Y);

    }

    @Override
    public void renderToolTip(PoseStack matrix, int x, int y) {
        if (isInside(x, y)) {
            GuiUtils.renderTooltip(matrix,
                diff.getTooltip(), x, y);

        }
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, SIZE_X, SIZE_Y, x, y);
    }

}


