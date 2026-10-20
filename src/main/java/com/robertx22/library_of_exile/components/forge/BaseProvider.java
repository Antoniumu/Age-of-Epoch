package com.robertx22.library_of_exile.components.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.common.capabilities.ICapabilitySerializable;
import com.robertx22.forge_compat.common.util.LazyOptional;

import org.jetbrains.annotations.NotNull;

public abstract class BaseProvider<TYPE, OWNER_CLASS> implements ICapabilitySerializable<CompoundTag> {

    public OWNER_CLASS owner;

    public BaseProvider(OWNER_CLASS owner) {
        this.owner = owner;

        impl = newDefaultImpl(owner);
        cap = LazyOptional.of(() -> impl);
    }

    public abstract TYPE newDefaultImpl(OWNER_CLASS owner);

    public abstract Capability<TYPE> dataInstance();

    TYPE impl;
    private final LazyOptional<TYPE> cap;

    @Override
    public CompoundTag serializeNBT() {
        return (CompoundTag) dataInstance().getStorage()
            .writeNBT(dataInstance(), impl, null);

    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        dataInstance().getStorage()
            .readNBT(dataInstance(), impl, null, nbt);

    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == dataInstance()) {
            return this.cap.cast();
        }
        return LazyOptional.empty();
    }
}