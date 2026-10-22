package com.robertx22.dungeon_realm.block_entity;

import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.dungeon_realm.item.relic.RelicItemData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;

public class MapDeviceMenu extends AbstractContainerMenu {
    
    private final Container container;
    private final int containerRows;

    public MapDeviceMenu(int containerId, Inventory playerInventory, Container container, int rows) {
        super(MenuType.GENERIC_9x3, containerId);
        this.container = container;
        this.containerRows = rows;

        container.startOpen(playerInventory.player);

        // Add container slots with custom validation
        int containerSlotIndex = 0;
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new MapDeviceSlot(container, containerSlotIndex, 8 + col * 18, 18 + row * 18));
                containerSlotIndex++;
            }
        }

        // Add player inventory slots
        int playerInventoryY = 103 + (rows - 4) * 18;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInventoryY + row * 18));
            }
        }

        // Add player hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, playerInventoryY + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < this.containerRows * 9) {
                if (!this.moveItemStackTo(itemstack1, this.containerRows * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, this.containerRows * 9, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
    
    private static class MapDeviceSlot extends Slot {
        
        public MapDeviceSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
        
        @Override
        public boolean mayPlace(ItemStack stack) {
            // Only allow relic items
            if (!DungeonItemNbt.RELIC.has(stack)) {
                return false;
            }
            
            try {
                RelicItemData newRelicData = DungeonItemNbt.RELIC.loadFrom(stack);
                
                // Check if adding this relic would exceed the type limit
                if (wouldExceedTypeLimit(newRelicData.getType().id, this.getSlotIndex(), stack)) {
                    return false;
                }
                
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        
        private boolean wouldExceedTypeLimit(String relicType, int targetSlot, ItemStack newStack) {
            HashMap<String, Integer> typeCount = new HashMap<>();
            
            // Count existing relics of each type, excluding the target slot
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (i == targetSlot) continue; // Skip the slot we're trying to place into
                
                ItemStack existingStack = container.getItem(i);
                if (!existingStack.isEmpty() && DungeonItemNbt.RELIC.has(existingStack)) {
                    try {
                        RelicItemData existingData = DungeonItemNbt.RELIC.loadFrom(existingStack);
                        typeCount.put(existingData.getType().id, typeCount.getOrDefault(existingData.getType().id, 0) + 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            // Add the new relic to the count
            int currentCount = typeCount.getOrDefault(relicType, 0);
            
            try {
                RelicItemData newRelicData = DungeonItemNbt.RELIC.loadFrom(newStack);
                return currentCount + 1 > newRelicData.getType().max_equipped;
            } catch (Exception e) {
                e.printStackTrace();
                return true; // Reject on error
            }
        }
    }
}