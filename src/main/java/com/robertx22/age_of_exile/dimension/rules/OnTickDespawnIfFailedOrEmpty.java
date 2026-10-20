package com.robertx22.age_of_exile.dimension.rules;

import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import net.minecraft.world.entity.Mob;

public class OnTickDespawnIfFailedOrEmpty {
    public static void despawn(Mob mob) {

        if (mob.tickCount % 50 == 0) {
            try {
                if (WorldUtils.isMapWorldClass(mob.level)) {
                    if (Load.dungeonData(mob.level).data.get(mob.blockPosition()).data.failedOrEmpty()) {
                        mob.kill();
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
