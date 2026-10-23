package com.robertx22.forge_compat.event.entity.player;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class AttackEntityEvent extends Event {
    private final Player player;
    private final Entity target;

    public AttackEntityEvent(Player player, Entity target) {
        this.player = player;
        this.target = target;
    }

    public Player getPlayer() { return player; }

    public LivingEntity getEntityLiving() { return (LivingEntity) player; }

    public Entity getTarget() { return target; }

    @Override
    public boolean isCancelable() { return true; }
}
