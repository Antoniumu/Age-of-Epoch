package com.robertx22.divine_missions.db_init;

import com.robertx22.divine_missions.database.adders.ConditionTypeAdders;
import com.robertx22.divine_missions.database.adders.RewardTypeAdders;
import com.robertx22.divine_missions.database.adders.TaskTypesAdder;

public class RegisterNonDatapackEntries {

    public static void register() {

        new TaskTypesAdder().registerAll();
        new RewardTypeAdders().registerAll();
        new ConditionTypeAdders().registerAll();
    }
}
