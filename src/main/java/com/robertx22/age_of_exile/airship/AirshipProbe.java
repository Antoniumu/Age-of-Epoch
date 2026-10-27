package com.robertx22.age_of_exile.airship;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Opt-in dev-client probe. No flag => no world mutation, no bypass command available to players. */
public final class AirshipProbe {
    private static final Path FLAG=Path.of("airship-e2e.flag");
    private AirshipProbe() {}
    public static void tick(MinecraftServer server) {
        if(server.getPlayerList().getPlayers().isEmpty() || !Files.exists(FLAG)) return;
        try {
            ServerPlayer p=server.getPlayerList().getPlayers().get(0);
            if(!Level.OVERWORLD.equals(p.level().dimension())) return;
            String action=Files.readString(FLAG).trim();
            Files.delete(FLAG);
            AirshipData d=AirshipService.data(p);
            ServerLevel level=(ServerLevel)p.level();
            if(!action.equals("summon") && !action.equals("winged") && !d.summoned) throw new IllegalStateException("Mooring was not persisted");
            System.out.println("AIRSHIPTEST SNAP action="+action+" anchor="+d.x+","+d.y+","+d.z+" player="+p.position()+" eye="+level.getBlockState(net.minecraft.core.BlockPos.containing(p.getEyePosition())));
            for(Entity actor:level.getEntitiesOfClass(Entity.class,p.getBoundingBox().inflate(150),e->e.entityTags().contains(AirshipService.TAG)))
                System.out.println("AIRSHIPTEST CREW "+actor.getId()+" "+actor.entityTags()+" pos="+actor.position()+" alive="+actor.isAlive());
            if(action.equals("summon") || action.equals("winged")) {
                if(action.equals("winged")) WingedSailorService.summon(p); else AirshipService.summon(p,true);
                for(Entity actor:level.getEntitiesOfClass(Entity.class,p.getBoundingBox().inflate(150),e->e.entityTags().contains(AirshipService.TAG)))
                    System.out.println("AIRSHIPTEST SPAWN "+actor.getId()+" pos="+actor.position());
                System.out.println("AIRSHIPTEST BUILD anchor="+d.x+","+d.y+","+d.z+" deck="+level.getBlockState(new net.minecraft.core.BlockPos(d.x,d.y,d.z+4)));
                if(!d.summoned) throw new IllegalStateException("No clear mooring");
                p.teleportTo(level,d.x+(d.layout.equals("winged_sailor")?78.5:30.5),d.y+32,d.z+(d.layout.equals("winged_sailor")?85.5:31.5),Set.of(),135,12,false);
                // Spectator gives a stable exterior shot without falling; restored before interactions.
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            } else if(action.equals("accepted")) {
                if(d.stage!=1) throw new IllegalStateException("UI button did not accept quest");
            } else if(action.equals("lift-check")) {
                if(Math.abs(p.getY()-d.y-1)>0.1) throw new IllegalStateException("Lift did not arrive on deck");
            } else if(action.equals("board")) {
                p.teleportTo(level,d.x+0.5,d.y+1,d.z+4.5,Set.of(),180,0,false);
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            } else if(action.equals("lift")) {
                AirshipService.descend(p);
                if(Math.abs(p.getY()-d.groundY)>0.1) throw new IllegalStateException("Descent failed before lift test");
                d.liftReadyAtMillis=0; // Only the dev probe skips the player's five-second exit grace period.
                // AirshipService.tick performs the same lift as for real players.
            } else {
                String npc=action.equals("mara")?"mara":action.equals("quartermaster")?"quartermaster":"captain";
                Entity e=level.getEntitiesOfClass(Entity.class,
                    new AABB(d.x-100,d.y-100,d.z-100,d.x+100,d.y+100,d.z+100),
                    candidate->AirshipService.npcId(candidate).equals(npc)).stream().findFirst().orElseThrow();
                if(!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals("minecraft:mannequin"))
                    throw new IllegalStateException("Crew is not player-shaped");
                if(action.equals("far-reject")) {
                    int before=d.stage;
                    p.teleportTo(level,d.x+0.5,d.y+1,d.z+12.5,Set.of(),180,0,false);
                    AirshipService.accept(p,e.getId());
                    if(d.stage!=before) throw new IllegalStateException("Remote packet advanced campaign");
                } else {
                    double tx=e.getX(),tz=e.getZ()+2.5;float yaw=180;
                    if(d.layout.equals("winged_sailor")) {
                        boolean found=false;
                        for(int[] offset:new int[][]{{0,2},{2,0},{-2,0},{0,-2},{0,1},{1,0},{-1,0},{0,-1}}) {
                            var feet=e.blockPosition().offset(offset[0],0,offset[1]);
                            if(level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir() && level.getBlockState(feet.below()).isSolid()) {
                                tx=feet.getX()+0.5;tz=feet.getZ()+0.5;
                                yaw=(float)Math.toDegrees(Math.atan2(-(e.getX()-tx),e.getZ()-tz));found=true;break;
                            }
                        }
                        if(!found) throw new IllegalStateException("No accessible neighbour for "+npc);
                    }
                    p.teleportTo(level,tx,e.getY(),tz,Set.of(),yaw,0,false);
                    if(action.equals("accept") || action.equals("mara") || action.equals("quartermaster") || action.equals("complete")) {
                        AirshipService.accept(p,e.getId());
                        int expected=switch(action) { case "accept"->1; case "mara"->2; case "quartermaster"->3; default->4; };
                        if(d.stage!=expected) throw new IllegalStateException("Wrong stage "+d.stage);
                    } else AirshipService.open(p,e);
                }
            }
            System.out.println("AIRSHIPTEST PASS "+action+" stage="+d.stage);
        } catch(Exception e) {
            System.out.println("AIRSHIPTEST FAIL "+e);
            e.printStackTrace();
        }
    }
}
