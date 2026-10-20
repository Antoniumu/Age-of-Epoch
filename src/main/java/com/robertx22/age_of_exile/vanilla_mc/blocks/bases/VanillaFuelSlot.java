package com.robertx22.age_of_exile.vanilla_mc.blocks.bases;

import com.robertx22.forge_compat.fml.server.ServerLifecycleHooks;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class VanillaFuelSlot extends Slot {
    public VanillaFuelSlot(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (ServerLifecycleHooks.getCurrentServer() != null && ServerLifecycleHooks.getCurrentServer().overworld() != null) {
            return ServerLifecycleHooks.getCurrentServer().overworld().fuelValues().isFuel(stack);
        }
        return !stack.isEmpty();
    }

}
