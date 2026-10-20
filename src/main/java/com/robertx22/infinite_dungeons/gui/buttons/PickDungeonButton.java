package com.robertx22.infinite_dungeons.gui.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.robertx22.infinite_dungeons.database.db_types.layout.DungeonLayout;
import com.robertx22.infinite_dungeons.gui.PickDungeonScreen;
import com.robertx22.infinite_dungeons.main.MainID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;

public class PickDungeonButton extends ImageButton {

    public static int SIZE_X = 29;
    public static int SIZE_Y = 25;

    public DungeonLayout layout;

    static ResourceLocation TEX = MainID.id("textures/gui/select_dungeon_button.png");

    public PickDungeonButton(PickDungeonScreen screen, DungeonLayout layout, int x, int y) {
        super(x, y, SIZE_X, SIZE_Y, 0, 0, 0, TEX, new IPressable() {
            @Override
            public void onPress(Button button) {
                screen.currentLayout = layout;
                screen.init();
            }
        });
        this.layout = layout;
    }

    @Override
    public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {

        super.renderButton(matrices, mouseX, mouseX, delta);

        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager()
            .bind(layout.getIconTextureLoc());
        RenderSystem.enableDepthTest();
        blit(matrices, this.x + 8, this.y + 5, 0, 0, 16, 16, 16, 16);

    }
}
