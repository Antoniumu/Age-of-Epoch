package com.robertx22.age_of_exile.uncommon.utilityclasses;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

public class ChatUtils {

    public static boolean isChatOpen() {
        return Minecraft.getInstance().gui != null && Minecraft.getInstance().gui.screen() instanceof ChatScreen;
    }

    public static boolean wasChatOpenRecently() {
        return ClientOnly.ticksSinceChatWasOpened > -5;
    }

}
