package com.robertx22.world_of_exile.world_gen;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Compile-time stub of World of Exile's AbstractPool. */
public class AbstractPool {

    public final Identifier id;
    public final List<Identifier> children = new ArrayList<>();

    public AbstractPool(Identifier id) {
        this.id = id;
    }

    public void add(Identifier child) {
        children.add(child);
    }

    /** Returns this pool; kept so call sites written against the original builder API still compile. */
    public AbstractPool build() {
        return this;
    }

    /** Processor list of this pool; subclasses may narrow the return type. */
    public Object processorList() {
        return null;
    }
}
