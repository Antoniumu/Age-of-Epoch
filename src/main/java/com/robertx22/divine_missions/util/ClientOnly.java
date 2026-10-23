package com.robertx22.divine_missions.util;

import com.robertx22.divine_missions.gui.MissionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ClientOnly {
    public static Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    public static void openMissionsScreen() {
        Minecraft.getInstance().gui
            .setScreen(new MissionsScreen());
    }
}
