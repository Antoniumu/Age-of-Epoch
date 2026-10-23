package com.robertx22.divine_missions.events;

import com.robertx22.divine_missions.database.TaskTypeIds;
import com.robertx22.divine_missions.mission_gen.MissionUtil;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
public class OnMobKill extends EventConsumer<ExileEvents.OnMobDeath> {

    @Override
    public void accept(ExileEvents.OnMobDeath event) {
        if (event.killer instanceof Player) {

            if (event.mob instanceof Mob || event.mob instanceof Enemy || !event.mob.getType()
                .getCategory()
                .isFriendly()) {

                String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(event.mob.getType())
                    .toString();

                boolean validKill = ExileEvents.IS_KILLED_ENTITY_VALID.callEvents(new ExileEvents.IsEntityKilledValid(event.mob, event.killer)).isValid;
                // Age of Exile can call and say " you killed mob too low level, false! "

                MissionUtil.tryDoMissions((Player) event.killer, x -> {
                    if (x.getTaskType()
                        .GUID()
                        .equals(TaskTypeIds.SPECIFIC_MOB_KILL)) {
                        if (x.getTaskEntry().data.equals(mobId)) {

                            x.increaseProgress();
                            return;
                        }
                    }
                });

                if (validKill) {
                    // only non specific mob kills can be penalized for killing different levels,
                    // because some mob types can only be of certain level ranges.
                    MissionUtil.tryDoMissions((Player) event.killer, x -> {
                        if (x.getTaskType()
                            .GUID()
                            .equals(TaskTypeIds.ANY_MOB_KILL)) {
                            x.increaseProgress();
                            return;
                        }
                    });
                }

            }
        }
    }
}
