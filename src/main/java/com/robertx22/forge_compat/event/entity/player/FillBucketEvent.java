package com.robertx22.forge_compat.event.entity.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class FillBucketEvent extends PlayerEvent {
    private final ItemStack current;
    private final Level level;
    private final HitResult target;
    private ItemStack result = ItemStack.EMPTY;

    public FillBucketEvent(Player player, ItemStack current, Level level, HitResult target) {
        super(player);
        this.current = current;
        this.level = level;
        this.target = target;
    }

    public ItemStack getEmptyBucket() { return current; }
    public Level getLevel() { return level; }
    public HitResult getTarget() { return target; }
    public ItemStack getFilledBucket() { return result; }
    public void setFilledBucket(ItemStack bucket) { this.result = bucket; }
}
