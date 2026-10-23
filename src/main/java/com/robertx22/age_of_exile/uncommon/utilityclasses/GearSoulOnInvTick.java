package com.robertx22.age_of_exile.uncommon.utilityclasses;

import com.robertx22.age_of_exile.database.data.ingredient.SlashIngredient;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftedConsumableData;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftingProcessData;
import com.robertx22.age_of_exile.player_skills.ingredient.data.IngredientData;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulData;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulItem;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class GearSoulOnInvTick {

    public static void checkAndGenerate(Player player) {

        try {

            if (player.level().isClientSide()) {
                return;
            }

            boolean changed = false;

            // items are given after the inventory was updated, otherwise a refund could be
            // overwritten by the stack this method writes back into the same slot
            List<ItemStack> toGive = new ArrayList<>();

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (tryStack(player, stack, toGive)) {
                    player.getInventory().setItem(i, stack.copy());
                    changed = true;
                }
            }

            toGive.forEach(x -> PlayerUtils.giveItem(x, player));

            if (changed) {
                player.inventoryMenu.broadcastChanges();
                if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
                    player.containerMenu.broadcastChanges();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static boolean tryStack(Player player, ItemStack stack, List<ItemStack> toGive) {
        if (stack.isEmpty()) {
            return false;
        }

        if (StackSaving.STAT_SOULS.has(stack)) {
            StatSoulData soul = StackSaving.STAT_SOULS.loadFrom(stack);

            if (soul != null && soul.canInsertIntoStack(stack)) {
                try {
                    GearItemData gear = soul.createGearData(stack);
                    StackNBT.getOrCreate(stack).remove(StatSoulItem.TAG);
                    gear.saveToStack(stack);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (StackSaving.CRAFT_PROCESS.has(stack)) {
            CraftingProcessData pdata = null;
            PlayerSkillEnum skill = null;
            try {
                pdata = StackSaving.CRAFT_PROCESS.loadFrom(stack);
                skill = pdata == null ? null : pdata.getProfession();

                if (pdata == null || skill == null) {
                    // the profession no longer exists (datapack reload) - do not leave a stuck item
                    clearCraftProcess(stack);
                    player.sendSystemMessage(Words.CraftFailed.locName()
                        .withStyle(ChatFormatting.RED));
                    return true;
                }

                if (RandomUtils.roll(pdata.getSuccessChance(player))) {

                    if (skill.isGearCraftingProf()) {
                        GearItemData data = pdata.craftGear(stack, player);
                        clearCraftProcess(stack);
                        StackSaving.GEARS.saveTo(stack, data);
                    } else {
                        CraftedConsumableData data = pdata.craftConsumable(player);
                        clearCraftProcess(stack);
                        StackSaving.CRAFTED_CONSUMABLE.saveTo(stack, data);
                    }

                    int exp = (int) (25 * pdata.getIngredientsCount());
                    Load.playerRPGData(player).professions.addExp(player, skill, exp);

                } else {
                    onCraftFailed(player, stack, pdata, skill, toGive);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                // otherwise the item would stay in the inventory and fail again every tick
                try {
                    clearCraftProcess(stack);
                    if (skill != null && !skill.isGearCraftingProf()) {
                        stack.shrink(1);
                    }
                    player.sendSystemMessage(Words.CraftFailed.locName()
                        .withStyle(ChatFormatting.RED));
                    return true;
                } catch (Exception ignored) {
                }
            }
        }

        return false;
    }

    /**
     * Failure used to always turn the item into gunpowder, which was both confusing and
     * destroyed the base gear. Now the base item survives the failed enchantment, and
     * consumables refund one of the used ingredients.
     */
    static void onCraftFailed(Player player, ItemStack stack, CraftingProcessData pdata, PlayerSkillEnum skill, List<ItemStack> toGive) {
        clearCraftProcess(stack);

        if (skill.isGearCraftingProf()) {
            player.sendSystemMessage(Words.CraftFailedGearKept.locName()
                .withStyle(ChatFormatting.RED));
            return;
        }

        stack.shrink(1);

        IngredientData refund = RandomUtils.randomFromList(pdata.getAllIngredients());

        if (refund != null) {
            SlashIngredient ing = refund.getIngredient();
            if (ing != null) {
                net.minecraft.world.item.Item refundItem = ing.getItem();
                if (refundItem != null && refundItem != net.minecraft.world.item.Items.AIR) {
                    ItemStack refundStack = new ItemStack(refundItem);
                    StackSaving.INGREDIENTS.saveTo(refundStack, refund);
                    toGive.add(refundStack);
                }
            }
        }

        player.sendSystemMessage(Words.CraftFailed.locName()
            .withStyle(ChatFormatting.RED));
    }

    static void clearCraftProcess(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !StackNBT.has(stack)) {
            return;
        }
        CompoundTag tag = StackNBT.getOrCreate(stack);
        tag.remove(StackSaving.CRAFT_PROCESS.GUID());
    }

}

