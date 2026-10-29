package com.robertx22.age_of_exile.airship;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Opt-in development-only measurement. Targets are candidate cells, not proven routes. */
final class DevRouteProbe {
    private static final Path TARGET=Path.of("airship-route-target.json");
    private static final Gson JSON=new Gson();
    private static BlockPos goal,last;
    private static String npc="";
    private static java.util.UUID player;
    private static long started;
    private DevRouteProbe() {}

    private static List<LivingEntity> crew(ServerLevel level,AirshipData d){
        return level.getEntitiesOfClass(LivingEntity.class,new AABB(d.x-60,d.y-52,d.z-36,d.x+61,d.y+51,d.z+56),e->!AirshipService.npcId(e).isEmpty());
    }
    /** Refuse to run when the optional client mod or protective settings are absent. */
    private static void checkBaritoneSafety() throws Exception {
        Object settings=Class.forName("baritone.api.BaritoneAPI").getMethod("getSettings").invoke(null);
        for(String name:List.of("allowBreak","allowPlace","allowSprint","allowParkour","allowWaterBucketFall","allowInventory")){
            Object setting=settings.getClass().getField(name).get(settings);
            Object value=setting.getClass().getField("value").get(setting);
            if(!Boolean.FALSE.equals(value))throw new IllegalStateException("Unsafe Baritone setting: "+name+"="+value);
        }
        Object breaks=settings.getClass().getField("allowBreakAnyway").get(settings);
        if(!((List<?>)breaks.getClass().getField("value").get(breaks)).isEmpty())throw new IllegalStateException("Baritone break exceptions are not empty");
        Object fall=settings.getClass().getField("maxFallHeightNoWater").get(settings);
        if(!Integer.valueOf(0).equals(fall.getClass().getField("value").get(fall)))throw new IllegalStateException("Baritone fall limit not zero");
        System.out.println("AIRSHIPROUTE BARITONE safety_verified=true");
    }
    static void target(ServerLevel level,ServerPlayer p,AirshipData d,String id) throws Exception {
        checkBaritoneSafety();
        if(AirshipStory.index(id)<0)throw new IllegalArgumentException("Unknown NPC");
        var actors=crew(level,d);var matches=actors.stream().filter(e->AirshipService.npcId(e).equals(id)).toList();
        if(matches.size()!=1)throw new IllegalStateException("Expected exactly one NPC: "+id);
        var target=matches.getFirst();BlockPos choice=null;
        outer:for(int radius=1;radius<=3;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            var candidate=target.blockPosition().offset(dx,0,dz);
            if(AirshipCrewPlacement.clearView(level,target,actors,candidate)){choice=candidate;break outer;}
        }
        goal=choice;last=null;npc=id;player=p.getUUID();started=level.getGameTime();
        if(goal==null){Files.writeString(TARGET,JSON.toJson(Map.of("npc",id,"candidate",false)));System.out.println("AIRSHIPROUTE TARGET npc="+id+" candidate=false");return;}
        Files.writeString(TARGET,JSON.toJson(Map.of("npc",id,"candidate",true,"x",goal.getX(),"y",goal.getY(),"z",goal.getZ())));
        System.out.println("AIRSHIPROUTE TARGET npc="+id+" candidate=true goal="+goal+" start="+p.position());
        trace(level,p);
    }
    static void trace(ServerLevel level,ServerPlayer p){
        if(goal==null||!p.getUUID().equals(player)||level.getGameTime()%4!=0)return;
        var now=p.blockPosition();if(now.equals(last))return;last=now;
        System.out.println("AIRSHIPROUTE STEP npc="+npc+" ticks="+(level.getGameTime()-started)+" x="+p.getX()+" y="+p.getY()+" z="+p.getZ()+" ground="+p.onGround());
    }
    static void result(ServerLevel level,ServerPlayer p,AirshipData d,String id) throws Exception {
        if(!id.equals(npc)||!p.getUUID().equals(player))throw new IllegalStateException("No matching route attempt");
        var actors=crew(level,d);var matches=actors.stream().filter(e->AirshipService.npcId(e).equals(id)).toList();
        if(matches.size()!=1)throw new IllegalStateException("Expected exactly one NPC: "+id);
        var actor=matches.getFirst();boolean nearGoal=goal!=null && Math.abs(p.getX()-(goal.getX()+.5))<=1 && Math.abs(p.getZ()-(goal.getZ()+.5))<=1 && Math.abs(p.getY()-goal.getY())<.6;
        boolean canTalk=p.isAlive()&&!p.isSpectator()&&actor.isAlive()&&p.distanceToSqr(actor)<=36&&p.hasLineOfSight(actor);
        System.out.println("AIRSHIPROUTE RESULT npc="+id+" candidate="+(goal!=null)+" arrived="+nearGoal+" on_ground="+p.onGround()+" los="+p.hasLineOfSight(actor)+" server_open_range="+canTalk+" elapsed_ticks="+(level.getGameTime()-started)+" pos="+p.position()+" scope=candidate_only_no_click");
        // A same-position camera rotation is test assistance, not part of the route.
        // The real right click is sent separately by the client runner.
        boolean clickReady=nearGoal && canTalk && p.onGround() && p.distanceToSqr(actor)<=9;
        if(clickReady){
            double dx=actor.getX()-p.getX(), dz=actor.getZ()-p.getZ();
            double dy=actor.getY()+actor.getBbHeight()*.5-p.getEyeY();
            float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));
            float pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
            p.teleportTo(level,p.getX(),p.getY(),p.getZ(),java.util.Set.of(),yaw,pitch,false);
        }
        Files.writeString(TARGET,JSON.toJson(Map.of("npc",id,"click_ready",clickReady)));
        System.out.println("AIRSHIPROUTE CLICK_PREP npc="+id+" ready="+clickReady+" same_position_camera_assist=true");
        goal=null;player=null;npc="";
    }
}
