package com.robertx22.the_harvest.main;

import com.robertx22.library_of_exile.registry.ExileRegistryEventClass;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.OrderedModConstructor;
import com.robertx22.the_harvest.database.HarvestDatabase;
import com.robertx22.the_harvest.database.holders.*;
import com.robertx22.forge_compat.eventbus.api.IEventBus;

import java.util.Arrays;
import java.util.List;

public class HarvestModConstructor extends OrderedModConstructor {
    public HarvestModConstructor(String modid) {
        super(modid);
    }

    @Override
    public List<ExileRegistryEventClass> getRegisterEvents() {
        return Arrays.asList(

        );
    }

    @Override
    public List<ExileKeyHolder> getAllKeyHolders() {
        return Arrays.asList(
                HarvestLeagues.INSTANCE,
                HarvestMapContents.INSTANCE,
                HarvestMapBlocks.INSTANCE,
                HarvestItemMods.INSTANCE,
                HarvestItemReqs.INSTANCE,
                HarvestOrbs.INSTANCE,
                HarvestArenas.INSTANCE,
                HarvestRelicTypes.INSTANCE,
                HarvestRelicStats.INSTANCE,
                HarvestRelicAffixes.INSTANCE
        );
    }

    @Override
    public void registerDeferredContainers(IEventBus iEventBus) {
        HarvestEntries.initDeferred();
    }

    @Override
    public void registerDeferredEntries() {
        HarvestEntries.init();
    }

    @Override
    public void registerDatabases() {
        HarvestDatabase.INSTANCE.initDatabases();
    }

    @Override
    public void registerDatabaseEntries() {
    }
}
