package com.robertx22.age_of_exile.a_libraries.trinkets;

import com.robertx22.age_of_exile.a_libraries.trinkets.interfaces.ITrinketType;
import com.robertx22.age_of_exile.a_libraries.trinkets.interfaces.INecklace;
import com.robertx22.age_of_exile.a_libraries.trinkets.interfaces.IRing;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyTrinketUtils {

    public static List<String> SLOTS = Arrays.asList("ring", "necklace");

    public static List<TrinketInventory> getHandlers(Player player) {
        return getHandlers(SLOTS, player);
    }

    public static List<TrinketInventory> getHandlers(List<String> slots, Player player) {

        List<TrinketInventory> list = new ArrayList<>();

        TrinketAttachment attachment = TrinketsApi.getAttachment(player);

        if (attachment != null) {
            for (String slot : slots) {

                String trinketId = TrinketSlots.trinketId(slot);

                if (trinketId == null) {
                    continue;
                }

                TrinketInventory inventory = attachment.getInventory(trinketId);

                if (inventory != null) {
                    list.add(inventory);
                }

            }
        }
        return list;
    }

    public static List<ItemStack> getAllSlots(Player player) {
        return getAllSlots(SLOTS, player);
    }

    public static ItemStack get(String slot, Player player, int num) {

        List<ItemStack> list = getAllSlots(Arrays.asList(slot), player);

        if (num + 1 > list.size()) {
            return ItemStack.EMPTY;
        } else {
            return list.get(num);
        }
    }

    private static boolean matchesSlot(ItemStack stack, String slot) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if ("necklace".equals(slot) && stack.getItem() instanceof INecklace) {
            return true;
        }
        if ("ring".equals(slot) && stack.getItem() instanceof IRing) {
            return true;
        }
        GearItemData gear = Gear.Load(stack);
        if (gear != null && gear.GetBaseGearType() != null && gear.GetBaseGearType().getGearSlot() != null) {
            return slot.equals(gear.GetBaseGearType().getGearSlot().GUID());
        }
        GearSlot gs = GearSlot.getSlotOf(stack.getItem());
        return gs != null && slot.equals(gs.GUID());
    }

    public static List<ItemStack> getAllSlots(List<String> slots, Player player) {

        List<ItemStack> list = new ArrayList<>();

        List<TrinketInventory> handlers = getHandlers(slots, player);
        if (!handlers.isEmpty()) {
            handlers.forEach(x -> {
                for (int i = 0; i < x.getContainerSize(); i++) {
                    ItemStack stack = x.getItem(i);
                    list.add(stack);
                }
            });
            return list;
        }

        return list;

    }

    public static boolean isTrinketOfSlot(Item item, String slotName) {
        if (item instanceof ITrinketType type) {
            return type.trinketTypeName().equals(slotName);
        }
        return false;
    }

}
