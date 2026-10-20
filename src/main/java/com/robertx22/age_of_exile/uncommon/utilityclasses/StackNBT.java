package com.robertx22.age_of_exile.uncommon.utilityclasses;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

/**
 * ItemStack NBT access for the 26.3 component-based item model.
 * Maps the pre-1.20.5 tag API onto the CUSTOM_DATA item component while
 * returning the live internal CompoundTag reference so in-place mutations
 * (e.g. StackNBT.getOrCreate(stack).putString(...) or LoadSave.Save(..., StackNBT.get(stack), id))
 * persist directly on the ItemStack.
 */
public class StackNBT {

    private static final Field CUSTOM_DATA_TAG_FIELD = resolveTagField();

    private static Field resolveTagField() {
        try {
            for (Field f : CustomData.class.getDeclaredFields()) {
                if (f.getType() == CompoundTag.class) {
                    f.setAccessible(true);
                    return f;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static CompoundTag getLiveTag(CustomData data) {
        if (data == null) return null;
        if (CUSTOM_DATA_TAG_FIELD != null) {
            try {
                return (CompoundTag) CUSTOM_DATA_TAG_FIELD.get(data);
            } catch (Throwable ignored) {
            }
        }
        return data.copyTag();
    }

    private static CustomData createLiveCustomData(CompoundTag tag) {
        CustomData data = CustomData.of(tag);
        if (CUSTOM_DATA_TAG_FIELD != null) {
            try {
                CUSTOM_DATA_TAG_FIELD.set(data, tag);
            } catch (Throwable ignored) {
            }
        }
        return data;
    }

    public static CompoundTag get(ItemStack stack) {
        if (stack == null) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return getLiveTag(data);
    }

    public static boolean has(ItemStack stack) {
        return stack != null && stack.has(DataComponents.CUSTOM_DATA);
    }

    public static CompoundTag getOrCreate(ItemStack stack) {
        if (stack == null) return new CompoundTag();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            CompoundTag tag = new CompoundTag();
            data = createLiveCustomData(tag);
            stack.set(DataComponents.CUSTOM_DATA, data);
            return tag;
        }
        return getLiveTag(data);
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        if (stack == null) return;
        if (tag == null) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, createLiveCustomData(tag));
            if (tag.contains("CustomModelData")) {
                applyCustomModelDataComponent(stack, tag.getIntOr("CustomModelData", 0));
            }
        }
    }

    public static void setCustomModelData(ItemStack stack, int value) {
        if (stack == null) return;
        getOrCreate(stack).putInt("CustomModelData", value);
        applyCustomModelDataComponent(stack, value);
    }

    private static void applyCustomModelDataComponent(ItemStack stack, int value) {
        try {
            for (Constructor<?> ctor : CustomModelData.class.getDeclaredConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 4
                    && List.class.isAssignableFrom(params[0])
                    && List.class.isAssignableFrom(params[1])
                    && List.class.isAssignableFrom(params[2])
                    && List.class.isAssignableFrom(params[3])) {
                    ctor.setAccessible(true);
                    CustomModelData cmd = (CustomModelData) ctor.newInstance(
                        List.of((float) value),
                        List.of(),
                        List.of(),
                        List.of()
                    );
                    stack.set(DataComponents.CUSTOM_MODEL_DATA, cmd);
                    return;
                } else if (params.length == 1 && (params[0] == int.class || params[0] == Integer.class)) {
                    ctor.setAccessible(true);
                    CustomModelData cmd = (CustomModelData) ctor.newInstance(value);
                    stack.set(DataComponents.CUSTOM_MODEL_DATA, cmd);
                    return;
                }
            }
        } catch (Throwable ignored) {
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
            tag.put("tag", custom.copy());
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
            set(stack, tag.getCompoundOrEmpty("tag").copy());
        }
        return stack;
    }
}
