package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.capability.player.data.PlayerBuffData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.world.entity.player.Player;

public class OnPlayerDeath extends EventConsumer<ExileEvents.OnPlayerDeath> {

    @Override
    public void accept(ExileEvents.OnPlayerDeath event) {
        try {

            Player player = event.player;
            var cd = Load.Unit(player).getCooldowns();

            if (!cd.isOnCooldown("death_event")) {

                Load.Unit(player).setAllDirtyOnLoginEtc();

                cd.setOnCooldown("death_event", 100);

                Load.Unit(player).setEquipsChanged();

                PlayerData data = Load.player(player);

                if (Load.Unit(player).getLevel() > ServerContainer.get().DEATH_PENALTY_START_LEVEL.get()) {
                    Load.Unit(player).onDeathDoPenalty();
                    data.rested_xp.onDeath();
                    data.favor.onDeath(player);
                } else {
                    player.sendSystemMessage(Words.DEATH_PENALTY_AVOIDED.locName(ServerContainer.get().DEATH_PENALTY_START_LEVEL.get()));
                }

                data.buff = new PlayerBuffData(); // we delete all the buff foods and potions on death, if in the future i want some buffs to persist across death, change this
                data.playerDataSync.setDirty();


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

