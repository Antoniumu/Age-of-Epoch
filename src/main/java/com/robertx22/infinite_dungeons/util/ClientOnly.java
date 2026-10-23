package com.robertx22.infinite_dungeons.util;

import com.robertx22.infinite_dungeons.gui.DungeonShopScreen;
import com.robertx22.infinite_dungeons.gui.PickDungeonScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ClientOnly {

    public static Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    public static void openPickDungeonScreen() {
        Minecraft.getInstance().gui
            .setScreen(new PickDungeonScreen());
    }

    public static void openDungeonShopScreen() {
        Minecraft.getInstance().gui
            .setScreen(new DungeonShopScreen());
    }
}
