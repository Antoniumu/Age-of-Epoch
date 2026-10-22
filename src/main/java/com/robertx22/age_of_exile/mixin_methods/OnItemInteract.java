package com.robertx22.age_of_exile.mixin_methods;

import com.robertx22.age_of_exile.database.data.currency.base.ICurrencyItemEffect;
import com.robertx22.age_of_exile.database.data.currency.loc_reqs.LocReqContext;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulData;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulItem;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.ISalvagable;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.SalvagedDustItem;
import com.robertx22.library_of_exile.utils.SoundUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class OnItemInteract {

    public static void on(AbstractContainerMenu screen, int i, int j, ContainerInput slotActionType, Player player, CallbackInfoReturnable<ItemStack> ci) {

        if (slotActionType != ContainerInput.PICKUP) {
            return;
        }

        ItemStack cursor = screen.getCarried();

        if (!cursor.isEmpty()) {

            Slot slot = null;
            try {
                slot = screen.slots.get(i);
            } catch (Exception e) {
            }
            if (slot == null || !slot.hasItem() || !slot.mayPickup(player)
                || slot instanceof net.minecraft.world.inventory.ResultSlot
                || slot instanceof net.minecraft.world.inventory.FurnaceResultSlot
                || slot instanceof com.robertx22.age_of_exile.player_skills.crafting_inv.ProfCraftResultSlot
                || slot.container instanceof net.minecraft.world.inventory.ResultContainer) {
                return;
            }

            ItemStack stack = slot.getItem();

            boolean success = false;

            if (stack.isDamaged() && cursor.getItem() instanceof SalvagedDustItem) {

                GearItemData gear = Gear.Load(stack);

                if (gear == null) {
                    return;
                }

                SalvagedDustItem essence = (SalvagedDustItem) cursor.getItem();

                SoundUtils.playSound(player, SoundEvents.ANVIL_USE, 1, 1);

                int repair = essence.durabilityRepair;

                if (gear.getTier() > essence.tier.tier) {
                    repair /= 5;
                }
                ItemStack copy = stack.copy();
                copy.setDamageValue(Math.max(0, copy.getDamageValue() - repair));
                slot.set(copy);
                success = true;

            } else if (cursor.getItem() instanceof StatSoulItem) {
                StatSoulData data = StackSaving.STAT_SOULS.loadFrom(cursor);

                if (data == null && !stack.isEmpty() && !Gear.has(stack)) {
                    GearSlot targetSlot = GearSlot.getSlotOf(stack.getItem());
                    if (targetSlot != null) {
                        data = new StatSoulData();
                        data.slot = targetSlot.GUID();
                        int playerLvl = 1;
                        try {
                            playerLvl = Math.max(1, Load.Unit(player).getLevel());
                        } catch (Exception ignored) {
                        }
                        data.tier = LevelUtils.levelToTier(playerLvl);
                        GearRarity rar = ExileDB.GearRarities().random();
                        data.rar = rar != null ? rar.GUID() : "common";
                    }
                }

                if (data != null) {
                    if (data.canInsertIntoStack(stack)) {
                        ItemStack copy = stack.copy();
                        data.insertAsUnidentifiedOn(copy, player);
                        slot.set(copy);
                        success = true;
                    }
                }
            } else if (cursor.getItem() instanceof ICurrencyItemEffect) {
                LocReqContext ctx = new LocReqContext(player, stack.copy(), cursor);
                if (ctx.effect.canItemBeModified(ctx)) {
                    ItemStack result = ctx.effect.modifyItem(ctx).stack;
                    if (stack.getCount() == 1) {
                        slot.set(result.copy());
                    } else {
                        ItemStack copy = stack.copy();
                        copy.shrink(1);
                        slot.set(copy);
                        if (!player.level().isClientSide()) {
                            PlayerUtils.giveItem(result.copy(), player);
                        }
                    }
                    success = true;
                }
            } else if (cursor.getItem() == SlashItems.SALVAGE_HAMMER.get()) {
                ISalvagable data = ISalvagable.load(stack);

                if (data == null && stack.getItem() instanceof ISalvagable) {
                    data = (ISalvagable) stack.getItem();
                }

                if (data != null) {
                    SoundUtils.playSound(player, SoundEvents.ANVIL_USE, 1, 1);

                    ItemStack salvagedStack = stack.copy();
                    ItemStack remaining = stack.copy();
                    remaining.shrink(1);
                    slot.set(remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                    if (!player.level().isClientSide()) {
                        data.getSalvageResult(salvagedStack)
                            .forEach(x -> PlayerUtils.giveItem(x, player));
                    }
                    slot.setChanged();
                    ci.setReturnValue(ItemStack.EMPTY);
                    ci.cancel();
                    return;
                }
            } else if (cursor.getItem() == SlashItems.SOCKET_EXTRACTOR.get()) {

                GearItemData gear = Gear.Load(stack);

                if (gear != null) {
                    if (gear.sockets != null && gear.sockets.sockets.size() > 0) {
                        try {
                            ItemStack gem = new ItemStack(gear.sockets.sockets.get(0)
                                .getGem()
                                .getItem());
                            gear.sockets.sockets.remove(0);
                            ItemStack copy = stack.copy();
                            Gear.Save(copy, gear);
                            slot.set(copy);
                            if (!player.level().isClientSide()) {
                                PlayerUtils.giveItem(gem, player);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        slot.setChanged();
                        ci.setReturnValue(ItemStack.EMPTY);
                        ci.cancel();
                        return;
                    }
                }
            }

            if (success) {
                SoundUtils.ding(player.level(), player.blockPosition());
                SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.ANVIL_USE, 1, 1);

                cursor.shrink(1);
                screen.setCarried(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                slot.setChanged();
                ci.setReturnValue(ItemStack.EMPTY);
                ci.cancel();
            }

        }
    }
}
