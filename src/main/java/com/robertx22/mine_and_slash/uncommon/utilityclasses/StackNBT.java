package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.forge_compat.common.NbtStackCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class StackNBT {
    public static CompoundTag saveStack(HolderLookup.Provider registries, ItemStack stack) {
        CompoundTag out = new CompoundTag();
        if (stack == null || stack.isEmpty()) return out;
        out.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        out.putInt("count", stack.getCount());
        if (NbtStackCompat.hasTag(stack)) {
            out.put("tag", NbtStackCompat.getTag(stack).copy());
        }
        return out;
    }

    public static ItemStack loadStack(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return ItemStack.EMPTY;
        String id = tag.getStringOr("id", "");
        if (id.isEmpty()) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item, Math.max(1, tag.getIntOr("count", 1)));
        if (tag.contains("tag")) {
            NbtStackCompat.setTag(stack, tag.getCompoundOrEmpty("tag").copy());
        }
        return stack;
    }

    public static CompoundTag saveStack(ItemStack stack) {
        return saveStack(null, stack);
    }

    public static ItemStack loadStack(CompoundTag tag) {
        return loadStack(null, tag);
    }
}
