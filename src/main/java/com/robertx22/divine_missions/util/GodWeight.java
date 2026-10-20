package com.robertx22.divine_missions.util;

import com.robertx22.divine_missions.database.db_types.God;
import com.robertx22.library_of_exile.registry.IWeighted;

public class GodWeight implements IWeighted {
    public God god;
    public int weight;

    public GodWeight(God god, int weight) {
        this.god = god;
        this.weight = weight;
    }

    @Override
    public int Weight() {
        return weight;
    }
}
