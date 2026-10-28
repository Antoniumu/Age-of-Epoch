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
    private static int ritualEmeralds,ritualIron;
    public static void tick(MinecraftServer server) {
        if(server.getPlayerList().getPlayers().isEmpty() || !Files.exists(FLAG)) return;
        try {
            ServerPlayer p=server.getPlayerList().getPlayers().get(0);
            if(!Level.OVERWORLD.equals(p.level().dimension())) return;
            String action=Files.readString(FLAG).trim();
            Files.delete(FLAG);
            AirshipData d=AirshipService.data(p);
            ServerLevel level=(ServerLevel)p.level();
            if(!action.equals("prepare") && !action.equals("summon") && !action.equals("winged") && !action.equals("ritual-stock") && !d.summoned) throw new IllegalStateException("Mooring was not persisted");
            System.out.println("AIRSHIPTEST SNAP action="+action+" anchor="+d.x+","+d.y+","+d.z+" player="+p.position()+" eye="+level.getBlockState(net.minecraft.core.BlockPos.containing(p.getEyePosition())));
            for(Entity actor:level.getEntitiesOfClass(Entity.class,p.getBoundingBox().inflate(150),e->e.entityTags().contains(AirshipService.TAG)))
                System.out.println("AIRSHIPTEST CREW "+actor.getId()+" "+actor.entityTags()+" pos="+actor.position()+" alive="+actor.isAlive());
            if(action.equals("prepare")) {
                p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                p.setHealth(p.getMaxHealth());
                // Protect the fresh random spawn while waiting for terrain; real quest interactions use survival.
            } else if(action.equals("summon") || action.equals("winged")) {
                if(action.equals("winged")) WingedSailorService.summon(p); else AirshipService.summon(p,true);
                for(Entity actor:level.getEntitiesOfClass(Entity.class,p.getBoundingBox().inflate(150),e->e.entityTags().contains(AirshipService.TAG)))
                    System.out.println("AIRSHIPTEST SPAWN "+actor.getId()+" pos="+actor.position());
                System.out.println("AIRSHIPTEST BUILD anchor="+d.x+","+d.y+","+d.z+" deck="+level.getBlockState(new net.minecraft.core.BlockPos(d.x,d.y,d.z+4)));
                if(!d.summoned) throw new IllegalStateException("No clear mooring");
                p.teleportTo(level,d.x+(d.layout.equals("winged_sailor")?78.5:30.5),d.y+32,d.z+(d.layout.equals("winged_sailor")?85.5:31.5),Set.of(),135,12,false);
                // Spectator gives a stable exterior shot without falling; restored before interactions.
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            } else if(action.equals("ritual-stock")) {
                p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,32));
                p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,16));
                ritualEmeralds=countItem(p,net.minecraft.world.item.Items.EMERALD);ritualIron=countIron(p);
            } else if(action.equals("ritual-paid")) {
                if(!AirshipHub.state(server).exists || countItem(p,net.minecraft.world.item.Items.EMERALD)!=ritualEmeralds-32 || countIron(p)!=ritualIron-16) throw new IllegalStateException("Ritual payment/state mismatch");
            } else if(action.equals("hub-check")) {
                var hub=AirshipHub.state(server);if(!hub.exists || !hub.phase.equals("IDLE") || hub.x!=d.x || hub.y!=d.y || hub.z!=d.z) throw new IllegalStateException("Not attached to singleton");
                var restored=new com.google.gson.Gson().fromJson(Files.readString(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/aoe-airship-hub.json")),HubState.class);
                if(!restored.valid() || !restored.exists || restored.revision!=hub.revision || restored.x!=hub.x) throw new IllegalStateException("Shared hub disk state lost");
                long revision=hub.revision;int stage=d.stage;WingedSailorService.summon(p);
                if(hub.revision!=revision || d.stage!=stage) throw new IllegalStateException("Duplicate summon changed hub/story");
                if(!AirshipHub.protectedAt(level,new net.minecraft.core.BlockPos(d.x,d.y,d.z+4))) throw new IllegalStateException("Lift not protected");
            } else if(action.equals("crew-check")) {
                var crew=level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(d.x-100,d.y-100,d.z-100,d.x+100,d.y+100,d.z+100),e->!AirshipService.npcId(e).isEmpty());
                for(String id:AirshipStory.IDS) {
                    var found=crew.stream().filter(e->AirshipService.npcId(e).equals(id)).toList();
                    if(found.size()!=1) throw new IllegalStateException("Expected one "+id+", got "+found.size());
                    if(id.equals("oren") && Math.abs(found.getFirst().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).getBaseValue()-1.6)>.001) throw new IllegalStateException("Oren scale lost");
                    if(id.equals("tik") && !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(found.getFirst().getType()).toString().equals("minecraft:parrot")) throw new IllegalStateException("Tik is not a real bird");
                }
                if(AirshipCrewPlacement.complete(level,crew)!=0) throw new IllegalStateException("Refresh duplicated crew");
            } else if(action.equals("attention-check")) {
                var captain=level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,p.getBoundingBox().inflate(16),e->AirshipService.npcId(e).equals("captain")).stream().findFirst().orElseThrow();
                float desired=CrewMotion.yaw(p.getX()-captain.getX(),p.getZ()-captain.getZ());
                if(Math.abs(CrewMotion.wrap(captain.getYHeadRot()-desired))>12 || Math.abs(CrewMotion.wrap(captain.getYRot()-desired))>15) throw new IllegalStateException("NPC did not turn toward player");
                System.out.println("AIRSHIPTEST ATTENTION head="+captain.getYHeadRot()+" body="+captain.getYRot()+" desired="+desired);
            } else if(action.equals("finished")) {
                if(d.stage!=11 || d.storyDecision!=1 || d.favorCompleted!=5 || d.captainTrust<20 || d.maraTrust<20 || d.bramTrust<20)
                    throw new IllegalStateException("Final campaign state/rewards wrong: stage="+d.stage+" favors="+d.favorCompleted);
                var original=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p);
                var restored=new com.robertx22.age_of_exile.capability.player.RPGPlayerData(p);
                restored.loadFromNBT(original.saveToNBT());
                var r=restored.airship;
                if(r.stage!=d.stage || r.captainTrust!=d.captainTrust || r.maraTrust!=d.maraTrust || r.bramTrust!=d.bramTrust || r.favorCompleted!=d.favorCompleted || r.favorAccepted!=d.favorAccepted || r.storyDecision!=d.storyDecision || !r.giverSkin.equals(d.giverSkin) || r.baselineDungeons!=d.baselineDungeons)
                    throw new IllegalStateException("Actual RPGPlayerData NBT roundtrip lost campaign state");
                System.out.println("AIRSHIPTEST NBT roundtrip stage="+r.stage+" trust="+r.captainTrust+","+r.maraTrust+","+r.bramTrust+" favors="+r.favorCompleted+" decision="+r.storyDecision);
            } else if(action.startsWith("seed-")) {
                var book=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p).questBook;
                switch(action) {
                    case "seed-chests"->{ for(int i=0;i<3;i++) com.robertx22.age_of_exile.questbook.QuestBookService.onChestLooted(p); }
                    case "seed-kills"->book.mobKills+=10;
                    case "seed-dungeon"->com.robertx22.age_of_exile.questbook.QuestBookService.onStandardDungeonCompleted(p);
                    default->throw new IllegalArgumentException(action);
                }
                com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p).syncToClient(p);
                System.out.println("AIRSHIPTEST SYNTHETIC RPG counters/hook: "+action);
            } else if(action.equals("chapter-one")) {
                if(d.stage!=4) throw new IllegalStateException("Keyboard skip/accept did not finish chapter 1: "+d.stage);
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
                String npc=action.endsWith("mara")?"mara":action.endsWith("quartermaster")?"quartermaster":action.startsWith("report") || action.equals("gate")?AirshipStory.actor(d.stage):"captain";
                for(String candidate:AirshipStory.IDS) if(action.endsWith("-"+candidate)) { npc=candidate;break; }
                final String selected=npc;
                Entity e=level.getEntitiesOfClass(Entity.class,
                    new AABB(d.x-100,d.y-100,d.z-100,d.x+100,d.y+100,d.z+100),
                    candidate->AirshipService.npcId(candidate).equals(selected)).stream().findFirst().orElseThrow();
                if(!npc.equals("tik") && !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals("minecraft:mannequin"))
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
                        var nearbyCrew=level.getEntitiesOfClass(Entity.class,e.getBoundingBox().inflate(10),candidate->!AirshipService.npcId(candidate).isEmpty());
                        for(int[] offset:new int[][]{{0,3},{3,0},{-3,0},{0,-3},{0,2},{2,0},{-2,0},{0,-2},{0,1},{1,0},{-1,0},{0,-1}}) {
                            var feet=e.blockPosition().offset(offset[0],0,offset[1]);
                            if(level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir() && level.getBlockState(feet.below()).isSolid()) {
                                if(AirshipStory.index(npc)>=3 && !AirshipCrewPlacement.clearView(level,e,nearbyCrew,feet)) continue;
                                tx=feet.getX()+0.5;tz=feet.getZ()+0.5;
                                yaw=(float)Math.toDegrees(Math.atan2(-(e.getX()-tx),e.getZ()-tz));found=true;break;
                            }
                        }
                        if(!found) throw new IllegalStateException("No accessible neighbour for "+npc);
                    }
                    float cameraPitch=action.startsWith("photo-")?12:0;
                    if(AirshipStory.index(npc)>=3) cameraPitch=(float)-Math.toDegrees(Math.atan2(e.getBbHeight()*.65-1.62,Math.hypot(e.getX()-tx,e.getZ()-tz)));
                    p.teleportTo(level,tx,e.getY(),tz,Set.of(),yaw,cameraPitch,false);
                    p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                    if(action.startsWith("photo-")) {
                        e.setYRot(yaw+180);
                        if(e instanceof net.minecraft.world.entity.LivingEntity living) living.setYHeadRot(yaw+180);
                    } else if(action.startsWith("attend-")) {
                        e.setYRot(yaw);if(e instanceof net.minecraft.world.entity.LivingEntity living) living.setYHeadRot(yaw);
                    } else if(action.equals("gate") || action.equals("stale")) {
                        int before=d.stage,trust=d.captainTrust+d.maraTrust+d.bramTrust;
                        AirshipService.accept(p,e.getId(),action.equals("stale")?before-1:before,0);
                        if(d.stage!=before || trust!=d.captainTrust+d.maraTrust+d.bramTrust) throw new IllegalStateException("Gate/replay was bypassed");
                    } else if(action.equals("favor-captain") || action.equals("favor-claim-captain")) {
                        int before=d.captainTrust;AirshipService.accept(p,e.getId(),d.stage,2);
                        if(action.equals("favor-claim-captain")) {
                            if(d.captainTrust!=before+15) throw new IllegalStateException("Captain favor was not rewarded");
                            AirshipService.accept(p,e.getId(),d.stage,2);
                            if(d.captainTrust!=before+15) throw new IllegalStateException("Favor replay rewarded twice");
                        } else if((d.favorAccepted&1)==0) throw new IllegalStateException("Favor not accepted");
                    } else if(action.equals("iron-quartermaster")) {
                        p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,16));
                        int before=countIron(p),trust=d.bramTrust;
                        AirshipService.accept(p,e.getId(),d.stage,2);AirshipService.accept(p,e.getId(),d.stage,2);
                        if(countIron(p)!=before-16 || d.bramTrust!=trust+15) throw new IllegalStateException("Iron favor was not atomically consumed/rewarded");
                        AirshipService.accept(p,e.getId(),d.stage,2);
                        if(countIron(p)!=before-16 || d.bramTrust!=trust+15) throw new IllegalStateException("Iron favor replay was rewarded");
                    } else if(action.startsWith("report") || action.equals("truth")) {
                        int before=d.stage;
                        AirshipService.accept(p,e.getId(),before,action.equals("truth")?1:0);
                        if(d.stage!=before+1) throw new IllegalStateException("Campaign did not advance from "+before);
                        if(action.equals("truth") && d.storyDecision!=1) throw new IllegalStateException("Choice was not saved");
                        if(d.stage==11 && (d.captainTrust<20 || d.maraTrust<20 || d.bramTrust<20)) throw new IllegalStateException("Independent trust rewards missing");
                    } else if(action.equals("accept") || action.equals("mara") || action.equals("quartermaster") || action.equals("complete")) {
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
    private static int countItem(ServerPlayer p,net.minecraft.world.item.Item item) { int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))count+=p.getInventory().getItem(i).getCount();return count; }
    private static int countIron(ServerPlayer p) {
        int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++) if(p.getInventory().getItem(i).is(net.minecraft.world.item.Items.IRON_INGOT)) count+=p.getInventory().getItem(i).getCount();return count;
    }
}
