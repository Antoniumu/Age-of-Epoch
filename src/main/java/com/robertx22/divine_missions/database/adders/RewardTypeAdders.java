package com.robertx22.divine_missions.database.adders;

import com.robertx22.divine_missions.database.reward_types.CommandRewardType;
import com.robertx22.divine_missions.database.reward_types.ExpRewardType;
import com.robertx22.divine_missions.database.reward_types.ItemRewardType;
import com.robertx22.divine_missions.database.reward_types.LootTableRewardType;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

public class RewardTypeAdders implements ExileRegistryInit {

    @Override
    public void registerAll() {

        new CommandRewardType().registerToExileRegistry();
        new LootTableRewardType().registerToExileRegistry();
        new ExpRewardType().registerToExileRegistry();
        new ItemRewardType().registerToExileRegistry();

    }
}
