package com.robertx22.forge_compat.event;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ItemStackedOnOtherEvent extends Event {
    private final ItemStack carriedItem;
    private final ItemStack stackedOnItem;
    private final Slot slot;
    private final ClickAction action;
    private final Player player;
    private final SlotAccess carriedSlotAccess;

    public ItemStackedOnOtherEvent(ItemStack carriedItem, ItemStack stackedOnItem, Slot slot, ClickAction action, Player player, SlotAccess carriedSlotAccess) {
        this.carriedItem = carriedItem;
        this.stackedOnItem = stackedOnItem;
        this.slot = slot;
        this.action = action;
        this.player = player;
        this.carriedSlotAccess = carriedSlotAccess;
    }

    public ItemStack getCarriedItem() { return carriedItem; }
    public ItemStack getStackedOnItem() { return stackedOnItem; }
    public Slot getSlot() { return slot; }
    public ClickAction getClickAction() { return action; }
    public Player getPlayer() { return player; }
    public SlotAccess getCarriedSlotAccess() { return carriedSlotAccess; }
}
