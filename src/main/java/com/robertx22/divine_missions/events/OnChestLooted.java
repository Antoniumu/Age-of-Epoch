package com.robertx22.divine_missions.events;

import com.robertx22.divine_missions.database.TaskTypeIds;
import com.robertx22.divine_missions.mission_gen.MissionUtil;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;

public class OnChestLooted extends EventConsumer<ExileEvents.OnChestLooted> {

    @Override
    public void accept(ExileEvents.OnChestLooted event) {
        MissionUtil.tryDoMissions(event.player, x -> {
            if (x.getTaskEntry().task_type.equals(TaskTypeIds.LOOT_CHEST)) {
                x.increaseProgress();
                return;
            }
        });

    }
}
