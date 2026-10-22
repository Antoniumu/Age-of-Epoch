package com.robertx22.forge_compat.event.entity.player;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ItemFishedEvent extends PlayerEvent {
    private final NonNullList<ItemStack> stacks = NonNullList.create();
    private final FishingHook hook;
    private int rodDamage;

    public ItemFishedEvent(List<ItemStack> stacks, int rodDamage, FishingHook hook) {
        super(hook != null && hook.getOwner() instanceof Player p ? p : null);
        if (stacks != null) this.stacks.addAll(stacks);
        this.rodDamage = rodDamage;
        this.hook = hook;
    }

    public NonNullList<ItemStack> getDrops() { return stacks; }
    public FishingHook getHookEntity() { return hook; }
    public int getRodDamage() { return rodDamage; }
    public void damageRodBy(int dmg) { this.rodDamage = dmg; }
}
