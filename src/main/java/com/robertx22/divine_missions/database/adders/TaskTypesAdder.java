package com.robertx22.divine_missions.database.adders;

import com.robertx22.divine_missions.database.task_types.*;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

public class TaskTypesAdder implements ExileRegistryInit {

    @Override
    public void registerAll() {

        new BringItem().registerToExileRegistry();
        new KillAnyMob().registerToExileRegistry();
        new KillSpecificMob().registerToExileRegistry();
        new KillMobTag().registerToExileRegistry();
        new LootChest().registerToExileRegistry();

    }
}
