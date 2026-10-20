package com.robertx22.divine_missions.database.db_types;

import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.divine_missions.db_init.RegistryTypes;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;

import java.util.HashMap;

public class TaskEntry implements JsonExileRegistry<TaskEntry>, IAutoGson<TaskEntry> {
    public static TaskEntry SERIALIZER = new TaskEntry();

    public int weight = 1000;
    public String id = "";

    public String task_type = "";
    public String data = "";
    public int min = 1;
    public int max = 3;
    public int seconds = 0;

    public HashMap<String, Integer> worths = new HashMap<>();

    public TaskType getType() {
        return MissionsDB.TaskTypes()
            .get(task_type);
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return RegistryTypes.TASK_ENTRY;
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
    public Class<TaskEntry> getClassForSerialization() {
        return TaskEntry.class;
    }
}
