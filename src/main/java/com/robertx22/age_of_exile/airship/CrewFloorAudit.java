package com.robertx22.age_of_exile.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import java.util.List;

/** Read-only local floor diagnostic; deliberately ignores stairs, ladders and partial blocks. */
public final class CrewFloorAudit {
    private CrewFloorAudit() {}
    private static CrewFloorSearch.Result scan(ServerLevel level,LivingEntity actor,BlockPos origin,int radius,int maximum){
        return CrewFloorSearch.search(new CrewFloorSearch.Cell(origin.getX(),origin.getZ()),radius,maximum,cell->{
            var feet=new BlockPos(cell.x(),origin.getY(),cell.z());
            // Use the actual scaled actor bounds and require full support under its footprint.
            AABB bounds=actor.getBoundingBox().move(feet.getX()+.5-actor.getX(),feet.getY()-actor.getY(),feet.getZ()+.5-actor.getZ());
            if(!CrewFloorSearch.supported(bounds.minX,bounds.minZ,bounds.maxX,bounds.maxZ,c->{
                var at=new BlockPos(c.x(),origin.getY(),c.z());
                return level.hasChunkAt(at)&&level.getWorldBorder().isWithinBounds(at)&&level.getBlockState(at.below()).is(BlockTags.PLANKS);
            })||!level.noCollision(actor,bounds))return false;
            return level.getEntitiesOfClass(LivingEntity.class,bounds.inflate(.03),e->e!=actor&&e.isAlive()).isEmpty();
        });
    }
    /** Developer probe only: no blocks, actors or saves are changed. */
    public static void log(ServerLevel level,LivingEntity observer,BlockPos lift,List<? extends LivingEntity> crew){
        var fromLift=scan(level,observer,lift,64,8000);
        System.out.println("AIRSHIPFLOOR LIFT y="+lift.getY()+" cells="+fromLift.cells().size()+" truncated="+fromLift.truncated()+" scope=flat_planks_only");
        for(LivingEntity actor:crew){
            var local=scan(level,actor,actor.blockPosition(),4,81);
            boolean nearby=actor.blockPosition().getY()==lift.getY()&&fromLift.cells().stream().anyMatch(c->
                Math.abs((long)c.x()-actor.blockPosition().getX())<=2&&Math.abs((long)c.z()-actor.blockPosition().getZ())<=2);
            System.out.println("AIRSHIPFLOOR NPC="+AirshipService.npcId(actor)+" local="+actor.blockPosition()+" cells="+local.cells().size()+" truncated="+local.truncated()+" lift_near_flat="+nearby);
        }
    }
}
