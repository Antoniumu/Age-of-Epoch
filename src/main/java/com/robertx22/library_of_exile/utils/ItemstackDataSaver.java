package com.robertx22.library_of_exile.utils;

import com.robertx22.library_of_exile.registry.IGUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Supplier;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
public class ItemstackDataSaver<T> implements IGUID {

    String id;
    Class<T> clazz;
    Supplier<T> constructor;

    public ItemstackDataSaver(String id, Class<T> clazz, Supplier<T> constructor) {
        this.id = id;
        this.clazz = clazz;
        this.constructor = constructor;

        if (AllItemStackSavers.ALL.stream()
            .noneMatch(x -> x.GUID()
                .equals(GUID()))) {
            AllItemStackSavers.ALL.add(this);
        }

    }

    public Supplier<T> getConstructor() {
        return constructor;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public boolean has(ItemStack stack) {
        return stack != null && StackNBT.has(stack) && StackNBT.get(stack)
            .contains(id);
    }

    public T loadFrom(ItemStack stack) {

        if (stack == null) {
            return null;
        }
        if (!StackNBT.has(stack)) {
            return null;
        }

        T object = LoadSave.Load(clazz, constructor.get(), StackNBT.get(stack), id);

        return object;

    }

    public void saveTo(ItemStack stack, T object) {
        this.saveToObject(stack, object);
    }

    public void saveToObject(ItemStack stack, Object object) {
        if (stack == null) {
            return;
        }

        if (!StackNBT.has(stack)) {
            StackNBT.set(stack, new CompoundTag());
        }
        if (object != null) {
            LoadSave.Save(object, StackNBT.get(stack), id);
        }

    }

    @Override
    public String GUID() {
        return id;
    }
}
