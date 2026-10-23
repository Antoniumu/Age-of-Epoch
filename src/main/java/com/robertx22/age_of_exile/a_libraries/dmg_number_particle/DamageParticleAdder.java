package com.robertx22.age_of_exile.a_libraries.dmg_number_particle;

import com.robertx22.age_of_exile.gui.overlays.mob_bar.MobBarScreen;
import com.robertx22.age_of_exile.vanilla_mc.packets.DmgNumPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class DamageParticleAdder {

    public static void displayParticle(Entity entity, DmgNumPacket packet) {
        if (entity instanceof LivingEntity living) {
            MobBarScreen.onMobDamaged(living);
        }
        DamageParticleRenderer.PARTICLES.add(new DamageParticle(entity, packet));
    }

}
