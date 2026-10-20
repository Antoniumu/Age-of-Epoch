package com.robertx22.age_of_exile.dimension.rules;

import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class OnTickCancelTargettingOtherMobs {
    public static void cancelTarget(Mob mob) {
        if (mob.tickCount % 50 == 0) {
            if (WorldUtils.isMapWorldClass(mob.level())) {
                if (mob.getTarget() instanceof Player == false) {
                    mob.setTarget(null);
                }
            }
        }
    }
}
