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
            if(action.equals("summon")) {
                AirshipService.summon(p,true);
                if(!d.summoned) throw new IllegalStateException("No clear mooring");
                p.teleportTo(level,d.x+30.5,d.y+12,d.z+31.5,Set.of(),135,12,false);
                // Spectator gives a stable exterior shot without falling; restored before interactions.
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            } else if(action.equals("board")) {
                p.teleportTo(level,d.x+0.5,d.y+1,d.z+4.5,Set.of(),180,0,false);
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            } else if(action.equals("lift")) {
                AirshipService.descend(p);
                // AirshipService.tick performs the same lift as for real players.
            } else {
                String npc=action.equals("mara")?"mara":action.equals("quartermaster")?"quartermaster":"captain";
                Entity e=level.getEntitiesOfClass(Entity.class,
                    new AABB(d.x-12,d.y-4,d.z-22,d.x+12,d.y+26,d.z+22),
                    candidate->AirshipService.npcId(candidate).equals(npc)).stream().findFirst().orElseThrow();
                if(action.equals("far-reject")) {
                    int before=d.stage;
                    p.teleportTo(level,d.x+0.5,d.y+1,d.z+12.5,Set.of(),180,0,false);
                    AirshipService.accept(p,e.getId());
                    if(d.stage!=before) throw new IllegalStateException("Remote packet advanced campaign");
                } else {
                    p.teleportTo(level,e.getX(),e.getY(),e.getZ()+2.5,Set.of(),180,0,false);
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
