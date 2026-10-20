package com.robertx22.forge_compat.blocks;

/** 1.16-style tickable tile entity marker; tick() is invoked by the block entity itself. */
public interface ITickableBlockEntity {
    void tick();
}
