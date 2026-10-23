package com.robertx22.age_of_exile.mmorpg.registers.client;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashContainers;
import com.robertx22.age_of_exile.player_skills.crafting_inv.ProfCraftScreen;
import com.robertx22.age_of_exile.player_skills.items.backpacks.BackpackScreen;
import com.robertx22.age_of_exile.player_skills.items.backpacks.mat_pouch.MatBagScreen;
import com.robertx22.age_of_exile.vanilla_mc.blocks.runeword_station.RuneWordStationGui;
import com.robertx22.forge_compat.client.MenuScreensCompat;

public class ContainerGuiRegisters {

    public static void reg() {

        MenuScreensCompat.register(SlashContainers.BACKPACK.get(), BackpackScreen::new);
        MenuScreensCompat.register(SlashContainers.RUNEWORD.get(), RuneWordStationGui::new);
        MenuScreensCompat.register(SlashContainers.MAT_POUCH.get(), MatBagScreen::new);
        MenuScreensCompat.register(SlashContainers.PROF_CRAFTING.get(), ProfCraftScreen::new);

    }

}
