package com.robertx22.age_of_exile.uncommon.utilityclasses;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

/**
 * ItemStack NBT access for the 26.3 component-based item model.
 * Maps the pre-1.20.5 tag API onto the CUSTOM_DATA item component.
 */
public class StackNBT {

    public static CompoundTag get(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static boolean has(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    public static CompoundTag getOrCreate(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            CompoundTag tag = new CompoundTag();
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return tag;
        }
        // CustomData is immutable; expose a mutable copy and re-set it on write via set()
        return data.copyTag();
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        if (tag == null) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /**
     * Persist mutations made on a tag obtained via getOrCreate(): call this after
     * modifying the returned CompoundTag.
     */
    public static void save(ItemStack stack, CompoundTag tag) {
        set(stack, tag);
    }
}
