package info.loenwind.autosave.handlers.java;

import info.loenwind.autosave.Registry;
import info.loenwind.autosave.handlers.IHandler;
import info.loenwind.autosave.util.NBTAction;
import info.loenwind.autosave.util.TypeUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.lang.reflect.Type;
import java.util.Set;

public class HandleEnum implements IHandler<Enum<?>> {

    public HandleEnum() {
    }

    @Override
    public Class<?> getRootType() {
        return Enum.class;
    }

    @Override
    public boolean store(Registry registry, Set<NBTAction> phase, CompoundTag nbt, Type type, String name,
                         Enum<?> object) throws IllegalArgumentException, IllegalAccessException {
        nbt.putInt(name, object.ordinal());
        return true;
    }

    @Override
    public Enum<?> read(Registry registry, Set<NBTAction> phase, CompoundTag nbt, Type type, String name,
                        Enum<?> object) {
        if (nbt.contains(name)) {
            Enum<?>[] enumConstants = (Enum<?>[]) TypeUtil.toClass(type)
                .getEnumConstants();
            if (enumConstants != null) { // This should be "impossible"
                return enumConstants[Mth.clamp(nbt.getIntOr(name, 0), 0, enumConstants.length - 1)];
            }
        }
        return object;
    }

}
