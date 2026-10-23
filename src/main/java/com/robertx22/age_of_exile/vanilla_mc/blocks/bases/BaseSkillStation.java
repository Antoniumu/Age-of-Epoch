package com.robertx22.age_of_exile.vanilla_mc.blocks.bases;

import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.vanilla_mc.blocks.IAutomatable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseSkillStation extends BaseModificationStation implements IAutomatable, ISmeltingStation {

    PlayerSkillEnum skill;
    RecipeType<? extends Recipe<? extends RecipeInput>> recipeType;

    public BaseSkillStation(RecipeType<? extends Recipe<? extends RecipeInput>> recipeType, PlayerSkillEnum skill, BlockEntityType<?> type, int slots, BlockPos pos, BlockState state) {
        super(type, slots, pos, state);
        this.skill = skill;
        this.recipeType = recipeType;
    }

    @Override
    public boolean modifyItem(int number, Player player) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }

    public float getCookProgress() {
        return cook_ticks / (20F * 8F);
    }

    public float getFuelProgress() {
        return fuel / 2000F;
    }

    public void burnFuelIfNeeded() {
        if (!hasFuel()) {
            getFuelSlots().forEach(x -> {
                ItemStack stack = itemStacks[x];
                if (!stack.isEmpty()) {
                    stack.shrink(1);
                    fuel += 200;
                }
            });
        }
    }

    public boolean hasFuel() {
        if (!needsFuel()) {
            return true;
        }
        return fuel > 0;
    }

    public boolean needsFuel() {
        return !getFuelSlots().isEmpty();
    }

    public Container getCraftingInventory() {
        ItemStack[] craftinv = new ItemStack[getInputSlots().size()];
        int i = 0;
        for (Integer INPUT_SLOT : getInputSlots()) {
            craftinv[i] = itemStacks[INPUT_SLOT];
            i++;
        }
        return new SimpleContainer(craftinv);
    }

    public ItemStack getStackBeingCrafted() {
        return ItemStack.EMPTY;
    }

    public void reduceFuel() {
        fuel--;
        if (fuel < 0) {
            fuel = 0;
        }
    }

    @Override
    public void onSmeltTick() {
        reduceFuel();
    }
}
