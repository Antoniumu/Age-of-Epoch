package com.robertx22.forge_compat.event.entity.player;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemTooltipEvent extends PlayerEvent {
    private final TooltipFlag flags;
    private final ItemStack itemStack;
    private final List<Component> toolTip;

    public ItemTooltipEvent(ItemStack itemStack, Player entityPlayer, List<Component> list, TooltipFlag flags) {
        super(entityPlayer);
        this.itemStack = itemStack;
        this.toolTip = list;
        this.flags = flags;
    }

    public TooltipFlag getFlags() { return flags; }
    public ItemStack getItemStack() { return itemStack; }
    public List<Component> getToolTip() { return toolTip; }
}
