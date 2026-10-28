package com.robertx22.age_of_exile.airship;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashSet;
import java.util.WeakHashMap;

/** Stationary, ship-safe attention. Runs once per nearby actor, not once per watching player. */
public final class AirshipCrewLife {
    private AirshipCrewLife() {}
    private static final WeakHashMap<LivingEntity,Attention> MEMORY=new WeakHashMap<>();
    private static final class Attention {
        int target=-1,refresh;final float home;final double phase;
        Attention(LivingEntity npc) { home=npc.getYRot();phase=(npc.getUUID().getLeastSignificantBits()&65535)*.01; }
    }
    public static void register() { ServerTickEvents.END_LEVEL_TICK.register(AirshipCrewLife::tick); }
    private static boolean eligible(LivingEntity npc,ServerPlayer p,double distance) {
        return p.isAlive() && !p.isSpectator() && npc.distanceToSqr(p)<distance*distance && p.hasLineOfSight(npc);
    }
    private static void tick(ServerLevel level) {
        var seen=new HashSet<Integer>();
        for(ServerPlayer observer:level.players()) {
            if(observer.isSpectator()) continue;
            for(LivingEntity npc:level.getEntitiesOfClass(LivingEntity.class,observer.getBoundingBox().inflate(16),e->!AirshipService.npcId(e).isEmpty())) {
                if(!seen.add(npc.getId())) continue;
                Attention a=MEMORY.computeIfAbsent(npc,Attention::new);
                ServerPlayer target=level.getEntity(a.target) instanceof ServerPlayer p && eligible(npc,p,12)?p:null;
                if(target==null || --a.refresh<=0) {
                    // Keep the current listener unless a significantly closer player arrives.
                    double best=target==null?100:npc.distanceToSqr(target)*.65;
                    for(ServerPlayer p:level.players()) if(eligible(npc,p,10) && npc.distanceToSqr(p)<best) { target=p;best=npc.distanceToSqr(p); }
                    a.target=target==null?-1:target.getId();a.refresh=40;
                }
                double time=level.getGameTime()*.035+a.phase;
                float yaw,pitch;
                if(target!=null) {
                    double dx=target.getX()-npc.getX(),dz=target.getZ()-npc.getZ();
                    yaw=CrewMotion.yaw(dx,dz);pitch=CrewMotion.pitch(target.getEyeY()-npc.getEyeY(),Math.hypot(dx,dz));
                } else {
                    yaw=a.home+(float)Math.sin(time*.47)*13;
                    pitch=(float)Math.sin(time*.73)*3;
                }
                // Head leads; torso follows more slowly. No walking/falling off deck, no random teleporting.
                float body=CrewMotion.approach(npc.getYRot(),yaw,1.8F);
                float head=CrewMotion.approach(npc.getYHeadRot(),yaw,6F);
                head=body+Math.clamp(CrewMotion.wrap(head-body),-65F,65F);
                npc.setYRot(body);npc.setYBodyRot(body);npc.setYHeadRot(head);
                npc.setXRot(CrewMotion.approach(npc.getXRot(),pitch,2F));
            }
        }
    }
}
