package com.robertx22.age_of_exile.player_skills.crafting_inv;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ProfCraftResultSlot extends Slot {
    private final ProfCraftContainer menu;
    private final CraftingContainer craftSlots;
    private final Player player;

    public ProfCraftResultSlot(Player p, CraftingContainer craftInv, Container inv, int num, int x, int y) {
        this(null, p, craftInv, inv, num, x, y);
    }

    public ProfCraftResultSlot(ProfCraftContainer menu, Player p, CraftingContainer craftInv, Container inv, int num, int x, int y) {
        super(inv, num, x, y);
        this.menu = menu;
        this.player = p;
        this.craftSlots = craftInv;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    void useIngredients() {
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack ing = craftSlots.getItem(i);
            if (!ing.isEmpty()) {
                ing.shrink(1);
                if (ing.isEmpty()) {
                    craftSlots.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        craftSlots.setChanged();
    }

    @Override
    public void onTake(Player p, ItemStack stack) {
        useIngredients();
        super.onTake(p, stack);
        if (menu != null) {
            menu.slotsChanged(craftSlots);
        }
    }
}
