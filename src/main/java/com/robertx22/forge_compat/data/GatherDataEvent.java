package com.robertx22.forge_compat.data;

import com.robertx22.forge_compat.eventbus.api.Event;
import com.robertx22.forge_compat.fml.event.lifecycle.IModBusEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GatherDataEvent extends Event implements IModBusEvent {
    public static class CompatGenerator {
        private final DataGenerator delegate;
        public final List<DataProvider> providers = new ArrayList<>();

        public CompatGenerator(DataGenerator delegate) {
            this.delegate = delegate;
        }

        public <T extends DataProvider> T addProvider(boolean run, T provider) {
            if (run && provider != null) {
                providers.add(provider);
            }
            return provider;
        }

        public PackOutput getPackOutput() {
            return delegate != null ? delegate.getPackOutput() : null;
        }
    }

    private final CompatGenerator generator;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;

    public GatherDataEvent(DataGenerator dataGenerator, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        this.generator = new CompatGenerator(dataGenerator);
        this.lookupProvider = lookupProvider;
    }

    public CompatGenerator getGenerator() { return generator; }
    public CompletableFuture<HolderLookup.Provider> getLookupProvider() { return lookupProvider; }
    public boolean includeServer() { return true; }
    public boolean includeClient() { return true; }
}
