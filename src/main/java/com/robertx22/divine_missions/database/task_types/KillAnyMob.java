package com.robertx22.divine_missions.database.task_types;

import com.robertx22.divine_missions.database.TaskTypeIds;
import com.robertx22.divine_missions.database.db_types.TaskType;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.saving.TaskData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

public class KillAnyMob extends TaskType {

    public KillAnyMob() {
        super(TaskTypeIds.ANY_MOB_KILL);
    }

    @Override
    public MutableComponent getTranslatable(Player player, TaskData data) {
        return Component.translatable(DivineMissions.MODID + ".task." + this.id);

    }
}
