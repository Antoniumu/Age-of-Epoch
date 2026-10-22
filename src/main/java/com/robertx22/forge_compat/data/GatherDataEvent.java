package com.robertx22.forge_compat.data;

import com.robertx22.forge_compat.eventbus.api.Event;
import com.robertx22.forge_compat.fml.event.lifecycle.IModBusEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;

import java.util.concurrent.CompletableFuture;

public class GatherDataEvent extends Event implements IModBusEvent {
    private final DataGenerator dataGenerator;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;

    public GatherDataEvent(DataGenerator dataGenerator, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        this.dataGenerator = dataGenerator;
        this.lookupProvider = lookupProvider;
    }

    public DataGenerator getGenerator() { return dataGenerator; }
    public CompletableFuture<HolderLookup.Provider> getLookupProvider() { return lookupProvider; }
    public boolean includeServer() { return true; }
    public boolean includeClient() { return true; }
}
