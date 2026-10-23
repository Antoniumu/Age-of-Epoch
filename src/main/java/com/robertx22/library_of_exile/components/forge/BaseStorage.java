package com.robertx22.library_of_exile.components.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Direction;
import com.robertx22.forge_compat.common.capabilities.Capability;

public interface BaseStorage<TYPE extends ICommonCap> extends Capability.IStorage<TYPE> {

    @Override
    public default Tag writeNBT(Capability<TYPE> capability, TYPE instance, Direction side) {
        return instance.saveToNBT();
    }

    @Override
    public default void readNBT(Capability<TYPE> capability, TYPE instance, Direction side, Tag nbt) {
        instance.loadFromNBT((CompoundTag) nbt);
    }

}