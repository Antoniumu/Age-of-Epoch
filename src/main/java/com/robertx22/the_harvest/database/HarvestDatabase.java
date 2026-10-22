package com.robertx22.the_harvest.database;

import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryContainer;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.SyncTime;
import com.robertx22.library_of_exile.registry.helpers.ExileDatabaseInit;
import com.robertx22.library_of_exile.registry.register_info.SeriazableRegistration;
import com.robertx22.the_harvest.main.HarvestMain;
import net.minecraft.data.CachedOutput;

public class HarvestDatabase extends ExileDatabaseInit {

    public static HarvestDatabase INSTANCE = new HarvestDatabase(HarvestMain.MODID);

    public HarvestDatabase(String modid) {
        super(modid);
    }

    public static SeriazableRegistration SERIALIZABLE_INFO = new SeriazableRegistration(HarvestMain.MODID);

    public static ExileRegistryType HARVEST_ARENA = ExileRegistryType.register(HarvestMain.MODID, "harvest_arena", 0, HarvestArena.SERIALIZER, SyncTime.ON_LOGIN);


    public static ExileRegistryContainer<HarvestArena> getHarvestArenas() {
        return (ExileRegistryContainer<HarvestArena>) Database.getRegistry(HARVEST_ARENA);
    }

    @Override
    public void initDatabases() {
        Database.addRegistry(new ExileRegistryContainer<>(HARVEST_ARENA, "aztec").setIsDatapack());
    }

    @Override
    public void runDataGen(CachedOutput cachedOutput) {
        try {
            new HarvestDataGen().run(CachedOutput.NO_CACHE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
