package com.robertx22.age_of_exile.player_skills.ingredient;

import com.robertx22.age_of_exile.database.data.crafting_req.CraftingReq;
import com.robertx22.age_of_exile.database.data.ingredient.SlashIngredient;
import com.robertx22.age_of_exile.player_skills.crafting_inv.ProfCraftResult;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftSlotData;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftingProcessData;
import com.robertx22.age_of_exile.player_skills.ingredient.data.IngredientData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProfCraftingRecipe {

    public static ProfCraftResult canCraft(Player player, CraftingContainer inv, SimpleContainer resultinv, PlayerSkillEnum skill) {

        if (player == null || inv == null || skill == null) {
            return ProfCraftResult.fail(Words.WrongIngredient);
        }

        List<IngredientData> list = new ArrayList<>();

        if (skill.isGearCraftingProf()) {
            int gearCount = 0;
            boolean hasgearwithdata = false;
            ItemStack gearStack = ItemStack.EMPTY;

            for (int i = 0; i < inv.getContainerSize(); ++i) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {

                    if (StackSaving.GEARS.has(stack)) {
                        hasgearwithdata = true;
                        break;
                    }

                    if (skill.gearMatchesProfession(stack.getItem())) {
                        gearCount++;
                        gearStack = stack;
                    }

                }
            }
            if (hasgearwithdata) {
                return ProfCraftResult.fail(Words.GearHasData);
            }
            if (gearCount == 0) {
                return ProfCraftResult.fail(Words.NoGear);
            }
            if (gearCount > 1) {
                return ProfCraftResult.fail(Words.CantUseMoreOf);
            }

            ProfCraftResult reqResult = canUseGear(player, gearStack, skill);
            if (!reqResult.canCraft) {
                return reqResult;
            }
        }

        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {

                if (skill.isGearCraftingProf()) {
                    if (skill.gearMatchesProfession(stack.getItem())) {
                        continue;
                    }
                }

                if (!StackSaving.INGREDIENTS.has(stack)) {
                    return ProfCraftResult.fail(Words.WrongIngredient);
                }
            }
        }

        HashMap<String, Integer> countmap = new HashMap<>();

        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (StackSaving.INGREDIENTS.has(stack)) {
                IngredientData data = StackSaving.INGREDIENTS.loadFrom(stack);
                SlashIngredient ingredient = data == null ? null : data.getIngredient();

                if (ingredient == null) {
                    // the ingredient was removed from the datapack or the item data is broken
                    return ProfCraftResult.fail(Words.WrongRecipeIngredient);
                }

                if (!ingredient.isAllowedInProfession(skill.id)) {
                    return ProfCraftResult.fail(Words.WrongRecipeIngredient);
                }
                if (ingredient.isOneOfAKind()) {
                    countmap.put(ingredient.getOneOfAKindId(), countmap.getOrDefault(ingredient.getOneOfAKindId(), 0) + 1);
                }
                if (countmap.getOrDefault(ingredient.getOneOfAKindId(), 0) > 1) {
                    return ProfCraftResult.fail(Words.CantUseMoreOf);
                }
                list.add(data);
            }
        }

        if (list.isEmpty()) {
            return ProfCraftResult.fail(Words.NoIngredients);
        }

        if (list.size() > 6) {
            return ProfCraftResult.fail(Words.NoIngredients);
        }

        return ProfCraftResult.success();

    }

    /**
     * Some items (datapack driven) require a minimum level in a profession before they can be used
     * as a crafting base. Before this check existed the whole CraftingReq registry was dead code.
     */
    private static ProfCraftResult canUseGear(Player player, ItemStack gearStack, PlayerSkillEnum skill) {
        if (gearStack == null || gearStack.isEmpty()) {
            return ProfCraftResult.success();
        }

        CraftingReq req = CraftingReq.getFor(gearStack.getItem());

        if (req == null) {
            return ProfCraftResult.success();
        }

        // the requirement stores its own profession, fall back to the used tool's one
        String skillId = req.skill == null || req.skill.isEmpty() ? skill.id : req.skill;

        int current = Load.playerRPGData(player).professions
            .getDataFor(skillId)
            .getLvl();

        if (current >= req.lvl) {
            return ProfCraftResult.success();
        }

        return ProfCraftResult.fail(Words.CraftLevelTooLow, req.lvl, current);
    }

    public static ItemStack craftResult(CraftingContainer inv, SimpleContainer resultinv, PlayerSkillEnum skill) {

        if (inv == null || skill == null) {
            return ItemStack.EMPTY;
        }

        List<CraftSlotData> list = new ArrayList<>();

        int ingredientCount = 0;

        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);

            CraftSlotData slot = new CraftSlotData();
            slot.place = i;

            if (StackSaving.INGREDIENTS.has(stack)) {
                IngredientData data = StackSaving.INGREDIENTS.loadFrom(stack);

                slot.ing = data;

                if (data != null) {
                    ingredientCount++;
                }

            }

            list.add(slot);
        }

        if (ingredientCount < 1) {
            return ItemStack.EMPTY;
        }

        CraftingProcessData data = new CraftingProcessData();
        data.prof = skill.GUID();
        data.ingredients = list;

        ItemStack stack = ItemStack.EMPTY;

        if (skill.isGearCraftingProf()) {
            for (int i = 0; i < inv.getContainerSize(); ++i) {
                ItemStack gear = inv.getItem(i);
                if (!gear.isEmpty()) {
                    if (skill.gearMatchesProfession(gear.getItem())) {
                        stack = gear.copyWithCount(1);
                        break;
                    }
                }
            }

        } else {
            net.minecraft.world.item.Item resultItem = skill.getCraftResultItem();
            if (resultItem == null) {
                return ItemStack.EMPTY;
            }
            stack = new ItemStack(resultItem);
        }

        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        StackSaving.CRAFT_PROCESS.saveTo(stack, data);
        return stack;
    }

}
