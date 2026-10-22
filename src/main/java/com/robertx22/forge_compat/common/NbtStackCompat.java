package com.robertx22.forge_compat.common;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.lang.reflect.Field;
import java.util.List;

public final class NbtStackCompat {
    private static final Field CUSTOM_DATA_TAG_FIELD = resolveTagField();

    private static Field resolveTagField() {
        try {
            for (Field f : CustomData.class.getDeclaredFields()) {
                if (f.getType() == CompoundTag.class) {
                    f.setAccessible(true);
                    return f;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static CustomData wrapLive(CompoundTag tag) {
        CustomData data = CustomData.of(tag);
        if (CUSTOM_DATA_TAG_FIELD != null) {
            try {
                CUSTOM_DATA_TAG_FIELD.set(data, tag);
            } catch (Throwable ignored) {}
        }
        return data;
    }

    public static boolean hasTag(ItemStack stack) {
        return stack != null && stack.has(DataComponents.CUSTOM_DATA);
    }

    public static CompoundTag getTag(ItemStack stack) {
        if (stack == null) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        if (CUSTOM_DATA_TAG_FIELD != null) {
            try {
                return (CompoundTag) CUSTOM_DATA_TAG_FIELD.get(data);
            } catch (Throwable ignored) {}
        }
        return data.copyTag();
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        if (stack == null) return new CompoundTag();
        CompoundTag existing = getTag(stack);
        if (existing != null) {
            return existing;
        }
        CompoundTag created = new CompoundTag();
        stack.set(DataComponents.CUSTOM_DATA, wrapLive(created));
        return created;
    }

    public static CompoundTag getTagElement(ItemStack stack, String key) {
        CompoundTag tag = getTag(stack);
        if (tag == null || !tag.contains(key)) return null;
        return tag.getCompoundOrEmpty(key);
    }

    public static CompoundTag getOrCreateTagElement(ItemStack stack, String key) {
        CompoundTag root = getOrCreateTag(stack);
        if (!root.contains(key)) {
            CompoundTag sub = new CompoundTag();
            root.put(key, sub);
            return sub;
        }
        return root.getCompoundOrEmpty(key);
    }

    public static void removeTagKey(ItemStack stack, String key) {
        CompoundTag tag = getTag(stack);
        if (tag != null) {
            tag.remove(key);
            if (tag.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            }
        }
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        if (stack == null) return;
        if (tag == null) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, wrapLive(tag));
            if (tag.contains("CustomModelData")) {
                syncCustomModelData(stack, tag.getIntOr("CustomModelData", 0));
            }
        }
    }

    public static void syncCustomModelData(ItemStack stack, int value) {
        if (stack == null) return;
        getOrCreateTag(stack).putInt("CustomModelData", value);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
            List.of((float) value),
            List.of(),
            List.of(),
            List.of()
        ));
    }

    public static ItemStack setHoverName(ItemStack stack, Component name) {
        if (stack != null) {
            if (name == null) {
                stack.remove(DataComponents.CUSTOM_NAME);
            } else {
                stack.set(DataComponents.CUSTOM_NAME, name);
            }
        }
        return stack;
    }
}
