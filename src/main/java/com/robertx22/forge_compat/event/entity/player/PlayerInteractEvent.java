package com.robertx22.forge_compat.event.entity.player;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PlayerInteractEvent extends Event {
    private final Player player;
    private final InteractionHand hand;
    private final Level world;
    private final BlockPos pos;
    private final Entity target;

    public PlayerInteractEvent(Player player, InteractionHand hand, Level world, BlockPos pos, Entity target) {
        this.player = player;
        this.hand = hand;
        this.world = world;
        this.pos = pos;
        this.target = target;
    }

    public Player getPlayer() { return player; }

    public Player getEntity() { return player; }

    public InteractionHand getHand() { return hand; }

    public ItemStack getItemStack() {
        return player != null && hand != null ? player.getItemInHand(hand) : ItemStack.EMPTY;
    }

    public Level getWorld() { return world; }

    public BlockPos getPos() { return pos; }

    public Entity getTarget() { return target; }

    @Override
    public boolean isCancelable() { return true; }

    public static class RightClickItem extends PlayerInteractEvent {
        public RightClickItem(Player player, InteractionHand hand, Level world) {
            super(player, hand, world, null, null);
        }
    }

    public static class RightClickBlock extends PlayerInteractEvent {
        public RightClickBlock(Player player, InteractionHand hand, Level world, BlockPos pos) {
            super(player, hand, world, pos, null);
        }
    }

    public static class EntityInteract extends PlayerInteractEvent {
        public EntityInteract(Player player, InteractionHand hand, Entity target) {
            super(player, hand, target.level(), null, target);
        }
    }
}
