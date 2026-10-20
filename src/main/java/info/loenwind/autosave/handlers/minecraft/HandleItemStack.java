package info.loenwind.autosave.handlers.minecraft;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import info.loenwind.autosave.Registry;
import info.loenwind.autosave.engine.StorableEngine;
import info.loenwind.autosave.exceptions.NoHandlerFoundException;
import info.loenwind.autosave.handlers.IHandler;
import info.loenwind.autosave.util.NBTAction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Type;
import java.util.Set;

public class HandleItemStack implements IHandler<ItemStack> {

    public HandleItemStack() {
    }

    @Override
    public Class<?> getRootType() {
        return ItemStack.class;
    }

    @Override
    public boolean store(Registry registry, Set<NBTAction> phase, CompoundTag nbt, Type type, String name, ItemStack object)
        throws IllegalArgumentException, IllegalAccessException, InstantiationException, NoHandlerFoundException {
        if (object.isEmpty()) {
            nbt.putBoolean(name + StorableEngine.EMPTY_POSTFIX, true);
        } else {
            CompoundTag tag = new CompoundTag();
            StackNBT.saveStack(object, tag);
            nbt.put(name, tag);
        }
        return true;
    }

    @Override
    public ItemStack read(Registry registry, Set<NBTAction> phase, CompoundTag nbt, Type type, String name,
                          ItemStack object) throws IllegalArgumentException, IllegalAccessException, InstantiationException, NoHandlerFoundException {
        if (nbt.contains(name)) {
            CompoundTag tag = nbt.getCompoundOrEmpty(name);
            return StackNBT.loadStack(tag);
        } else if (nbt.contains(name + StorableEngine.EMPTY_POSTFIX)) {
            return ItemStack.EMPTY;
        }
        return object;
    }
}
