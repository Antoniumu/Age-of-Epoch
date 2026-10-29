package com.robertx22.age_of_exile.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;

/** Adds missing crew only; /airship refresh also re-applies textures to existing moorings. */
public final class AirshipCrewPlacement {
    private AirshipCrewPlacement() {}
    public static LivingEntity create(ServerLevel level,String id) {
        var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(id.equals("tik")?"parrot":"mannequin"));
        Entity e=type.create(level,EntitySpawnReason.COMMAND);
        if(!(e instanceof LivingEntity actor)) return null;
        if(actor instanceof Mob mob) { mob.setNoAi(true);mob.setPersistenceRequired(); }
        actor.setSilent(true);
        actor.setCustomName(Component.literal(AirshipStory.name(id)));actor.setCustomNameVisible(false);
        actor.addTag(AirshipService.TAG);actor.addTag("aoe_npc_"+id);
        var scale=actor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if(scale!=null) scale.setBaseValue(id.equals("oren")?1.6:1);
        return actor;
    }
    public static int complete(ServerLevel level,List<? extends Entity> originals) {
        if(originals.isEmpty()) return 0;
        var crew=new ArrayList<Entity>(originals);int added=0;
        for(String id:AirshipStory.IDS) {
            if(crew.stream().anyMatch(e->AirshipService.npcId(e).equals(id))) continue;
            Entity anchor=crew.stream().filter(e->AirshipService.npcId(e).equals(id.equals("oren")||id.equals("selin")?"quartermaster":id.equals("lysander")?"captain":"mara")).findFirst().orElse(crew.getFirst());
            LivingEntity actor=create(level,id);if(actor==null) continue;
            if(id.equals("lysander")) {
                var mara=crew.stream().filter(e->AirshipService.npcId(e).equals("mara")).findFirst().orElse(null);
                if(mara!=null && placeConnected(level,actor,mara,crew,LYSANDER_LAYOUT)) {
                    level.addFreshEntity(actor);AirshipService.configureSkin(level,actor);crew.add(actor);added++;continue;
                }
                System.err.println("No connected position for Lysander; /airship refresh can retry");
                continue; // Never fall back to an isolated room.
            }
            boolean placed=false;
            search:for(int radius=id.equals("tik")?2:id.equals("selin")?5:3;radius<=13;radius++) for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) {
                if(Math.max(Math.abs(dx),Math.abs(dz))!=radius) continue;
                BlockPos feet=anchor.blockPosition().offset(dx,0,dz);
                if(!level.hasChunkAt(feet) || !level.getWorldBorder().isWithinBounds(feet) || !level.getBlockState(feet.below()).isSolid() || !level.getFluidState(feet.below()).isEmpty()) continue;
                int height=id.equals("oren")?3:2;
                boolean clear=true;for(int y=0;y<height;y++) if(!level.getBlockState(feet.above(y)).isAir()) {clear=false;break;}
                if(!clear) continue;
                actor.snapTo(feet.getX()+.5,feet.getY(),feet.getZ()+.5,anchor.getYRot(),0);
                if(!level.noCollision(actor,actor.getBoundingBox())) continue;
                if(crew.stream().anyMatch(e->e.distanceToSqr(actor)<(id.equals("tik")?4:9))) continue;
                boolean reachable=false;
                for(int[] delta:new int[][]{{0,2},{2,0},{-2,0},{0,-2},{0,3},{3,0},{-3,0},{0,-3}})
                    if(clearView(level,actor,crew,feet.offset(delta[0],0,delta[1]))) {reachable=true;break;}
                if(!reachable) continue;
                level.addFreshEntity(actor);AirshipService.configureSkin(level,actor);crew.add(actor);added++;placed=true;break search;
            }
            if(!placed) System.err.println("No safe crew position for "+id+"; /airship refresh can retry without duplicating actors");
        }
        return added;
    }
    private static final String LYSANDER_LAYOUT="aoe_lysander_access_v1";
    private static final int[][] CARDINAL={{1,0},{-1,0},{0,1},{0,-1}};
    private static boolean standing(ServerLevel level,Entity actor,BlockPos pos) {
        if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.getBlockState(pos).isAir()||!level.getBlockState(pos.above()).isAir()||!level.getBlockState(pos.below()).isSolid()||!level.getFluidState(pos.below()).isEmpty())return false;
        return level.noCollision(actor,new AABB(pos.getX()+.2,pos.getY(),pos.getZ()+.2,pos.getX()+.8,pos.getY()+1.8,pos.getZ()+.8));
    }
    /** Conservative flat walking component starting at an interaction position beside Mara.
     * It does not infer access through closed doors or teleport to an isolated neighbouring cell. */
    private static java.util.Set<BlockPos> connectedFloor(ServerLevel level,Entity actor,Entity mara,List<? extends Entity> crew) {
        var seen=new java.util.LinkedHashSet<BlockPos>();var queue=new java.util.ArrayDeque<BlockPos>();var origin=mara.blockPosition();
        for(int[] delta:new int[][]{{0,2},{2,0},{-2,0},{0,-2},{0,3},{3,0},{-3,0},{0,-3},{0,1},{1,0},{-1,0},{0,-1}}){
            BlockPos p=origin.offset(delta[0],0,delta[1]);
            if(clearView(level,mara,crew,p)&&standing(level,actor,p)&&seen.add(p))queue.add(p);
        }
        while(!queue.isEmpty()&&seen.size()<600){
            BlockPos p=queue.removeFirst();
            for(int[] delta:CARDINAL){var n=p.offset(delta[0],0,delta[1]);
                if(Math.abs(n.getX()-origin.getX())>13||Math.abs(n.getZ()-origin.getZ())>13||seen.contains(n)||!standing(level,actor,n))continue;
                // Don't route the player through another crew member's physical space.
                var box=new AABB(n.getX()+.2,n.getY(),n.getZ()+.2,n.getX()+.8,n.getY()+1.8,n.getZ()+.8);
                if(crew.stream().anyMatch(e->e!=actor&&e.getBoundingBox().inflate(.1).intersects(box)))continue;
                seen.add(n);queue.addLast(n);
            }
        }
        return seen;
    }
    private static boolean placeConnected(ServerLevel level,LivingEntity actor,Entity mara,List<? extends Entity> crew,String tag){
        double oldX=actor.getX(),oldY=actor.getY(),oldZ=actor.getZ();float oldYaw=actor.getYRot(),oldPitch=actor.getXRot();
        var floor=connectedFloor(level,actor,mara,crew);
        for(var feet:floor){
            if(feet.distSqr(mara.blockPosition())<16)continue;
            actor.snapTo(feet.getX()+.5,feet.getY(),feet.getZ()+.5,mara.getYRot(),0);
            if(!level.noCollision(actor,actor.getBoundingBox())||crew.stream().anyMatch(e->e!=actor&&e.distanceToSqr(actor)<9))continue;
            if(!level.getEntitiesOfClass(Entity.class,actor.getBoundingBox().inflate(.3),e->e!=actor&&e.isAlive()).isEmpty())continue;
            boolean interaction=floor.stream().anyMatch(pos->pos.distSqr(feet)<=9&&pos.distSqr(feet)>=1&&clearView(level,actor,crew,pos));
            if(interaction){actor.addTag(tag);return true;}
        }
        actor.snapTo(oldX,oldY,oldZ,oldYaw,oldPitch);return false;
    }
    /** Invoked only by the development probe to assess a candidate captain location. */
    static boolean experimentCaptain(ServerLevel level,AirshipData d){
        var crew=level.getEntitiesOfClass(LivingEntity.class,new AABB(d.x-60,d.y-52,d.z-36,d.x+61,d.y+51,d.z+56),e->!AirshipService.npcId(e).isEmpty());
        var captain=crew.stream().filter(e->AirshipService.npcId(e).equals("captain")).toList();
        var mara=crew.stream().filter(e->AirshipService.npcId(e).equals("mara")).toList();
        if(captain.size()!=1||mara.size()!=1)return false;
        var actor=captain.getFirst();var uuid=actor.getUUID();var old=actor.position();
        boolean placed=placeConnected(level,actor,mara.getFirst(),crew,"aoe_dev_captain_candidate");
        System.out.println("AIRSHIPROUTE CAPTAIN_FIXTURE placed="+placed+" uuid="+uuid+" old="+old+" new="+actor.position());
        return placed && actor.getUUID().equals(uuid);
    }
    /** One-time access migration for the original bridge captain. Never changes ship blocks.
     * A nearby player postpones the move, so a conversation is not interrupted. */
    public static boolean repairCaptain(ServerPlayer player){
        var d=AirshipService.data(player);
        if(!d.summoned||!"winged_sailor".equals(d.layout)||!net.minecraft.world.level.Level.OVERWORLD.equals(player.level().dimension()))return false;
        var level=(ServerLevel)player.level();var hub=AirshipHub.state(level.getServer());
        if(!hub.exists||!"IDLE".equals(hub.phase)||player.distanceToSqr(d.x,d.y,d.z)>100*100)return false;
        var crew=level.getEntitiesOfClass(LivingEntity.class,new AABB(d.x-60,d.y-52,d.z-36,d.x+61,d.y+51,d.z+56),e->!AirshipService.npcId(e).isEmpty());
        var captains=crew.stream().filter(e->AirshipService.npcId(e).equals("captain")).toList();
        var anchors=crew.stream().filter(e->AirshipService.npcId(e).equals("mara")).toList();
        if(captains.size()!=1||anchors.size()!=1)return false;
        var actor=captains.getFirst();String tag="aoe_captain_access_v1";
        if(actor.entityTags().contains(tag))return false;
        // Respect non-original/custom placements and keep existing accessible positions.
        if(actor.getY()<d.y+9||lysanderConnected(level,actor,anchors.getFirst(),crew)){actor.addTag(tag);return false;}
        if(level.players().stream().anyMatch(p->p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(actor)<64))return false;
        var uuid=actor.getUUID();boolean moved=placeConnected(level,actor,anchors.getFirst(),crew,tag);
        if(moved)System.out.println("AIRSHIPTEST CAPTAIN ACCESS relocated uuid="+uuid+" pos="+actor.blockPosition());
        return moved;
    }
    /** Once per existing Winged Sailor, move the same actor, retaining its UUID and all story data. */
    public static boolean repairLysander(ServerPlayer player){
        var d=AirshipService.data(player);if(!d.summoned||!"winged_sailor".equals(d.layout)||!net.minecraft.world.level.Level.OVERWORLD.equals(player.level().dimension()))return false;
        var level=(ServerLevel)player.level();var hub=AirshipHub.state(level.getServer());
        if(!hub.exists||!"IDLE".equals(hub.phase)||player.distanceToSqr(d.x,d.y,d.z)>100*100)return false;
        var box=new AABB(d.x-60,d.y-52,d.z-36,d.x+61,d.y+51,d.z+56);
        var crew=level.getEntitiesOfClass(LivingEntity.class,box,e->!AirshipService.npcId(e).isEmpty());
        var lys=crew.stream().filter(e->AirshipService.npcId(e).equals("lysander")).toList();
        var mara=crew.stream().filter(e->AirshipService.npcId(e).equals("mara")).toList();
        if(lys.size()!=1||mara.size()!=1||lys.getFirst().entityTags().contains(LYSANDER_LAYOUT))return false;
        boolean success=placeConnected(level,lys.getFirst(),mara.getFirst(),crew,LYSANDER_LAYOUT);
        if(success)System.out.println("AIRSHIPTEST LYSANDER relocated uuid="+lys.getFirst().getUUID()+" position="+lys.getFirst().blockPosition());
        return success;
    }
    /** Conservatively repair additional original auto-placed crew only when their local floor
     * is disconnected from their role's established anchor. No world blocks or players move. */
    public static int repairOtherCrew(ServerPlayer player){
        var d=AirshipService.data(player);
        if(!d.summoned||!"winged_sailor".equals(d.layout)||!net.minecraft.world.level.Level.OVERWORLD.equals(player.level().dimension()))return 0;
        var level=(ServerLevel)player.level();var hub=AirshipHub.state(level.getServer());
        if(!hub.exists||!"IDLE".equals(hub.phase)||player.distanceToSqr(d.x,d.y,d.z)>100*100)return 0;
        var crew=level.getEntitiesOfClass(LivingEntity.class,new AABB(d.x-60,d.y-52,d.z-36,d.x+61,d.y+51,d.z+56),e->!AirshipService.npcId(e).isEmpty());
        int moved=0;
        for(String id:List.of("oren","selin","veya","tik")){
            String tag="aoe_crew_access_v1_"+id;
            var found=crew.stream().filter(e->AirshipService.npcId(e).equals(id)).toList();
            String anchorId=id.equals("oren")||id.equals("selin")?"quartermaster":"mara";
            var anchors=crew.stream().filter(e->AirshipService.npcId(e).equals(anchorId)).toList();
            if(found.size()!=1||anchors.size()!=1)continue;
            var actor=found.getFirst();var anchor=anchors.getFirst();
            if(actor.entityTags().contains(tag))continue;
            if(lysanderConnected(level,actor,anchor,crew)){actor.addTag(tag);continue;}
            if(placeConnected(level,actor,anchor,crew,tag)){
                moved++;System.out.println("AIRSHIPTEST CREW ACCESS relocated npc="+id+" uuid="+actor.getUUID()+" pos="+actor.blockPosition());
            }else System.err.println("AIRSHIPTEST CREW ACCESS deferred npc="+id+"; no safe connected slot");
        }
        return moved;
    }
    static boolean lysanderConnected(ServerLevel level,LivingEntity actor,Entity mara,List<? extends Entity> crew){
        var floor=connectedFloor(level,actor,mara,crew);
        return floor.stream().anyMatch(pos->pos.distSqr(actor.blockPosition())<=9&&pos.distSqr(actor.blockPosition())>=1&&clearView(level,actor,crew,pos));
    }
    /** Real standing room + unobstructed sight, not merely an empty foot block. */
    public static boolean clearView(ServerLevel level,Entity target,List<? extends Entity> others,BlockPos feet) {
        if(!level.hasChunkAt(feet) || !level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir() || !level.getBlockState(feet.below()).isSolid() || !level.getFluidState(feet.below()).isEmpty()) return false;
        var box=new AABB(feet.getX()+.2,feet.getY(),feet.getZ()+.2,feet.getX()+.8,feet.getY()+1.8,feet.getZ()+.8);
        if(!level.noCollision(target,box)) return false;
        var from=new net.minecraft.world.phys.Vec3(feet.getX()+.5,feet.getY()+1.62,feet.getZ()+.5);
        var to=target.getEyePosition();
        if(level.clip(new net.minecraft.world.level.ClipContext(from,to,net.minecraft.world.level.ClipContext.Block.VISUAL,net.minecraft.world.level.ClipContext.Fluid.NONE,target)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS) return false;
        for(Entity other:others) if(other!=target && (other.getBoundingBox().inflate(.15).intersects(box) || other.getBoundingBox().inflate(.1).clip(from,to).isPresent())) return false;
        return true;
    }
    public static int refresh(ServerPlayer p) {
        AirshipData d=AirshipService.data(p);
        if(!d.summoned || !p.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD) || p.distanceToSqr(d.x,d.y,d.z)>150*150) {
            p.sendSystemMessage(Component.literal("Подойди к своему дирижаблю, чтобы обновить экипаж."));return 0;
        }
        ServerLevel level=(ServerLevel)p.level();
        var crew=level.getEntitiesOfClass(LivingEntity.class,new AABB(d.x-100,d.y-100,d.z-100,d.x+100,d.y+100,d.z+100),e->!AirshipService.npcId(e).isEmpty());
        for(var actor:crew) AirshipService.configureSkin(level,actor);
        int added=complete(level,crew);
        boolean moved=repairLysander(p);
        int others=repairOtherCrew(p);
        p.sendSystemMessage(Component.literal((moved?"Лисандр перенесён к доступному проходу. ":"")+(others>0?"Перемещено ещё: "+others+". ":"")+"Экипаж обновлён. Новых членов команды: "+added+". Прогресс истории сохранён."));return 1;
    }
}
