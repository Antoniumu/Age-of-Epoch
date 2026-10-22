package com.robertx22.dungeon_realm.database;

import com.robertx22.dungeon_realm.database.boss_arena.BossArena;
import com.robertx22.dungeon_realm.database.dungeon.Dungeon;
import com.robertx22.dungeon_realm.database.uber_arena.UberBossArena;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryContainer;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.SyncTime;
import com.robertx22.library_of_exile.registry.helpers.ExileDatabaseInit;
import com.robertx22.library_of_exile.registry.register_info.SeriazableRegistration;
import net.minecraft.data.CachedOutput;

public class DungeonDatabase extends ExileDatabaseInit {
    public static DungeonDatabase INSTANCE = new DungeonDatabase(DungeonMain.MODID);

    public static SeriazableRegistration INFO = new SeriazableRegistration(DungeonMain.MODID);

    public DungeonDatabase(String modid) {
        super(modid);
    }

    public static ExileRegistryType BOSS_ARENA = ExileRegistryType.register(DungeonMain.MODID, "boss_arena", 51, BossArena.SERIALIZER, SyncTime.NEVER);
    public static ExileRegistryType UBER_BOSS = ExileRegistryType.register(DungeonMain.MODID, "uber_boss", 45, UberBossArena.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType DUNGEON = ExileRegistryType.register(DungeonMain.MODID, "dungeon", 34, Dungeon.SERIALIZER, SyncTime.ON_LOGIN); // todo does the client need this?

    public static ExileRegistryContainer<Dungeon> Dungeons() {
        return Database.getRegistry(DUNGEON);
    }

    public static ExileRegistryContainer<UberBossArena> UberBoss() {
        return Database.getRegistry(UBER_BOSS);
    }

    public static ExileRegistryContainer<BossArena> BossArena() {
        return Database.getRegistry(BOSS_ARENA);
    }

    @Override
    public void initDatabases() {

        Database.addRegistry(new ExileRegistryContainer<>(DUNGEON, "empty").setIsDatapack());
        Database.addRegistry(new ExileRegistryContainer<>(UBER_BOSS, "empty").setIsDatapack());
        Database.addRegistry(new ExileRegistryContainer<>(BOSS_ARENA, "empty").setIsDatapack());

    }

    @Override
    public void runDataGen(CachedOutput cachedOutput) {
        try {

            new DungeonDataGen().run(CachedOutput.NO_CACHE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
