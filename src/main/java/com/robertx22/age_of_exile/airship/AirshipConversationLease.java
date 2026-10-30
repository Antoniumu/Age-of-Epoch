package com.robertx22.age_of_exile.airship;

import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Server-authoritative, short-lived indication that a player is conversing with a crew member.
 * One lease per player; expiry covers disconnects, crashes and transitions between dialogue screens.
 * It is not saved and it never changes story progress. All calls are on the server thread. */
public final class AirshipConversationLease {
    static final int LIFETIME_TICKS=80;
    // Do not retain ServerLevel: its player list would keep weak keys alive after shutdown.
    private record Lease(UUID actor,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,long expires) {}
    private static final WeakHashMap<ServerPlayer,Lease> LEASES=new WeakHashMap<>();
    private AirshipConversationLease() {}
    static boolean unexpired(long now,long expires){return now<expires;}
    public static boolean renew(ServerPlayer player,Entity actor){
        if(player==null||actor==null||!player.isAlive()||player.isSpectator()||player.isRemoved()||
           !(player.level() instanceof ServerLevel level)||actor.level()!=level||!actor.isAlive()||
           AirshipService.npcId(actor).isEmpty()||player.distanceToSqr(actor)>36||!player.hasLineOfSight(actor))return false;
        LEASES.put(player,new Lease(actor.getUUID(),level.dimension(),level.getGameTime()+LIFETIME_TICKS));
        return true;
    }
    /** Stable tie-break regardless of WeakHashMap iteration order. */
    static boolean preferred(double distance,UUID id,double bestDistance,UUID bestId){
        return bestId==null || distance<bestDistance || (distance==bestDistance && id.compareTo(bestId)<0);
    }
    /** The nearest valid listener, with UUID tie-breaking for concurrent conversations. */
    public static ServerPlayer listener(ServerLevel level,Entity actor){
        var it=LEASES.entrySet().iterator();ServerPlayer best=null;double bestDistance=Double.POSITIVE_INFINITY;
        while(it.hasNext()){
            var entry=it.next();var p=entry.getKey();var lease=entry.getValue();
            if(p==null||!p.isAlive()||p.isRemoved()||p.isSpectator()||!p.level().dimension().equals(lease.dimension())||
               !unexpired(p.level().getGameTime(),lease.expires())){it.remove();continue;}
            if(p.level()==level&&actor.isAlive()&&!actor.isRemoved()&&lease.actor().equals(actor.getUUID())){
                double distance=p.distanceToSqr(actor);
                if(distance<=36&&p.hasLineOfSight(actor)&&preferred(distance,p.getUUID(),bestDistance,best==null?null:best.getUUID())){
                    best=p;bestDistance=distance;
                }
            }
        }
        return best;
    }
    public static boolean active(ServerLevel level,Entity actor){return listener(level,actor)!=null;}

}
