package com.robertx22.age_of_exile.uncommon.utilityclasses;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * ItemStack NBT access for the 26.3 component-based item model.
 * Maps the pre-1.20.5 tag API onto the CUSTOM_DATA item component.
 */
public class StackNBT {

    public static CompoundTag get(ItemStack stack) {
        if (stack == null) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static boolean has(ItemStack stack) {
        return stack != null && stack.has(DataComponents.CUSTOM_DATA);
    }

    public static CompoundTag getOrCreate(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            CompoundTag tag = new CompoundTag();
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return tag;
        }
        return data.copyTag();
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        if (tag == null) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static void save(ItemStack stack, CompoundTag tag) {
        set(stack, tag);
    }

    public static CompoundTag saveStack(ItemStack stack) {
        return saveStack(stack, null);
    }

    public static CompoundTag saveStack(ItemStack stack, CompoundTag tag) {
        if (tag == null) tag = new CompoundTag();
        if (stack == null || stack.isEmpty()) {
            tag.putString("id", "minecraft:air");
            tag.putByte("Count", (byte) 0);
            return tag;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        tag.putString("id", id != null ? id.toString() : "minecraft:air");
        tag.putByte("Count", (byte) stack.getCount());
        CompoundTag custom = get(stack);
        if (custom != null) {
            tag.put("tag", custom);
        }
        return tag;
    }

    public static ItemStack loadStack(CompoundTag tag) {
        if (tag == null) return ItemStack.EMPTY;
        String idStr = tag.getStringOr("id", "minecraft:air");
        int count = tag.getByteOr("Count", (byte) 0);
        if (count <= 0 || "minecraft:air".equals(idStr)) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(idStr));
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, count);
        if (tag.contains("tag")) {
            set(stack, tag.getCompoundOrEmpty("tag"));
        }
        return stack;
    }
}
