package com.robertx22.age_of_exile.a_libraries.curios;

import com.robertx22.age_of_exile.a_libraries.curios.interfaces.INecklace;
import com.robertx22.age_of_exile.a_libraries.curios.interfaces.IRing;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MyCurioUtils {

    public static List<String> SLOTS = Arrays.asList("ring", "necklace");

    public static List<ICurioStacksHandler> getHandlers(Player player) {
        return getHandlers(SLOTS, player);
    }

    public static List<ICurioStacksHandler> getHandlers(List<String> slots, Player player) {

        List<ICurioStacksHandler> list = new ArrayList<>();

        ICuriosItemHandler handler = CuriosApi.getCuriosHelper()
            .getCuriosHandler(player)
            .orElse(null);

        if (handler != null) {
            for (String slot : slots) {

                Optional<ICurioStacksHandler> sh = handler.getStacksHandler(slot);

                if (sh.isPresent()) {
                    list.add(sh.get());
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

        List<ICurioStacksHandler> handlers = getHandlers(slots, player);
        if (!handlers.isEmpty()) {
            handlers.forEach(x -> {
                for (int i = 0; i < x.getSlots(); i++) {
                    ItemStack stack = x.getStacks().getStackInSlot(i);
                    list.add(stack);
                }
            });
            return list;
        }

        if (player == null || player.getInventory() == null) {
            return list;
        }

        for (String slot : slots) {
            int maxForSlot = "ring".equals(slot) ? 2 : 1;
            int found = 0;
            for (int i = 0; i < player.getInventory().getContainerSize() && found < maxForSlot; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (matchesSlot(stack, slot)) {
                    list.add(stack);
                    found++;
                }
            }
            while (found < maxForSlot) {
                list.add(ItemStack.EMPTY);
                found++;
            }
        }

        return list;

    }
}
