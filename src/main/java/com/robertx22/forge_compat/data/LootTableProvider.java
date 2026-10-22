package com.robertx22.forge_compat.data;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class LootTableProvider {
    public record SubProviderEntry(Supplier<LootTableSubProvider> provider, Object paramSet) {}
    public LootTableProvider(Object output, Set<?> requiredTables, List<SubProviderEntry> subProviders) {}
}
