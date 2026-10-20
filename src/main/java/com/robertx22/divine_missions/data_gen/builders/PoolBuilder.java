package com.robertx22.divine_missions.data_gen.builders;

import com.robertx22.divine_missions.data_gen.adders.GodsAdder;
import com.robertx22.divine_missions.database.condition_types.ConditionData;
import com.robertx22.divine_missions.database.db_types.Pool;
import com.robertx22.library_of_exile.registry.IGUID;

import java.util.List;
import java.util.stream.Collectors;

public class PoolBuilder {

    Pool pool = new Pool();

    public static <T extends IGUID> PoolBuilder of(String id, int weight, Pool.PoolType type, Pool.PickType pickType, List<T> entries) {

        PoolBuilder b = new PoolBuilder();
        b.pool.id = id;
        b.pool.weight = weight;

        b.pool.type = type;
        b.pool.pick_type = pickType;

        b.pool.entries = entries.stream()
            .map(x -> x.GUID())
            .collect(Collectors.toList());

        return b;

    }

    public PoolBuilder addCondition(ConditionData data) {
        pool.conditions.add(data);
        return this;
    }

    public Pool buildForGod(String god) {
        pool.gods.add(god);
        pool.addToSerializables();
        return pool;
    }

    public Pool buildForGods() {
        pool.gods.addAll(GodsAdder.ALL_GODS);
        pool.addToSerializables();
        return pool;
    }

}
