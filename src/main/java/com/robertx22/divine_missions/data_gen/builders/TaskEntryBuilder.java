package com.robertx22.divine_missions.data_gen.builders;

import com.robertx22.divine_missions.database.TaskTypeIds;
import com.robertx22.divine_missions.database.WorthTypeIds;
import com.robertx22.divine_missions.database.db_types.TaskEntry;
import com.robertx22.divine_missions.util.MinMax;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
public class TaskEntryBuilder {

    public static TaskEntry kill(int seconds, int weight, EntityType type, int worth, MinMax minmax) {
        TaskEntry en = new TaskEntry();
        en.id = "kill_" + BuiltInRegistries.ENTITY_TYPE.getKey(type)
            .getPath();
        en.worths.put(WorthTypeIds.DEFAULT, worth);
        en.min = minmax.min;
        en.max = minmax.max;
        en.weight = weight;
        en.seconds = seconds;
        en.data = BuiltInRegistries.ENTITY_TYPE.getKey(type)
            .toString();
        en.task_type = TaskTypeIds.SPECIFIC_MOB_KILL;
        en.addToSerializables();
        return en;
    }

    public static TaskEntry ofAnyMobKill(int seconds, String id, int worth, MinMax minmax) {
        TaskEntry en = new TaskEntry();
        en.weight = 2000;
        en.id = id;
        en.worths.put(WorthTypeIds.DEFAULT, worth);
        en.worths.put(WorthTypeIds.AGE_OF_EXILE, worth);
        en.seconds = seconds;
        en.min = minmax.min;
        en.max = minmax.max;
        en.task_type = TaskTypeIds.ANY_MOB_KILL;
        en.addToSerializables();
        return en;
    }

    public static TaskEntry lootChest(int seconds, int weight, String id, int worth, MinMax minmax) {
        TaskEntry en = new TaskEntry();
        en.weight = weight;
        en.id = id;
        en.worths.put(WorthTypeIds.DEFAULT, worth);
        en.worths.put(WorthTypeIds.AGE_OF_EXILE, worth);
        en.seconds = seconds;
        en.min = minmax.min;
        en.max = minmax.max;
        en.task_type = TaskTypeIds.LOOT_CHEST;
        en.addToSerializables();
        return en;
    }

    public static TaskEntry collect(int weight, Item item, int worth, MinMax minmax) {
        TaskEntry en = new TaskEntry();
        en.id = "get_" + BuiltInRegistries.ITEM.getKey(item)
            .toString()
            .replace(":", "_");
        en.weight = weight;
        en.data = BuiltInRegistries.ITEM.getKey(item)
            .toString();
        en.worths.put(WorthTypeIds.DEFAULT, worth);
        en.min = minmax.min;
        en.max = minmax.max;
        en.seconds = 60;
        en.task_type = TaskTypeIds.BRING_ITEM;
        en.addToSerializables();
        return en;
    }

}
