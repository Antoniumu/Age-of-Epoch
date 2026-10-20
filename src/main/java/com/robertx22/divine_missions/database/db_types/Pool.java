package com.robertx22.divine_missions.database.db_types;

import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.divine_missions.database.condition_types.ConditionData;
import com.robertx22.divine_missions.db_init.RegistryTypes;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Pool implements JsonExileRegistry<Pool>, IAutoGson<Pool> {
    public static Pool SERIALIZER = new Pool();

    public enum PickType {
        ALWAYS_INCLUDE, PICK_ONE
    }

    public enum PoolType {
        REWARDS, TASKS
    }

    public int weight = 1000;
    public String id = "";

    public List<String> gods = new ArrayList<>();

    public PickType pick_type = PickType.PICK_ONE;
    public PoolType type = PoolType.REWARDS;

    public List<String> entries = new ArrayList<>();
    public List<ConditionData> conditions = new ArrayList<>();

    public List<Reward> getRewards() {
        return entries.stream()
            .map(x -> MissionsDB.Rewards()
                .get(x))
            .filter(x -> x != null)
            .collect(Collectors.toList());
    }

    public List<TaskEntry> getTaskEntries() {
        return entries.stream()
            .map(x -> MissionsDB.TaskEntries()
                .get(x))
            .filter(x -> x != null)
            .collect(Collectors.toList());
    }

    public boolean meetsConditions(Player player) {
        return conditions.stream()
            .allMatch(x -> x.getCondition()
                .isAllowed(player, x));
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return RegistryTypes.POOL;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }

    @Override
    public Class<Pool> getClassForSerialization() {
        return Pool.class;
    }
}
