package com.robertx22.age_of_exile.event_hooks.ontick;

import com.robertx22.age_of_exile.a_libraries.curios.MyCurioUtils;
import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Arrays;
import java.util.List;

public class UnequipGear {

    // dont drop weapons becasuse then newbies can't use stuff like axes at low level!

    public static List<EquipmentSlot> SLOTS = Arrays.asList(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.OFFHAND);

    static void drop(Player player, EquipmentSlot slot, ItemStack stack, MutableComponent txt) {
        ItemStack copy = stack.copy();

        player.setItemSlot(slot, ItemStack.EMPTY); // todo is this good?

        if (player.getItemBySlot(slot)
            .isEmpty()) {
            PlayerUtils.giveItem(copy, player);
            player.sendSystemMessage(txt
                );
        } else {
            player.setItemSlot(slot, copy);
            System.out.print("Error in unequipping gear, weird!!!");
        }
    }

    static void drop(Player player, ICurioStacksHandler handler, int number, ItemStack stack, MutableComponent txt) {

        ItemStack copy = stack.copy();
        handler.getStacks()
            .setStackInSlot(number, ItemStack.EMPTY);
        PlayerUtils.giveItem(copy, player);
        player.sendSystemMessage(txt);
    }

    public static void onTick(Player player) {

        int runewords = 0;
        int uniques = 0;

        for (EquipmentSlot slot : SLOTS) {

            ItemStack stack = player.getItemBySlot(slot);

            GearItemData gear = Gear.Load(stack);

            if (gear != null) {
                if (!gear.canPlayerWear(Load.Unit(player))) {
                    drop(player, slot, stack, Component.literal("Вы не соответствуете требованиям этого предмета.").withStyle(ChatFormatting.RED));
                } else if (gear.isUnique()) {
                    uniques++;
                    if (uniques > ServerContainer.get().MAX_UNIQUE_GEARS_WORN.get()) {
                        drop(player, slot, stack, Component.literal("Вы не можете надеть столько уникальных предметов.").withStyle(ChatFormatting.RED));
                    }
                }
            }
        }

        for (ICurioStacksHandler handler : MyCurioUtils.getHandlers(player)) {

            for (int i = 0; i < handler
                .getSlots(); i++) {

                ItemStack stack = handler
                    .getStacks()
                    .getStackInSlot(i);

                if (!stack.isEmpty()) {
                    GearItemData gear = Gear.Load(stack);

                    if (gear != null) {
                        if (!gear.canPlayerWear(Load.Unit(player))) {
                            drop(player, handler, i, stack, Component.literal("Вы не соответствуете требованиям этого предмета.").withStyle(ChatFormatting.RED));
                        } else if (gear.isUnique()) {
                            uniques++;
                            if (uniques > ServerContainer.get().MAX_UNIQUE_GEARS_WORN.get()) {
                                drop(player, handler, i, stack, Component.literal("Вы не можете надеть столько уникальных предметов.").withStyle(ChatFormatting.RED));
                            }
                        }
                    }
                }

            }

        }
    }
}
