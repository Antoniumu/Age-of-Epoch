package com.robertx22.age_of_exile.mmorpg.registers.client;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashContainers;
import com.robertx22.age_of_exile.player_skills.crafting_inv.ProfCraftScreen;
import com.robertx22.age_of_exile.player_skills.items.backpacks.BackpackScreen;
import com.robertx22.age_of_exile.player_skills.items.backpacks.mat_pouch.MatBagScreen;
import com.robertx22.age_of_exile.vanilla_mc.blocks.runeword_station.RuneWordStationGui;
import net.minecraft.client.gui.MenuScreens;

public class ContainerGuiRegisters {

    public static void reg() {

        MenuScreens.register(SlashContainers.BACKPACK.get(), BackpackScreen::new);
        MenuScreens.register(SlashContainers.RUNEWORD.get(), RuneWordStationGui::new);
        MenuScreens.register(SlashContainers.MAT_POUCH.get(), MatBagScreen::new);
        MenuScreens.register(SlashContainers.PROF_CRAFTING.get(), ProfCraftScreen::new);

    }

}
