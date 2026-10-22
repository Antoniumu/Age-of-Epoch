package com.robertx22.forge_compat.common.util;

import net.minecraft.nbt.Tag;

public interface INBTSerializable<T extends Tag> {
    T serializeNBT();
    void deserializeNBT(T nbt);
}
