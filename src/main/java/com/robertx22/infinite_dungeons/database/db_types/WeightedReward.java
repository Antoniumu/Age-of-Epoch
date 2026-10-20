package com.robertx22.infinite_dungeons.database.db_types;

import com.robertx22.infinite_dungeons.database.db_types.layout.ShopReward;
import com.robertx22.library_of_exile.registry.IWeighted;

public class WeightedReward implements IWeighted {

    public ShopReward reward;
    public int weight;

    public WeightedReward(ShopReward reward, int weight) {
        this.reward = reward;
        this.weight = weight;
    }

    @Override
    public int Weight() {
        return weight;
    }
}
