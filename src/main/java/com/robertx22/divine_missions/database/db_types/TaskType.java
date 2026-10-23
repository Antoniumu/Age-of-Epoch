package com.robertx22.divine_missions.database.db_types;

import com.robertx22.divine_missions.db_init.RegistryTypes;
import com.robertx22.divine_missions.saving.TaskData;
import com.robertx22.library_of_exile.registry.ExileRegistry;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;

public abstract class TaskType implements ExileRegistry<TaskType> {
    public static TaskType SERIALIZER = new TaskType("") {
        @Override
        public boolean isTaskDone(TaskData data, Player player) {
            return false;
        }

        @Override
        public MutableComponent getTranslatable(Player player, TaskData data) {
            return null;
        }
    };

    public int weight = 1000;
    public String id = "";

    public TaskType(String id) {
        this.id = id;
    }

    public boolean isTaskDone(TaskData data, Player player) {
        return data.curr >= data.req;
    }

    public boolean isTaskDoneTooltip(TaskData data, Player player) {
        return isTaskDone(data, player);
    }

    public int getCurrentDoneTooltip(Player player, TaskData data) {
        return data.curr;
    }

    public void spendItems(Player player, TaskData data) {

    }

    public int getRandomTaskCount(TaskEntry task, MissionRarity rar) {
        return (int) (RandomUtils.RandomRange(task.min, task.max) * rar.diff_multi);
    }

    public abstract MutableComponent getTranslatable(Player player, TaskData data);

    @Override
    public ExileRegistryType getExileRegistryType() {
        return RegistryTypes.TASK_TYPE;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }

}
