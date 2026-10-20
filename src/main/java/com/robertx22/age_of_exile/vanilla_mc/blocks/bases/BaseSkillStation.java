package com.robertx22.age_of_exile.vanilla_mc.blocks.bases;

import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.database.data.crafting_req.CraftingReq;
import com.robertx22.age_of_exile.database.data.player_skills.PlayerSkill;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.vanilla_mc.blocks.IAutomatable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;

import net.minecraft.core.registries.BuiltInRegistries;
public abstract class BaseSkillStation extends BaseModificationStation implements IAutomatable, ISmeltingStation {

    PlayerSkillEnum skill;
    RecipeType<? extends Recipe<Container>> recipeType;

    public BaseSkillStation(RecipeType<? extends Recipe<Container>> recipeType, PlayerSkillEnum skill, BlockEntityType<?> type, int slots) {
        super(type, slots);
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
        return cook_ticks / (20F * 8F); // todo setup cook time in recipe
    }

    public float getFuelProgress() {
        return fuel / 2000F;
    }

    public void burnFuelIfNeeded() {
        if (!hasFuel()) {

            getFuelSlots().forEach(x -> {
                ItemStack stack = itemStacks[x];

                int val = AbstractFurnaceBlockEntity.getFuel()
                    .getOrDefault(stack.getItem(), 0);

                if (val > 0) {
                    stack.shrink(1);
                    fuel += val;
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

    public Recipe<Container> getRecipeBeingTried() {

        Container inv = getCraftingInventory();

        Recipe<Container> recipe = this.level.getRecipeManager()
            .getRecipeFor(recipeType, inv, this.level)
            .orElse(null);

        return recipe;
    }

    public Container getCraftingInventory() {
        ItemStack[] craftinv = new ItemStack[getInputSlots().size()];
        int i = 0;
        for (Integer INPUT_SLOT : getInputSlots()) {
            craftinv[i] = itemStacks[INPUT_SLOT];
            i++;
        }
        Container inv = new SimpleContainer(craftinv);

        return inv;
    }

    public ItemStack getStackBeingCrafted() {
        Recipe<Container> recipe = getRecipeBeingTried();
        if (recipe != null) {
            return recipe.getResultItem();
        } else {
            return ItemStack.EMPTY;
        }
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

        Player player = getOwner();

        if (player != null) {

            Container inv = getCraftingInventory();

            Recipe<Container> recipe = getRecipeBeingTried();

            if (recipe != null) {

                ItemStack output = recipe.assemble(inv);

                CraftingReq req = null;

                String key = BuiltInRegistries.ITEM.getKey(output.getItem())
                    .toString();

                if (ExileDB.ItemCraftReq()
                    .isRegistered(key)) {
                    req = ExileDB.ItemCraftReq()
                        .get(key);
                }

                if (req != null) {
                    if (!req.meets(player)) {
                        cook_ticks--;
                        if (cook_ticks < 0) {
                            cook_ticks = 0;
                        }
                        return;
                    }
                }

                burnFuelIfNeeded();

                if (!hasFuel()) {
                    cook_ticks--;
                    if (cook_ticks < 0) {
                        cook_ticks = 0;
                    }
                    return;
                }

                for (Integer x : getOutputSlots()) {
                    if (canPushTo(x, output)) {

                        if (getCookProgress() < 1F) {
                            cook_ticks++;
                            return;
                        } else {
                            cook_ticks = 0;
                        }

                        this.expEarned += 1; // todo

                        getInputSlots().stream()
                            .forEach(e -> {
                                itemStacks[e].shrink(1);
                            });

                        if (skill != PlayerSkillEnum.NONE) {

                            PlayerSkill skill = ExileDB.PlayerSkills()
                                .get(this.skill.id);

                            RPGPlayerData skills = Load.playerRPGData(player);

                            int exp = skill.getExpForCraft(output, player);

                            skills.professions.addExp(player, skill.type_enum, exp);
                        }

                        pushTo(x, output);
                    }
                }
            } else {
                cook_ticks--;
                if (cook_ticks < 0) {
                    cook_ticks = 0;
                }
            }
        }

    }

}
