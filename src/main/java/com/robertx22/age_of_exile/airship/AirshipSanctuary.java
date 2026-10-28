package com.robertx22.age_of_exile.airship;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
/** Removes existing/entering hostiles without drops or RPG kill credit; never removes named crew. */
public final class AirshipSanctuary {
    private AirshipSanctuary() {}
    public static void register(){ServerTickEvents.END_LEVEL_TICK.register(level->{
        if(!Level.OVERWORLD.equals(level.dimension()) || level.getGameTime()%10!=0)return;
        HubState h=AirshipHub.state(level.getServer());if(!h.exists)return;
        AABB bounds=new AABB(h.x-65,h.y-56,h.z-40,h.x+66,h.y+55,h.z+60);
        for(Mob mob:level.getEntitiesOfClass(Mob.class,bounds,e->e.getType().getCategory()==MobCategory.MONSTER && AirshipService.npcId(e).isEmpty()))
            if(h.contains(mob.blockPosition().getX(),mob.blockPosition().getY(),mob.blockPosition().getZ()))mob.discard();
    });}
}
