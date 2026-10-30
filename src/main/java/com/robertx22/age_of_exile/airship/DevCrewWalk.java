package com.robertx22.age_of_exile.airship;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Explicit dev-only experiment. It is not registered as production crew behaviour. */
final class DevCrewWalk {
    private static volatile UUID actorId;
    static UUID observedActor(){return actorId;}
    private static List<CrewRoutePlanner.Cell> path=List.of();
    private static int next,steps,pausedTicks,checkedSteps;
    private static double originX,originY,originZ,maxDisplacement;
    private static String state="idle",reason="";
    private DevCrewWalk() {}
    private static AABB at(LivingEntity e,double x,double y,double z){return e.getBoundingBox().move(x-e.getX(),y-e.getY(),z-e.getZ());}
    private static boolean safe(ServerLevel level,LivingEntity actor,AABB box,int y){
        if(!CrewFloorSearch.supported(box.minX,box.minZ,box.maxX,box.maxZ,c->{
            var feet=new BlockPos(c.x(),y,c.z());
            return level.hasChunkAt(feet)&&level.getWorldBorder().isWithinBounds(feet)
                &&level.getBlockState(feet.below()).is(BlockTags.PLANKS)&&level.getFluidState(feet.below()).isEmpty();
        })||!level.noCollision(actor,box))return false;
        return level.getEntitiesOfClass(LivingEntity.class,box.inflate(.05),e->e!=actor&&e.isAlive()).isEmpty();
    }
    private static AABB swept(AABB a,AABB b){return new AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ));}
    static void start(ServerLevel level,ServerPlayer p,AirshipData d){
        if(!AirshipHub.state(level.getServer()).phase.equals("IDLE"))throw new IllegalStateException("Hub not idle");
        var actors=level.getEntitiesOfClass(LivingEntity.class,new AABB(d.x-60,d.y-52,d.z-36,d.x+61,d.y+51,d.z+56),e->AirshipService.npcId(e).equals("veya"));
        if(actors.size()!=1)throw new IllegalStateException("Expected one Veya");
        var actor=actors.getFirst();var pos=actor.blockPosition();int y=pos.getY();
        if(Math.abs(actor.getX()-(pos.getX()+.5))>.01||Math.abs(actor.getZ()-(pos.getZ()+.5))>.01||Math.abs(actor.getY()-y)>.01)throw new IllegalStateException("Veya not at cell centre");
        var start=new CrewRoutePlanner.Cell(pos.getX(),pos.getZ());
        java.util.function.Predicate<CrewRoutePlanner.Cell> canStand=c->safe(level,actor,at(actor,c.x()+.5,y,c.z()+.5),y);
        java.util.function.BiPredicate<CrewRoutePlanner.Cell,CrewRoutePlanner.Cell> canCross=(a,b)->safe(level,actor,swept(at(actor,a.x()+.5,y,a.z()+.5),at(actor,b.x()+.5,y,b.z()+.5)),y);
        List<CrewRoutePlanner.Cell> selected=List.of();
        for(int dx=-3;dx<=3&&selected.isEmpty();dx++)for(int dz=-3;dz<=3;dz++){
            if(Math.abs(dx)+Math.abs(dz)<2)continue;
            var result=CrewRoutePlanner.find(start,new CrewRoutePlanner.Cell(start.x()+dx,start.z()+dz),4,81,canStand,canCross);
            if(!result.path().isEmpty()&&result.path().size()<=7){selected=result.path();break;}
        }
        if(selected.isEmpty())throw new IllegalStateException("No conservative local route for Veya");
        var loop=new java.util.ArrayList<>(selected);for(int i=selected.size()-2;i>=0;i--)loop.add(selected.get(i));
        actorId=actor.getUUID();path=List.copyOf(loop);next=1;steps=0;pausedTicks=0;checkedSteps=-1;originX=actor.getX();originY=actor.getY();originZ=actor.getZ();maxDisplacement=0;state="walking";reason="";
        System.out.println("AIRSHIPWALK START npc=veya uuid="+actorId+" path="+path+" position="+actor.position());
    }
    static void tick(ServerLevel level){
        if(!state.equals("walking"))return;
        if(!(level.getEntity(actorId) instanceof LivingEntity actor)||!actor.isAlive()){state="stopped";reason="missing actor";return;}
        if(!AirshipHub.state(level.getServer()).phase.equals("IDLE")){state="stopped";reason="ship transition";return;}
        if(AirshipConversationLease.active(level,actor)){if(pausedTicks++==0)System.out.println("AIRSHIPWALK PAUSE conversation=true");return;}
        var target=path.get(next);double dx=target.x()+.5-actor.getX(),dz=target.z()+.5-actor.getZ(),distance=Math.hypot(dx,dz);
        double step=Math.min(.035,distance),x=actor.getX()+(distance<1e-8?0:dx/distance*step),z=actor.getZ()+(distance<1e-8?0:dz/distance*step);
        if(!safe(level,actor,swept(actor.getBoundingBox(),at(actor,x,originY,z)),(int)originY)){state="stopped";reason="obstruction or lost support";return;}
        actor.setPos(x,originY,z);steps++;maxDisplacement=Math.max(maxDisplacement,Math.hypot(x-originX,z-originZ));
        if(steps%20==0)System.out.println("AIRSHIPWALK STEP npc=veya step="+steps+" pos="+actor.position());
        if(distance<=.035+1e-8 && ++next==path.size()){state="completed";System.out.println("AIRSHIPWALK COMPLETE npc=veya steps="+steps+" max_distance="+maxDisplacement+" end="+actor.position());}
    }
    static void openConversation(ServerLevel level,ServerPlayer p){
        if(!state.equals("walking")||!(level.getEntity(actorId) instanceof LivingEntity actor))throw new IllegalStateException("No running experiment");
        AirshipService.open(p,actor);
        if(!AirshipConversationLease.active(level,actor))throw new IllegalStateException("Conversation not opened in range");
        System.out.println("AIRSHIPWALK FIXTURE conversation requested");
    }
    static void checkPause(ServerLevel level,ServerPlayer player){
        System.out.println("AIRSHIPWALK PAUSE_RESULT ticks="+pausedTicks+" steps="+steps);
        if(!state.equals("walking")||pausedTicks<40)throw new IllegalStateException("Conversation did not pause crew");
        if(!(level.getEntity(actorId) instanceof LivingEntity actor)||AirshipConversationLease.listener(level,actor)!=player)
            throw new IllegalStateException("Conversation listener not selected");
        float desired=CrewMotion.yaw(player.getX()-actor.getX(),player.getZ()-actor.getZ());
        if(Math.abs(CrewMotion.wrap(actor.getYHeadRot()-desired))>12)
            throw new IllegalStateException("Crew did not face listener");
        System.out.println("AIRSHIPWALK LISTENER npc=veya head="+actor.getYHeadRot()+" desired="+desired);
        if(checkedSteps>=0&&steps!=checkedSteps)throw new IllegalStateException("Crew moved during conversation screen transition");
        checkedSteps=steps;
    }
    static void checkResume(){
        System.out.println("AIRSHIPWALK RESUME_RESULT state="+state+" steps="+steps+" paused="+pausedTicks);
        if(!state.equals("completed"))throw new IllegalStateException("Crew did not resume after dialogue lease expired: "+state);
    }
    /** Dev fixture only: put the test player in the route, then expect a safe stop. */
    static void block(ServerLevel level,ServerPlayer p){
        if(!state.equals("walking") || !(level.getEntity(actorId) instanceof LivingEntity actor))throw new IllegalStateException("No running experiment");
        p.teleportTo(level,actor.getX(),actor.getY(),actor.getZ(),java.util.Set.of(),p.getYRot(),p.getXRot(),false);
        System.out.println("AIRSHIPWALK FIXTURE player placed in actor space");
    }
    static void checkBlocked(ServerLevel level){
        System.out.println("AIRSHIPWALK BLOCKED_RESULT state="+state+" reason="+reason+" steps="+steps);
        if(!state.equals("stopped")||!reason.equals("obstruction or lost support"))throw new IllegalStateException("Did not stop for blocker: "+state+" "+reason);
    }
    static void check(ServerLevel level){
        var actor=actorId==null?null:level.getEntity(actorId);
        System.out.println("AIRSHIPWALK RESULT state="+state+" reason="+reason+" steps="+steps+" max_distance="+maxDisplacement+" actor="+(actor==null?"missing":actor.position()));
        if(!state.equals("completed")||steps<20||maxDisplacement<1||actor==null||actor.distanceToSqr(originX,originY,originZ)>.01)throw new IllegalStateException("Crew experiment failed: "+state+" "+reason);
    }
}
