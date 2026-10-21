package com.robertx22.age_of_exile.dimension.rules;

import com.robertx22.age_of_exile.damage_hooks.util.AttackInformation;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class OnMobDamageEachOtherDisable {

    public static void onHurtEvent(AttackInformation event) {

        if (event.getAttackerEntity() instanceof Player == false || event.getAttackerEntity() == null) {
            if (event.getTargetEntity() instanceof Player == false) {
                if (WorldUtils.isMapWorldClass(event.getTargetEntity().level())
                    || WorldUtils.isSimulationWorld(event.getTargetEntity().level())) {
                    event.setAmount(0);

                    if (event.getAttackerEntity() instanceof Mob) {
                        Mob mob = (Mob) event.getAttackerEntity();
                        if (mob.getTarget() instanceof Player == false) {
                            mob.setTarget(null);
                        }
                    }

                }
            }
        }
    }
}