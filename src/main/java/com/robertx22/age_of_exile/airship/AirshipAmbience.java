package com.robertx22.age_of_exile.airship;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

/** Bounded per-hub budget, never multiplied by the number of players. */
public final class AirshipAmbience {
    private AirshipAmbience() {}
    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(level->{
            if(!Level.OVERWORLD.equals(level.dimension()) || level.getGameTime()%20!=0) return;
            HubState h=AirshipHub.state(level.getServer());if(!h.exists || level.players().stream().noneMatch(p->p.distanceToSqr(h.x,h.y,h.z)<180*180)) return;
            double phase=level.getGameTime()*.006;
            for(int i=0;i<64;i++) {
                double a=phase+i*Math.PI*2/64;
                double rx=h.layout.equals("winged_sailor")?49:10,rz=h.layout.equals("winged_sailor")?35:19;
                double x=h.x+Math.cos(a)*rx,z=h.z+10+Math.sin(a)*rz,y=h.y+4+Math.sin(a*3+phase)*2;
                level.sendParticles(i%4==0?ParticleTypes.PORTAL:ParticleTypes.END_ROD,x,y,z,1,.08,.12,.08,.006);
            }
        });
    }
}
