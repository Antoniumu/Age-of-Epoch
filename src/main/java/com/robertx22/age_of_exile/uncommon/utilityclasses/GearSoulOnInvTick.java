package com.robertx22.age_of_exile.uncommon.utilityclasses;

import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftedConsumableData;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftingProcessData;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulData;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulItem;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
public class GearSoulOnInvTick {

    public static void checkAndGenerate(Player player) {

        try {

            if (player.level().isClientSide()) {
                return;
            }

            boolean changed = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (tryStack(player, stack)) {
                    player.getInventory().setItem(i, stack.copy());
                    changed = true;
                }
            }
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

    static boolean tryStack(Player player, ItemStack stack) {
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
            try {
                CraftingProcessData pdata = StackSaving.CRAFT_PROCESS.loadFrom(stack);
                if (RandomUtils.roll(pdata.getSuccessChance(player))) {
                    PlayerSkillEnum skill = pdata.getProfession();
                    if (skill.isGearCraftingProf()) {
                        GearItemData data = pdata.craftGear(stack, player);
                        StackNBT.set(stack, new CompoundTag()); // clear the craft process nbt
                        StackSaving.GEARS.saveTo(stack, data);

                        int exp = (int) (25 * pdata.getIngredientsCount());
                        Load.playerRPGData(player).professions.addExp(player, pdata.getProfession(), exp);

                    } else {
                        CraftedConsumableData data = pdata.craftConsumable(player);
                        StackNBT.set(stack, new CompoundTag()); // clear the craft process nbt
                        StackSaving.CRAFTED_CONSUMABLE.saveTo(stack, data);

                        int exp = (int) (25 * pdata.getIngredientsCount());
                        Load.playerRPGData(player).professions.addExp(player, pdata.getProfession(), exp);
                    }

                    // Ежедневная цель: «создать N предметов ремеслом».
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        com.robertx22.age_of_exile.gamemaster.GameMaster.get()
                            .onCrafted(serverPlayer);
                    }

                } else {
                    stack.shrink(1);
                    PlayerUtils.giveItem(new ItemStack(Items.GUNPOWDER), player); // todo give specific fail items for each profession?
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

}
