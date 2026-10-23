package com.robertx22.age_of_exile.mmorpg.registers.common;

import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.player_skills.crafting_inv.ProfCraftContainer;
import com.robertx22.age_of_exile.player_skills.items.backpacks.BackpackContainer;
import com.robertx22.age_of_exile.player_skills.items.backpacks.mat_pouch.MatBagContainer;
import com.robertx22.age_of_exile.vanilla_mc.blocks.runeword_station.RuneWordStationContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.FriendlyByteBuf;
import com.robertx22.forge_compat.common.extensions.IForgeContainerType;

public class SlashContainers {

    public static void init() {

    }

    public static RegObj<MenuType<RuneWordStationContainer>> RUNEWORD = Def.container("runeword", () -> IForgeContainerType.create((int n, Inventory pinv, FriendlyByteBuf buf) -> new RuneWordStationContainer(n, pinv, buf)));
    public static RegObj<MenuType<BackpackContainer>> BACKPACK = Def.container("backpack", () -> IForgeContainerType.create((int n, Inventory pinv, FriendlyByteBuf buf) -> new BackpackContainer(n, pinv, buf)));
    public static RegObj<MenuType<MatBagContainer>> MAT_POUCH = Def.container("pouch", () -> IForgeContainerType.create((int n, Inventory pinv, FriendlyByteBuf buf) -> new MatBagContainer(n, pinv, buf)));
    public static RegObj<MenuType<ProfCraftContainer>> PROF_CRAFTING = Def.container("prof_crafting", () -> IForgeContainerType.create((int n, Inventory pinv, FriendlyByteBuf buf) -> new ProfCraftContainer(n, pinv, buf)));

}
