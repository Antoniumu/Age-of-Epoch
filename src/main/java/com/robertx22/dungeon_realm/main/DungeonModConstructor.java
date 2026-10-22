package com.robertx22.dungeon_realm.main;

import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.database.holders.*;
import com.robertx22.library_of_exile.registry.ExileRegistryEventClass;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.OrderedModConstructor;
import com.robertx22.forge_compat.eventbus.api.IEventBus;

import java.util.Arrays;
import java.util.List;

public class DungeonModConstructor extends OrderedModConstructor {
    public DungeonModConstructor(String modid) {
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
                DungeonMapBlocks.INSTANCE,
                DungeonDungeons.INSTANCE,
                DungeonBossArenas.INSTANCE,
                DungeonUberBosses.INSTANCE,
                DungeonBonusContents.INSTANCE,
                DungeonItemMods.INSTANCE,
                DungeonItemReqs.INSTANCE,
                DungeonOrbs.INSTANCE,
                DungeonLeagues.INSTANCE,
                DungeonRelicTypes.INSTANCE,
                DungeonRelicStats.INSTANCE,
                DungeonRelicAffixes.INSTANCE
        );
    }

    @Override
    public void registerDeferredContainers(IEventBus iEventBus) {
        DungeonEntries.initDeferred();
    }

    @Override
    public void registerDeferredEntries() {
        DungeonEntries.init();
    }

    @Override
    public void registerDatabases() {
        DungeonDatabase.INSTANCE.initDatabases();
    }

    @Override
    public void registerDatabaseEntries() {

    }
}
