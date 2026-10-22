package com.robertx22.forge_compat.event.entity.player;

import com.robertx22.forge_compat.common.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ArrowNockEvent extends PlayerEvent {
    private final ItemStack bow;
    private final InteractionHand hand;
    private final Level level;
    private final boolean hasAmmo;
    private InteractionResultHolder<ItemStack> action;

    public ArrowNockEvent(Player player, ItemStack item, InteractionHand hand, Level level, boolean hasAmmo) {
        super(player);
        this.bow = item;
        this.hand = hand;
        this.level = level;
        this.hasAmmo = hasAmmo;
    }

    public ItemStack getBow() { return bow; }
    public Level getLevel() { return level; }
    public InteractionHand getHand() { return hand; }
    public boolean hasAmmo() { return hasAmmo; }
    public InteractionResultHolder<ItemStack> getAction() { return action; }
    public void setAction(InteractionResultHolder<ItemStack> action) { this.action = action; }
}
