package com.robertx22.world_of_exile.world_gen;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Compile-time stub of World of Exile's AbstractPool. */
public class AbstractPool {

    public final ResourceLocation id;
    public final List<ResourceLocation> children = new ArrayList<>();

    public AbstractPool(ResourceLocation id) {
        this.id = id;
    }

    public void add(ResourceLocation child) {
        children.add(child);
    }
}
