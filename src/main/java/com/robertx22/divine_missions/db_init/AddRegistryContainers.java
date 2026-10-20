package com.robertx22.divine_missions.db_init;

import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryContainer;

public class AddRegistryContainers {

    public static void addAll() {

        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.GOD, null).setIsDatapack());
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.REPUTATION, null).setIsDatapack());
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.MISSION_RARITY, null).setIsDatapack());
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.TASK_ENTRY, null).setIsDatapack());
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.REWARD, null).setIsDatapack());
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.POOL, null).setIsDatapack());

        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.TASK_TYPE, null));
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.REWARD_TYPE, null));
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.CONDITION_TYPE, null));

    }
}
