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
    /** Real standing room + unobstructed sight, not merely an empty foot block. */
    public static boolean clearView(ServerLevel level,Entity target,List<? extends Entity> others,BlockPos feet) {
        if(!level.hasChunkAt(feet) || !level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir() || !level.getBlockState(feet.below()).isSolid() || !level.getFluidState(feet.below()).isEmpty()) return false;
        var box=new AABB(feet.getX()+.2,feet.getY(),feet.getZ()+.2,feet.getX()+.8,feet.getY()+1.8,feet.getZ()+.8);
        if(!level.noCollision(target,box)) return false;
        var from=new net.minecraft.world.phys.Vec3(feet.getX()+.5,feet.getY()+1.62,feet.getZ()+.5);
        var to=target.getEyePosition();
        if(level.clip(new net.minecraft.world.level.ClipContext(from,to,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,target)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS) return false;
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
        p.sendSystemMessage(Component.literal("Экипаж обновлён. Новых членов команды: "+added+". Прогресс истории сохранён."));return 1;
    }
}
