package com.robertx22.infinite_dungeons.gui.buttons.shop;

import com.robertx22.infinite_dungeons.main.MainID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;

public class ShopArrowButton extends ImageButton {

    public static int SIZE_X = 24;
    public static int SIZE_Y = 24;

    static ResourceLocation ID = MainID.id("textures/gui/arrow_right.png");

    Minecraft mc = Minecraft.getInstance();

    public ShopArrowButton(int xPos, int yPos) {
        super(xPos, yPos, SIZE_X, SIZE_Y, 0, 0, SIZE_Y + 1, ID, (button) -> {
        });

    }

}


