package com.robertx22.divine_missions.data_gen.adders;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;

public class PoolsAdder implements ExileRegistryInit {

    @Override
    public void registerAll() {
        new RewardPoolsAdder().registerAll();
        new TaskPoolsAdder().registerAll();
    }
}
