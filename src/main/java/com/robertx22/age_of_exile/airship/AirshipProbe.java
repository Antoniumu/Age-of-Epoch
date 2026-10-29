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
    private static int proofPaper,proofCopper;
    private static int ritualEmeralds,ritualIron,jumpEmeralds,jumpPearls,oldX,oldY,oldZ,oldBlocks,jumpStage,jumpFavors;
    private static java.util.Set<java.util.UUID> jumpCrew;
    private static java.util.UUID hostile;
    private static long oldRevision;
    private static int blocks(ServerLevel l,int x,int y,int z){int n=0;for(var pos:net.minecraft.core.BlockPos.betweenClosed(new net.minecraft.core.BlockPos(x-57,y-50,z-34),new net.minecraft.core.BlockPos(x+59,y+49,z+54)))if(!l.getBlockState(pos).isAir())n++;return n;}

    public static void tick(MinecraftServer server) {
        if(!net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment() || server.getPlayerList().getPlayers().isEmpty()) return;
        var observer=server.getPlayerList().getPlayers().getFirst();
        if(observer.level() instanceof ServerLevel serverLevel){DevRouteProbe.trace(serverLevel,observer);DevCrewWalk.tick(serverLevel);}
        if(!Files.exists(FLAG))return;
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
                // CI fixture only: deterministic dry landing when a random spawn is in water or blocked.
                // Production summon/landing checks remain unchanged; the ship geometry is untouched.
                int x=p.blockPosition().getX(),z=p.blockPosition().getZ();
                int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z)+3;
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                    level.setBlock(new net.minecraft.core.BlockPos(x+dx,y,z+dz),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),18);
                p.teleportTo(level,x+.5,y+1,z+.5,Set.of(),p.getYRot(),0,false);
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
            } else if(action.equals("hostile-spawn")) {
                @SuppressWarnings("unchecked") var type=(net.minecraft.world.entity.EntityType<net.minecraft.world.entity.Mob>)net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("zombie"));
                var feet=new net.minecraft.core.BlockPos(d.x,d.y+1,d.z+4);
                if(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(type,level,net.minecraft.world.entity.EntitySpawnReason.NATURAL,feet,level.getRandom()))throw new IllegalStateException("Natural spawning allowed in hub");
                var mob=type.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);mob.snapTo(d.x+.5,d.y+1,d.z+4.5,0,0);level.addFreshEntity(mob);hostile=mob.getUUID();
                if(net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.invoker().interact(p,level,net.minecraft.world.InteractionHand.MAIN_HAND,mob,null)==net.minecraft.world.InteractionResult.FAIL)throw new IllegalStateException("Hostile still invulnerable to attack callback");
            } else if(action.equals("hostile-cleared")) {
                if(level.getEntity(hostile)!=null)throw new IllegalStateException("Sanctuary did not remove hostile");
            } else if(action.equals("jump-stock") || action.equals("jump-recovery-stock")) {
                var h=AirshipHub.state(server);
                if(action.equals("jump-recovery-stock")){h.readyAt=0;d.hubJumpReadyAt=0;AirshipHub.save(server);} // Synthetic clock reset only, not a production cooldown bypass.
                // Explicit operator chunk tickets in the test world, not hidden production generation.
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"forceload add "+(d.x-57)+" "+(d.z-34)+" "+(d.x+59)+" "+(d.z+54));
                oldX=d.x;oldY=d.y;oldZ=d.z;oldRevision=h.revision;jumpStage=d.stage;jumpFavors=d.favorCompleted;oldBlocks=blocks(level,d.x,d.y,d.z);
                jumpCrew=level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(d.x-60,d.y-52,d.z-36,d.x+62,d.y+52,d.z+57),e->!AirshipService.npcId(e).isEmpty()).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
                if(jumpCrew.size()!=8)throw new IllegalStateException("Expected eight original NPCs");
                p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,16));p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL,8));jumpEmeralds=countItem(p,net.minecraft.world.item.Items.EMERALD);jumpPearls=countItem(p,net.minecraft.world.item.Items.ENDER_PEARL);
                p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();int x=d.x+160,z=d.z+4;
                int floor=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z)+3;
                for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)floor=Math.max(floor,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x+dx,z+dz)+3);
                // Explicit test landing fixture above the terrain/water, not a production bypass of site validation.
                for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)level.setBlock(new net.minecraft.core.BlockPos(x+dx,floor,z+dz),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),18);
                p.teleportTo(level,x+.5,floor+1,z+.5,Set.of(),90,-10,false);
            } else if(action.equals("jump-check")) {
                var h=AirshipHub.state(server);if(!h.phase.equals("IDLE")||h.jumpPending||h.revision!=oldRevision+1||h.x==oldX||d.x!=h.x)throw new IllegalStateException("Jump did not update singleton");
                if(countItem(p,net.minecraft.world.item.Items.EMERALD)!=jumpEmeralds-16||countItem(p,net.minecraft.world.item.Items.ENDER_PEARL)!=jumpPearls-8)throw new IllegalStateException("Jump payment wrong");
                if(d.stage!=jumpStage||d.favorCompleted!=jumpFavors||h.readyAt<=System.currentTimeMillis())throw new IllegalStateException("Story/cooldown lost during jump");
                if(blocks(level,oldX,oldY,oldZ)!=0||blocks(level,d.x,d.y,d.z)!=oldBlocks)throw new IllegalStateException("Blocks duplicated or lost");
                for(java.util.UUID id:jumpCrew){Entity npc=level.getEntity(id);if(npc==null||!npc.isAlive()||Math.abs(npc.getX()-d.x)>70)throw new IllegalStateException("Original crew UUID lost or left behind");}
                var restored=new com.google.gson.Gson().fromJson(Files.readString(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/aoe-airship-hub.json")),HubState.class);
                if(!restored.valid()||restored.x!=d.x||restored.revision!=h.revision)throw new IllegalStateException("Relocation not durable");
                System.out.println("AIRSHIPTEST JUMP blocks="+oldBlocks+" originalUUIDs="+jumpCrew+" from="+oldX+","+oldY+","+oldZ+" to="+d.x+","+d.y+","+d.z);
            } else if(action.equals("jump-recovery")) {
                var pending=AirshipHub.state(server);int tx=pending.targetX,ty=pending.targetY,tz=pending.targetZ;
                AirshipJump.interruptForProbe(server);if(AirshipJump.recover(p)!=1)throw new IllegalStateException("Snapshot rollback failed");
                var h=AirshipHub.state(server);
                if(h.jumpPending||!h.phase.equals("IDLE")||h.revision!=oldRevision||h.x!=oldX||blocks(level,oldX,oldY,oldZ)!=oldBlocks||blocks(level,tx,ty,tz)!=0)throw new IllegalStateException("Rollback changed anchor/duplicated geometry");
                if(countItem(p,net.minecraft.world.item.Items.EMERALD)!=jumpEmeralds||countItem(p,net.minecraft.world.item.Items.ENDER_PEARL)!=jumpPearls)throw new IllegalStateException("Failed move charged resources");
                for(var id:jumpCrew){var e=level.getEntity(id);if(e==null||Math.abs(e.getX()-oldX)>70)throw new IllegalStateException("Recovery lost original NPC");}
            } else if(action.equals("adventure-check")) {
                var state=d.adventure;com.robertx22.age_of_exile.airship.adventure.AdventureEngine.migrate(d);
                var engine=com.robertx22.age_of_exile.airship.adventure.AdventureCatalog.personal("captain");
                com.robertx22.age_of_exile.airship.adventure.AdventureEngine.accept(state,engine.id());
                com.robertx22.age_of_exile.airship.adventure.AdventureEngine.record(state,com.robertx22.age_of_exile.airship.adventure.AdventureCatalog.Metric.FISH,"minecraft:cod",1,"synthetic-nbt-fixture");
                com.robertx22.age_of_exile.airship.adventure.AdventureEngine.witnessCall(state);
                com.robertx22.age_of_exile.airship.adventure.AdventureEngine.chooseCall(state,"record");
                var original=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p);var restored=new com.robertx22.age_of_exile.capability.player.RPGPlayerData(p);restored.loadFromNBT(original.saveToNBT());
                var a=restored.airship.adventure;
                if(a.version!=1||!a.callDecision.equals("record")||!a.accepted.contains(engine.id())||a.progress.getOrDefault(engine.id(),0)!=1||!a.facts.contains("call.heard")||!a.events.equals(state.events)||!a.trust.equals(state.trust))throw new IllegalStateException("Adventure NBT lost nested maps/sets/facts");
                state.callDecision="";state.facts.remove("call.heard");state.facts.remove("call.record");
                state.accepted.remove(engine.id());state.progress.remove(engine.id());state.events.remove(engine.id()+":synthetic-nbt-fixture"); // Discard all synthetic gameplay state after testing persistence.
                com.robertx22.age_of_exile.airship.AirshipService.sync(p);
            } else if(action.equals("adventure-choice-check")) {
                if(!d.adventure.facts.contains("call.heard")||!d.adventure.callDecision.equals("record"))throw new IllegalStateException("Native NPC choices did not witness then record call");
            } else if(action.equals("voyage-unaccepted")) {
                var a=d.adventure;
                if(!com.robertx22.age_of_exile.airship.adventure.Voyage.started(a))throw new IllegalStateException("Preparation must start in the native captain UI");
                com.robertx22.age_of_exile.questbook.QuestBookService.onProfessionUsed(p,com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum.COOKING);
                com.robertx22.age_of_exile.questbook.QuestBookService.onStandardDungeonCompleted(p);
                if(a.progress.containsKey("voyage.progress.craft")||a.progress.containsKey("voyage.progress.scout"))throw new IllegalStateException("Unaccepted activity was counted");
            } else if(action.equals("voyage-profession")) {
                // Explicit adapter fixture, NOT a claim that this probe operated a crafting station.
                var book=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p).questBook;int before=book.professionsUsed.size();
                com.robertx22.age_of_exile.questbook.QuestBookService.onProfessionUsed(p,com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum.COOKING);
                if(book.professionsUsed.size()!=before||d.adventure.progress.getOrDefault("voyage.progress.craft",0)!=1||d.adventure.facts.contains("voyage.done.craft"))throw new IllegalStateException("Repeat known profession did not count only for accepted preparation");
            } else if(action.equals("voyage-dungeon")) {
                // Callback-contract fixture, not a naturally completed dungeon expedition.
                com.robertx22.age_of_exile.questbook.QuestBookService.onStandardDungeonCompleted(p);
                if(d.adventure.progress.getOrDefault("voyage.progress.scout",0)!=1)throw new IllegalStateException("Accepted standard dungeon event did not count");
            } else if(action.equals("voyage-proof")) {
                var a=d.adventure;
                if(!com.robertx22.age_of_exile.airship.adventure.Voyage.finished(a)||com.robertx22.age_of_exile.airship.adventure.Voyage.completed(a)!=2||com.robertx22.age_of_exile.airship.adventure.Voyage.active(a)!=0)throw new IllegalStateException("Two different accepted and reported approaches did not finish preparation");
                var original=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p);var restored=new com.robertx22.age_of_exile.capability.player.RPGPlayerData(p);restored.loadFromNBT(original.saveToNBT());var b=restored.airship.adventure;
                if(!b.facts.equals(a.facts)||!b.progress.equals(a.progress)||!b.events.equals(a.events)||!b.lastInvestigationLine.equals(a.lastInvestigationLine))throw new IllegalStateException("Preparation lost on real RPG NBT roundtrip");
            } else if(action.equals("investigation-stock")) {
                proofPaper=countItem(p,net.minecraft.world.item.Items.PAPER);
                p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COPPER_INGOT,4));
                proofCopper=countItem(p,net.minecraft.world.item.Items.COPPER_INGOT);
            } else if(action.equals("investigation-device-check")) {
                if(!d.adventure.facts.contains("evidence.device")||countItem(p,net.minecraft.world.item.Items.COPPER_INGOT)!=proofCopper-4)throw new IllegalStateException("Receiver: initialCopper="+proofCopper+" currentCopper="+countItem(p,net.minecraft.world.item.Items.COPPER_INGOT)+" device="+d.adventure.facts.contains("evidence.device")+" facts="+d.adventure.facts);
            } else if(action.equals("investigation-proof")) {
                var a=d.adventure;
                if(!a.facts.containsAll(java.util.List.of("evidence.water","evidence.device","evidence.witness","evidence.crosscheck","testimony.captain","truth.shared","truth.review_completed"))||a.facts.contains("promise.return_to_crew"))throw new IllegalStateException("Investigation skipped evidence/promise followup");
                if(countItem(p,net.minecraft.world.item.Items.PAPER)!=proofPaper+1||countItem(p,net.minecraft.world.item.Items.COPPER_INGOT)!=proofCopper-4)throw new IllegalStateException("Evidence token or copper accounting failed");
                var original=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p);var restored=new com.robertx22.age_of_exile.capability.player.RPGPlayerData(p);restored.loadFromNBT(original.saveToNBT());
                var b=restored.airship.adventure;
                if(!b.facts.equals(a.facts)||!b.progress.equals(a.progress)||!b.lastInvestigationLine.equals(a.lastInvestigationLine)||!b.events.equals(a.events))throw new IllegalStateException("Evidence ledger/promise lost in actual RPG NBT");
            } else if(action.equals("recon-biome-one")||action.equals("recon-biome-two")) {
                int want=action.equals("recon-biome-one")?0:1;
                if(!com.robertx22.age_of_exile.airship.adventure.Recon.accepted(d.adventure)||com.robertx22.age_of_exile.airship.adventure.Recon.count(d.adventure)!=want)throw new IllegalStateException("Recon must be accepted and previous observation received");
                // Test-only travel: teleport to genuine generated terrain, never inject progress or biome IDs.
                var spot=findReconSpot(level,d,want);
                if(spot==null)throw new IllegalStateException("No two accessible naturally generated biomes near the hub");
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                p.teleportTo(level,spot.getX()+.5,spot.getY(),spot.getZ()+.5,Set.of(),0,0,false);p.resetFallDistance();
                System.out.println("AIRSHIPTEST RECON TELEPORT world="+spot+" biome="+level.getBiome(spot).unwrapKey().map(k->k.identifier().toString()).orElse("?"));
            } else if(action.equals("recon-check-one")||action.equals("recon-check-two")) {
                int want=action.equals("recon-check-one")?1:2;
                if(com.robertx22.age_of_exile.airship.adventure.Recon.count(d.adventure)!=want)throw new IllegalStateException("Server biome observation count is not "+want);
            } else if(action.equals("recon-return")) {
                p.teleportTo(level,d.x+.5,d.y+1,d.z+4.5,Set.of(),0,0,false);p.resetFallDistance();
            } else if(action.equals("recon-proof")) {
                var a=d.adventure;
                if(!a.facts.containsAll(java.util.List.of("council.choice.map","recon.started","recon.accepted","recon.reported","recon.finished"))||com.robertx22.age_of_exile.airship.adventure.Recon.count(a)!=2)throw new IllegalStateException("Recon did not preserve plan and two observations");
                var original=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p);var restored=new com.robertx22.age_of_exile.capability.player.RPGPlayerData(p);restored.loadFromNBT(original.saveToNBT());
                if(!restored.airship.adventure.facts.equals(a.facts)||!restored.airship.adventure.progress.equals(a.progress))throw new IllegalStateException("Recon observations lost on RPG NBT roundtrip");
            } else if(action.equals("council-proof")) {
                var a=d.adventure;
                if(!a.facts.containsAll(java.util.List.of("council.started","council.heard.mara","council.heard.tik","council.choice.map","council.fulfilled","council.closed"))||a.facts.contains("council.promise.pending")||a.progress.getOrDefault("council.fish",0)!=1)throw new IllegalStateException("Council omitted a chosen commitment or its followup");
                var original=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p);var restored=new com.robertx22.age_of_exile.capability.player.RPGPlayerData(p);restored.loadFromNBT(original.saveToNBT());
                if(!restored.airship.adventure.facts.equals(a.facts)||!restored.airship.adventure.progress.equals(a.progress)||!restored.airship.adventure.events.equals(a.events))throw new IllegalStateException("Council facts lost on RPG NBT roundtrip");
            } else if(action.equals("adventure-fish")||action.equals("investigation-fish")||action.equals("council-fish")) {
                var held=p.getMainHandItem().copy(); // Dev rod must not overwrite the copper just stocked into an empty selected slot.
                p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                try {
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FISHING_ROD));
                String objective=action.equals("investigation-fish")?"investigation.water":action.equals("council-fish")?"council.fish":"captain.quiet_water";
                int target=action.equals("council-fish")?1:3;
                for(int attempt=0;attempt<20 && d.adventure.progress.getOrDefault(objective,0)<target;attempt++){
                    p.getMainHandItem().getItem().use(level,p,net.minecraft.world.InteractionHand.MAIN_HAND);
                    var hook=level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,p.getBoundingBox().inflate(32),e->e.getOwner()==p && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals("minecraft:fishing_bobber")).stream().findFirst().orElseThrow();
                    var nibble=hook.getClass().getDeclaredField("nibble");nibble.setAccessible(true);nibble.setInt(hook,20); // Synthetic bite timing ONLY; real vanilla rod retrieval and fishing loot hook below.
                    p.getMainHandItem().getItem().use(level,p,net.minecraft.world.InteractionHand.MAIN_HAND);
                }
                if(d.adventure.progress.getOrDefault(objective,0)!=target)throw new IllegalStateException("Real rod loot did not reach server fishing objective");
                } finally {p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);}
                if(action.equals("investigation-fish")&&countItem(p,net.minecraft.world.item.Items.COPPER_INGOT)!=proofCopper)throw new IllegalStateException("Fishing fixture changed receiver stock");
            } else if(action.equals("adventure-heart-check")) {
                if(!d.adventure.completed.contains("captain.quiet_water")||!d.adventure.facts.contains("memory:captain:confidence")||d.relationshipValue("captain")<=20||d.relationshipValue("captain")>=100)throw new IllegalStateException("Personal scene did not change gated relationship");
                var original=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p);var restored=new com.robertx22.age_of_exile.capability.player.RPGPlayerData(p);restored.loadFromNBT(original.saveToNBT());
                if(restored.airship.relationshipValue("captain")!=d.relationshipValue("captain")||!restored.airship.adventure.completed.equals(d.adventure.completed))throw new IllegalStateException("Relationship memory lost on NBT save");
            } else if(action.equals("adventure-moment-check")) {
                var ad=d.adventure;
                if(com.robertx22.age_of_exile.airship.adventure.CrewMoments.chosen(ad,"captain")!=0)throw new IllegalStateException("Captain personal choice not made through NPC UI");
                var rpg=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p);var restored=new com.robertx22.age_of_exile.capability.player.RPGPlayerData(p);restored.loadFromNBT(rpg.saveToNBT());
                if(com.robertx22.age_of_exile.airship.adventure.CrewMoments.chosen(restored.airship.adventure,"captain")!=0)throw new IllegalStateException("Personal choice lost on NBT roundtrip");
                if(!com.robertx22.age_of_exile.airship.adventure.CrewMoments.choose(ad,"captain",1).isEmpty())throw new IllegalStateException("Personal choice replayed");
                if(com.robertx22.age_of_exile.airship.adventure.CrewMoments.chosen(new com.robertx22.age_of_exile.airship.adventure.AdventureData(),"captain")!=-1)throw new IllegalStateException("Choice leaked to independent player state");
            } else if(action.equals("workshop-setup")) {
                // Fixtures ONLY: Oren's earlier quest/confidence came from UI and real sapling placement;
                // Tik's completion is seeded here, not claimed as an end-to-end delivery test.
                var ad=d.adventure;
                if(com.robertx22.age_of_exile.airship.adventure.CrewMoments.choose(ad,"oren",0).isEmpty())throw new IllegalStateException("Oren moment prerequisites missing");
                ad.completed.add("tik.spare_key");AirshipService.sync(p);
                if(!com.robertx22.age_of_exile.airship.adventure.CrewWorkshop.eligible(ad))throw new IllegalStateException("Workshop fixture failed");
            } else if(action.equals("workshop-private-setup")) {
                // Test-only reset after the consent-path proof; not an ordinary player action.
                d.adventure.facts.removeIf(f->f.startsWith("workshop:"));
                d.adventure.lastInvestigationLine="";AirshipService.sync(p);
                if(!com.robertx22.age_of_exile.airship.adventure.CrewWorkshop.eligible(d.adventure))throw new IllegalStateException("Private-path fixture failed");
            } else if(action.equals("workshop-private-proof")) {
                var ad=d.adventure;
                if(!ad.facts.containsAll(java.util.List.of("workshop:asked","workshop:talk_key","workshop:private","workshop:finished")) || ad.facts.contains("workshop:consent") || ad.facts.contains("workshop:oren_informed"))throw new IllegalStateException("Private branch omitted or leaked a fact");
                var rpg=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p);var restored=new com.robertx22.age_of_exile.capability.player.RPGPlayerData(p);restored.loadFromNBT(rpg.saveToNBT());
                if(!restored.airship.adventure.facts.contains("workshop:private") || !com.robertx22.age_of_exile.airship.adventure.CrewWorkshop.act(restored.airship.adventure,"oren",97).isEmpty() || !com.robertx22.age_of_exile.airship.adventure.CrewWorkshop.act(restored.airship.adventure,"tik",95).isEmpty())throw new IllegalStateException("Private branch did not survive NBT or replayed");
            } else if(action.equals("workshop-proof")) {
                var ad=d.adventure;
                if(!ad.facts.containsAll(java.util.List.of("workshop:asked","workshop:talk_key","workshop:consent","workshop:oren_informed","workshop:finished")))throw new IllegalStateException("Workshop UI omitted a consent step");
                var rpg=com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p);var restored=new com.robertx22.age_of_exile.capability.player.RPGPlayerData(p);restored.loadFromNBT(rpg.saveToNBT());
                if(!restored.airship.adventure.facts.containsAll(ad.facts))throw new IllegalStateException("Workshop facts lost on NBT roundtrip");
                if(!com.robertx22.age_of_exile.airship.adventure.CrewWorkshop.act(ad,"tik",98).isEmpty())throw new IllegalStateException("Workshop reward replayed");
                if(com.robertx22.age_of_exile.airship.adventure.CrewWorkshop.eligible(new com.robertx22.age_of_exile.airship.adventure.AdventureData()))throw new IllegalStateException("Workshop leaked between character states");
            } else if(action.equals("adventure-plant")) {
                if(!d.adventure.accepted.contains("oren.new_roots"))throw new IllegalStateException("Plant quest must first be accepted via native NPC UI");
                int x=d.x+85,z=d.z,y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z)+3;
                for(int i=0;i<4;i++){
                    var floor=new net.minecraft.core.BlockPos(x+i*2,y,z);level.setBlock(floor,net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),18);level.setBlock(floor.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),18);
                    var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_SAPLING);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
                    var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor).add(0,.5,0),net.minecraft.core.Direction.UP,floor,false);
                    ((net.minecraft.world.item.BlockItem)stack.getItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,stack,hit));
                }
                if(d.adventure.progress.getOrDefault("oren.new_roots",0)!=4)throw new IllegalStateException("Real BlockItem placement did not credit four unique saplings");
            } else if(action.equals("side-ui-check")) {
                if((d.favorAccepted&2)==0||(d.favorCompleted&2)!=0)throw new IllegalStateException("Real UI did not explicitly accept Mara side quest");
                if(!AirshipQuestTracker.active(d,com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(p).questBook,0).stream().anyMatch(v->v.contains("0/2 новых сундуков")))throw new IllegalStateException("Missing tracked named progress");
            } else if(action.equals("captain-migration-check")) {
                var actors=level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(d.x-60,d.y-52,d.z-36,d.x+61,d.y+51,d.z+56),e->AirshipService.npcId(e).equals("captain"));
                if(actors.size()!=1)throw new IllegalStateException("Expected one captain");
                var captain=actors.getFirst();var uuid=captain.getUUID();
                AirshipCrewPlacement.repairCaptain(p);
                if(!captain.getUUID().equals(uuid)||!captain.entityTags().contains("aoe_captain_access_v1"))throw new IllegalStateException("Captain migration not completed");
                if(AirshipCrewPlacement.repairCaptain(p))throw new IllegalStateException("Captain migration repeated");
                System.out.println("AIRSHIPTEST CAPTAIN MIGRATION uuid="+uuid+" pos="+captain.position());
            } else if(action.equals("captain-candidate-placement")) {
                if(!AirshipCrewPlacement.experimentCaptain(level,d))throw new IllegalStateException("No safe captain candidate near Mara");
            } else if(action.equals("crew-walk-start")) {
                DevCrewWalk.start(level,p,d);
            } else if(action.equals("crew-walk-block")) {
                DevCrewWalk.block(level,p);
            } else if(action.equals("crew-walk-block-check")) {
                DevCrewWalk.checkBlocked(level);
            } else if(action.equals("crew-walk-check")) {
                DevCrewWalk.check(level);
            } else if(action.startsWith("route-return-start-")) {
                DevRouteProbe.returnStart(level,p,action.substring("route-return-start-".length()));
            } else if(action.startsWith("route-return-result-")) {
                DevRouteProbe.returnResult(level,p,action.substring("route-return-result-".length()));
            } else if(action.startsWith("route-target-")) {
                DevRouteProbe.target(level,p,d,action.substring("route-target-".length()));
            } else if(action.startsWith("route-result-")) {
                DevRouteProbe.result(level,p,d,action.substring("route-result-".length()));
            } else if(action.equals("route-observe")) {
                // Read-only checkpoint for a player actually walking the ship; never moves anyone.
                var crew=level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(d.x-60,d.y-52,d.z-36,d.x+61,d.y+51,d.z+56),e->!AirshipService.npcId(e).isEmpty());
                if(crew.size()!=AirshipStory.IDS.length)throw new IllegalStateException("Expected eight crew for route observation");
                System.out.println("AIRSHIPROUTE PLAYER x="+p.getX()+" y="+p.getY()+" z="+p.getZ()+" alive="+p.isAlive()+" spectator="+p.isSpectator());
                for(var npc:crew)System.out.println("AIRSHIPROUTE NPC="+AirshipService.npcId(npc)+" x="+npc.getX()+" y="+npc.getY()+" z="+npc.getZ()+" distance_sq="+p.distanceToSqr(npc)+" los="+p.hasLineOfSight(npc)+" server_open_range="+(p.isAlive()&&!p.isSpectator()&&npc.isAlive()&&p.distanceToSqr(npc)<=36&&p.hasLineOfSight(npc)));
            } else if(action.equals("crew-floor-audit")) {
                // Observation only. Flat plank connectivity is a lower bound, not a complete route.
                var crew=level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(d.x-60,d.y-52,d.z-36,d.x+61,d.y+51,d.z+56),e->!AirshipService.npcId(e).isEmpty());
                if(crew.size()!=AirshipStory.IDS.length)throw new IllegalStateException("Expected all eight crew for floor audit");
                CrewFloorAudit.log(level,p,new net.minecraft.core.BlockPos(d.x,d.y+1,d.z+4),crew);
            } else if(action.equals("crew-check")) {
                var crew=level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(d.x-100,d.y-100,d.z-100,d.x+100,d.y+100,d.z+100),e->!AirshipService.npcId(e).isEmpty());
                for(String id:AirshipStory.IDS) {
                    var found=crew.stream().filter(e->AirshipService.npcId(e).equals(id)).toList();
                    if(found.size()!=1) throw new IllegalStateException("Expected one "+id+", got "+found.size());
                    if(id.equals("oren") && Math.abs(found.getFirst().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).getBaseValue()-1.6)>.001) throw new IllegalStateException("Oren scale lost");
                    if(id.equals("tik") && !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(found.getFirst().getType()).toString().equals("minecraft:parrot")) throw new IllegalStateException("Tik is not a real bird");
                }
                if(AirshipCrewPlacement.complete(level,crew)!=0) throw new IllegalStateException("Refresh duplicated crew");
                if(d.layout.equals("winged_sailor")) {
                    var bard=crew.stream().filter(e->AirshipService.npcId(e).equals("lysander")).findFirst().orElseThrow();
                    var mara=crew.stream().filter(e->AirshipService.npcId(e).equals("mara")).findFirst().orElseThrow();
                    if(!AirshipCrewPlacement.lysanderConnected(level,bard,mara,crew))throw new IllegalStateException("Lysander has no connected interaction position beside Mara");
                    var uuid=bard.getUUID();bard.removeTag("aoe_lysander_access_v1");
                    if(!AirshipCrewPlacement.repairLysander(p)||!bard.getUUID().equals(uuid)||!AirshipCrewPlacement.lysanderConnected(level,bard,mara,crew))throw new IllegalStateException("Existing Lysander layout migration failed");
                    if(AirshipCrewPlacement.repairLysander(p))throw new IllegalStateException("Lysander layout migration repeated");
                    System.out.println("AIRSHIPTEST PASS lysander-connected-migration uuid="+uuid);
                    var oldIds=new java.util.HashMap<String,java.util.UUID>();
                    for(String id:java.util.List.of("oren","selin","veya","tik"))oldIds.put(id,crew.stream().filter(e->AirshipService.npcId(e).equals(id)).findFirst().orElseThrow().getUUID());
                    AirshipCrewPlacement.repairOtherCrew(p);
                    for(String id:oldIds.keySet()){
                        var member=crew.stream().filter(e->AirshipService.npcId(e).equals(id)).findFirst().orElseThrow();
                        var anchor=crew.stream().filter(e->AirshipService.npcId(e).equals(id.equals("oren")||id.equals("selin")?"quartermaster":"mara")).findFirst().orElseThrow();
                        if(!member.getUUID().equals(oldIds.get(id)))throw new IllegalStateException("Crew UUID changed: "+id);
                        boolean connected=AirshipCrewPlacement.lysanderConnected(level,member,anchor,crew);
                        System.out.println("AIRSHIPTEST CREW ACCESS audit npc="+id+" connected="+connected+" pos="+member.blockPosition());
                    }
                    if(AirshipCrewPlacement.repairOtherCrew(p)!=0)throw new IllegalStateException("Crew migration unexpectedly repeated");
                }
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
    private static net.minecraft.core.BlockPos findReconSpot(ServerLevel level,AirshipData d,int want) {
        for(int radius:new int[]{160,256,384,512,768})for(int i=0;i<16;i++){
            double angle=2*Math.PI*i/16;int x=d.x+(int)Math.round(radius*Math.cos(angle)),z=d.z+(int)Math.round(radius*Math.sin(angle));
            int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            var feet=new net.minecraft.core.BlockPos(x,y,z);
            if(!level.getBlockState(feet).isAir()||!level.getBlockState(feet.above()).isAir()||!level.getBlockState(feet.below()).isSolid()||!level.getFluidState(feet.below()).isEmpty())continue;
            String biome=level.getBiome(feet).unwrapKey().map(k->k.identifier().toString()).orElse("");
            if(!biome.isEmpty()&&!d.adventure.facts.contains("recon.biome:"+biome))return feet;
        }
        return null;
    }
    private static int countItem(ServerPlayer p,net.minecraft.world.item.Item item) { int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))count+=p.getInventory().getItem(i).getCount();return count; }
    private static int countIron(ServerPlayer p) {
        int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++) if(p.getInventory().getItem(i).is(net.minecraft.world.item.Items.IRON_INGOT)) count+=p.getInventory().getItem(i).getCount();return count;
    }
}
