package com.robertx22.age_of_exile.airship;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
/** Four helices + tilted orbital halo + keel wake. One bounded budget per hub, not per viewer. */
public final class AirshipAmbience {
    private AirshipAmbience() {}
    public static void register(){ServerTickEvents.END_LEVEL_TICK.register(level->{
        if(!Level.OVERWORLD.equals(level.dimension()) || level.getGameTime()%4!=0)return;
        HubState h=AirshipHub.state(level.getServer());if(!h.exists || level.players().stream().noneMatch(p->p.distanceToSqr(h.x,h.y,h.z)<200*200))return;
        volume(level,h.x,h.y,h.z,level.getGameTime()*.025,1);
    });}
    public static void volume(ServerLevel level,double x,double y,double z,double time,double intensity){
        for(int i=0;i<128;i++){
            double u=i/128.0,a=u*Math.PI*8+time,side=(i%2==0?1:-1);
            double px=x+Math.cos(a)*12*(1-u*.45),py=y-5+u*42,pz=z+Math.sin(a)*12*(1-u*.45);
            if(i%4<2){px=x+Math.cos(a)*50;py=y+8+Math.sin(a*2+time)*5;pz=z+10+Math.sin(a)*35;}
            if(i%4==2){px=x+Math.cos(a)*37;py=y+30+Math.sin(a)*18;pz=z+10+Math.sin(a)*25;}
            level.sendParticles(i%3==0?ParticleTypes.PORTAL:ParticleTypes.END_ROD,px,py,pz,1,.12,.12,.12,.008*intensity);
        }
    }
}
