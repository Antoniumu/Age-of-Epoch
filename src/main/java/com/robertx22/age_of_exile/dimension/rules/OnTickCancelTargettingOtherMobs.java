package com.robertx22.age_of_exile.dimension.rules;

import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class OnTickCancelTargettingOtherMobs {
    public static void cancelTarget(Mob mob) {
        if (mob.tickCount % 20 == 0) {
            if (WorldUtils.isMapWorldClass(mob.level()) || WorldUtils.isSimulationWorld(mob.level())) {
                if (mob.getTarget() instanceof Player == false) {
                    mob.setTarget(null);
                }
                if (WorldUtils.isSimulationWorld(mob.level()) && !mob.isNoAi() && mob.getTarget() == null) {
                    Player nearest = mob.level().getNearestPlayer(mob, 48.0D);
                    if (nearest != null && !nearest.isSpectator() && !nearest.isCreative()) {
                        mob.setTarget(nearest);
                    }
                }
            }
        }
    }
}
