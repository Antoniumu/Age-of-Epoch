package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.robertx22.forge_compat.client.MenuScreensCompat;
import com.robertx22.mine_and_slash.capability.player.container.BackpackScreen;
import com.robertx22.mine_and_slash.capability.player.container.SkillGemsScreen;
import com.robertx22.mine_and_slash.database.data.profession.all.Professions;
import com.robertx22.mine_and_slash.database.data.profession.screen.*;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashContainers;

public class ContainerGuiRegisters {
    public static void reg() {
        MenuScreensCompat.register(SlashContainers.STATIONS.get(Professions.SALVAGING).get(), SalvagingScreen::new);
        MenuScreensCompat.register(SlashContainers.STATIONS.get(Professions.GEAR_CRAFTING).get(), GearCraftingScreen::new);
        MenuScreensCompat.register(SlashContainers.STATIONS.get(Professions.ALCHEMY).get(), AlchemyScreen::new);
        MenuScreensCompat.register(SlashContainers.STATIONS.get(Professions.INFUSING).get(), InfusingScreen::new);
        MenuScreensCompat.register(SlashContainers.STATIONS.get(Professions.COOKING).get(), CookingScreen::new);
        MenuScreensCompat.register(SlashContainers.SKILL_GEMS.get(), SkillGemsScreen::new);
        MenuScreensCompat.register(SlashContainers.BACKPACK.get(), BackpackScreen::new);
    }
}
