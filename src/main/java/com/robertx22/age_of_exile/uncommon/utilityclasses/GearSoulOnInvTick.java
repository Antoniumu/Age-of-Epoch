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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;

public class GearSoulOnInvTick {

    public static void checkAndGenerate(Player player) {

        try {

            if (player.level().isClientSide()) {
                return;
            }

            // Предметы, которые нужно выдать игроку (порох при провале, готовые предметы из стаков).
            // Выдаём их ПОСЛЕ записи всех слотов: иначе giveItem может положить предмет в слот,
            // который мы потом перезаписали бы старым (уже пустым) стаком, и предмет пропал бы.
            List<ItemStack> refunds = new ArrayList<>();

            boolean changed = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                ItemStack replacement = process(player, stack, refunds);
                if (replacement != null) {
                    player.getInventory().setItem(i, replacement);
                    changed = true;
                }
            }

            for (ItemStack refund : refunds) {
                PlayerUtils.giveItem(refund, player);
            }

            if (changed || !refunds.isEmpty()) {
                player.inventoryMenu.broadcastChanges();
                if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
                    player.containerMenu.broadcastChanges();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Возвращает новое содержимое слота, или null если слот менять не нужно.
     */
    static ItemStack process(Player player, ItemStack stack, List<ItemStack> refunds) {
        if (stack.isEmpty()) {
            return null;
        }

        if (StackSaving.STAT_SOULS.has(stack)) {
            StatSoulData soul = StackSaving.STAT_SOULS.loadFrom(stack);

            if (soul != null && soul.canInsertIntoStack(stack)) {
                try {
                    GearItemData gear = soul.createGearData(stack);
                    StackNBT.getOrCreate(stack).remove(StatSoulItem.TAG);
                    gear.saveToStack(stack);
                    return stack.copy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        if (StackSaving.CRAFT_PROCESS.has(stack)) {
            // Бросаем шанс по одной штуке: стак одинаковых предметов не должен получить один бросок на все
            ItemStack single = stack.copyWithCount(1);
            ItemStack out = craftOne(player, single, refunds);
            if (stack.getCount() <= 1) {
                return out;
            }
            if (!out.isEmpty()) {
                refunds.add(out);
            }
            return stack.copyWithCount(stack.getCount() - 1);
        }

        return null;
    }

    /**
     * Обрабатывает один предмет незавершённого крафта.
     * Возвращает готовый предмет, либо EMPTY (провал / ошибка: предмет исчезает, игрок получает порох).
     */
    private static ItemStack craftOne(Player player, ItemStack stack, List<ItemStack> refunds) {
        try {
            CraftingProcessData pdata = StackSaving.CRAFT_PROCESS.loadFrom(stack);
            if (pdata == null || pdata.getProfession() == null || pdata.getIngredientsCount() <= 0) {
                // Битые данные: предмет нельзя ни сделать, ни оставить — иначе он висит в инвентаре вечно
                refunds.add(new ItemStack(Items.GUNPOWDER));
                return ItemStack.EMPTY;
            }

            if (!RandomUtils.roll(pdata.getSuccessChance(player))) {
                refunds.add(new ItemStack(Items.GUNPOWDER)); // todo give specific fail items for each profession?
                return ItemStack.EMPTY;
            }

            PlayerSkillEnum skill = pdata.getProfession();
            ItemStack out;
            if (skill.isGearCraftingProf()) {
                GearItemData data = pdata.craftGear(stack, player);
                if (data == null) {
                    refunds.add(new ItemStack(Items.GUNPOWDER));
                    return ItemStack.EMPTY;
                }
                out = stack.copy();
                StackNBT.set(out, new CompoundTag()); // clear the craft process nbt
                StackSaving.GEARS.saveTo(out, data);
            } else {
                CraftedConsumableData data = pdata.craftConsumable(player);
                out = stack.copy();
                StackNBT.set(out, new CompoundTag()); // clear the craft process nbt
                StackSaving.CRAFTED_CONSUMABLE.saveTo(out, data);
            }

            int exp = (int) (25 * pdata.getIngredientsCount());
            Load.playerRPGData(player).professions.addExp(player, skill, exp);

            // Ежедневная цель: «создать N предметов ремеслом».
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                com.robertx22.age_of_exile.gamemaster.GameMaster.get()
                    .onCrafted(serverPlayer);
            }

            return out;
        } catch (Exception e) {
            e.printStackTrace();
            refunds.add(new ItemStack(Items.GUNPOWDER));
            return ItemStack.EMPTY;
        }
    }

}
