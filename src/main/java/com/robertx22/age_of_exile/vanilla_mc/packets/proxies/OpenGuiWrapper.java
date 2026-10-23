package com.robertx22.age_of_exile.vanilla_mc.packets.proxies;

import com.robertx22.age_of_exile.gui.screens.character_screen.CharacterScreen;
import com.robertx22.age_of_exile.questbook.QuestBookScreen;

public class OpenGuiWrapper {

    public static void openMainHub() {

        net.minecraft.client.Minecraft.getInstance().gui
            .setScreen(new CharacterScreen());

    }

    public static void openQuestBook() {
        net.minecraft.client.Minecraft.getInstance().gui
            .setScreen(new QuestBookScreen());
    }

}
