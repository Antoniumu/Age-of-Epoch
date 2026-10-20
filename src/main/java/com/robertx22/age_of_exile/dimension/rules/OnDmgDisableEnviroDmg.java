package com.robertx22.age_of_exile.dimension.rules;

import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.world.entity.player.Player;

public class OnDmgDisableEnviroDmg {

    public static void accept(ExileEvents.OnDamageEntity event) {
        if (event.mob instanceof Player == false) {
            if (WorldUtils.isMapWorldClass(event.mob.level)) {
                if (!event.source.isBypassInvul()) {
                    event.damage = 0;
                }
            }
        }
    }
}
