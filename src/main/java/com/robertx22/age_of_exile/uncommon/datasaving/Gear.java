package com.robertx22.age_of_exile.uncommon.datasaving;

import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.library_of_exile.utils.LoadSave;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Registry;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import net.minecraft.core.registries.BuiltInRegistries;
public class Gear {

    private static final String LOC = "GEAR_ITEM_DATA";

    public static boolean has(ItemStack stack) {
        return stack != null && StackNBT.has(stack) && StackNBT.get(stack)
            .contains(LOC);
    }

    public static GearItemData Load(ItemStack stack) {

        if (stack == null) {
            return null;
        }
        if (!StackNBT.has(stack)) {
            return null;
        }

        GearItemData gear = LoadSave.Load(GearItemData.class, new GearItemData(), StackNBT.get(stack), LOC);

        if (gear != null) {

            String id = BuiltInRegistries.ITEM.getKey(stack.getItem())
                .toString();

            if (gear.item_id.isEmpty()) {
                gear.item_id = id;
            } else {
                if (id.equals(gear.item_id) == false) {
                    // reroll by clearing gear tag because it was changed/upgraded
                    StackNBT.get(stack)
                        .remove(LOC);
                    return new GearItemData();
                }
            }
        }

        return gear;

    }

    public static void Save(ItemStack stack, GearItemData gear) {

        if (stack == null) {
            return;
        }
        if (gear != null) {
            LoadSave.Save(gear, StackNBT.getOrCreate(stack), LOC);
        }

    }

}
