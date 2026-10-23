package com.robertx22.divine_missions.database.adders;

import com.robertx22.divine_missions.database.condition_types.IsModLoaded;
import com.robertx22.divine_missions.database.condition_types.ReputationLevelCondition;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

public class ConditionTypeAdders implements ExileRegistryInit {

    @Override
    public void registerAll() {

        new ReputationLevelCondition().registerToExileRegistry();
        new IsModLoaded().registerToExileRegistry();

    }
}
