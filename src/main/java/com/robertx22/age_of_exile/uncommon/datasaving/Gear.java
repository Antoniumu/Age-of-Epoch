package com.robertx22.age_of_exile.uncommon.datasaving;

import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.database.registry.ExileDB;
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

    public static boolean repairIfMismatched(ItemStack stack, GearItemData gear) {
        if (stack == null || stack.isEmpty() || gear == null) {
            return false;
        }
        try {
            if (ExileDB.GearTypes().isEmpty() || ExileDB.GearSlots().isEmpty()) {
                return false;
            }
            BaseGearType currentType = gear.GetBaseGearType();
            GearSlot typeSlot = currentType != null ? currentType.getGearSlot() : null;
            if (typeSlot != null && GearSlot.isItemOfThisSlot(typeSlot, stack.getItem())) {
                return false;
            }
            GearSlot actualSlot = GearSlot.getSlotOf(stack.getItem());
            if (actualSlot == null) {
                return false;
            }
            BaseGearType fixedType = null;
            if (ExileDB.GearTypes().isRegistered(actualSlot.GUID())) {
                fixedType = ExileDB.GearTypes().get(actualSlot.GUID());
            } else {
                for (BaseGearType candidate : ExileDB.GearTypes().getList()) {
                    if (actualSlot.GUID().equals(candidate.gear_slot) || actualSlot.GUID().equals(candidate.GUID())) {
                        fixedType = candidate;
                        break;
                    }
                }
            }
            if (fixedType == null) {
                return false;
            }
            gear.gear_type = fixedType.GUID();
            if (gear.baseStats != null) {
                gear.baseStats.RerollFully(gear);
            }
            if (gear.imp != null) {
                gear.imp.RerollFully(gear);
            }
            Save(stack, gear);
            return true;
        } catch (Exception ignored) {
            return false;
        }
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

            repairIfMismatched(stack, gear);
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
