package com.robertx22.forge_compat.common.capabilities;

import com.robertx22.forge_compat.common.util.LazyOptional;
import net.minecraft.core.Direction;

public interface ICapabilityProvider {
    <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side);
}
