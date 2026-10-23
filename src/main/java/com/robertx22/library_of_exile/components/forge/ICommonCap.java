package com.robertx22.library_of_exile.components.forge;

import net.minecraft.nbt.CompoundTag;

public interface ICommonCap {
    CompoundTag saveToNBT();

    void loadFromNBT(CompoundTag value);

}