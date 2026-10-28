package com.robertx22.age_of_exile.vanilla_mc.packets.proxies;

import com.robertx22.age_of_exile.gui.screens.character_screen.CharacterScreen;


public class OpenGuiWrapper {

    public static void openMainHub() {

        net.minecraft.client.Minecraft.getInstance().gui
            .setScreen(new CharacterScreen());

    }

    public static void openQuestBook() {
        net.minecraft.client.Minecraft.getInstance().gui
            .setScreen(new com.robertx22.age_of_exile.airship.adventure.AdventureScreen());
    }

}
