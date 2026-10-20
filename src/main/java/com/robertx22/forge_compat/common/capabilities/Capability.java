package com.robertx22.forge_compat.common.capabilities;

import com.robertx22.forge_compat.common.util.LazyOptional;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;

public class Capability<T> {

    public interface IStorage<T> {
        Tag writeNBT(Capability<T> capability, T instance, Direction side);

        void readNBT(Capability<T> capability, T instance, Direction side, Tag nbt);
    }

    private final Class<T> type;
    private final IStorage<T> storage;

    Capability(Class<T> type, IStorage<T> storage) {
        this.type = type;
        this.storage = storage;
    }

    public Class<T> getType() { return type; }

    public IStorage<T> getStorage() { return storage; }

    public LazyOptional<T> getCapability(Object provider) {
        return CapabilityProvider.get(provider, this);
    }
}
