package com.robertx22.age_of_exile.player_skills.ingredient;

import com.robertx22.age_of_exile.player_skills.crafting_inv.ProfCraftResult;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftSlotData;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftingProcessData;
import com.robertx22.age_of_exile.player_skills.ingredient.data.IngredientData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.OnScreenMessageUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import com.robertx22.forge_compat.network.ClientboundTitleCompatPacket;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProfCraftingRecipe {

    public static ProfCraftResult canCraft(Player player, CraftingContainer inv, SimpleContainer resultinv, PlayerSkillEnum skill) {
        List<IngredientData> list = new ArrayList<>();

        if (skill.isGearCraftingProf()) {
            int gearCount = 0;
            boolean hasgearwithdata = false;

            for (int i = 0; i < inv.getContainerSize(); ++i) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {

                    if (StackSaving.GEARS.has(stack)) {
                        hasgearwithdata = true;
                        break;
                    }

                    if (skill.gearMatchesProfession(stack.getItem())) {
                        gearCount++;
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
                if (data != null) {
                    if (!data.getIngredient()
                        .isAllowedInProfession(skill.id)) {
                        return ProfCraftResult.fail(Words.WrongRecipeIngredient);
                    }
                    if (data.getIngredient()
                        .isOneOfAKind()) {
                        countmap.put(data.getIngredient()
                            .getOneOfAKindId(), countmap.getOrDefault(data.getIngredient()
                            .getOneOfAKindId(), 0) + 1);
                    }
                    if (countmap.getOrDefault(data.getIngredient()
                        .getOneOfAKindId(), 0) > 1) {
                        return ProfCraftResult.fail(Words.CantUseMoreOf);
                    }
                    list.add(data);
                }
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

    public static ItemStack craftResult(CraftingContainer inv, SimpleContainer resultinv, PlayerSkillEnum skill) {
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
            stack = new ItemStack(skill.getCraftResultItem());
        }

        StackSaving.CRAFT_PROCESS.saveTo(stack, data);
        return stack;
    }

}