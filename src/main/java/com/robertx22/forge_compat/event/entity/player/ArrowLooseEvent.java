package com.robertx22.forge_compat.event.entity.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ArrowLooseEvent extends PlayerEvent {
    private final ItemStack bow;
    private final Level level;
    private final boolean hasAmmo;
    private int charge;

    public ArrowLooseEvent(Player player, ItemStack bow, Level level, int charge, boolean hasAmmo) {
        super(player);
        this.bow = bow;
        this.level = level;
        this.charge = charge;
        this.hasAmmo = hasAmmo;
    }

    public ItemStack getBow() { return bow; }
    public Level getLevel() { return level; }
    public boolean hasAmmo() { return hasAmmo; }
    public int getCharge() { return charge; }
    public void setCharge(int charge) { this.charge = charge; }
}
