package com.robertx22.age_of_exile.vanilla_mc.blocks;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

public abstract class BaseTileContainer extends AbstractContainerMenu {

    public int size = 0;

    public BlockPos pos;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    protected BaseTileContainer(int size, MenuType<?> type, int id, Inventory invPlayer) {
        super(type, id);
        this.size = size;

        addPlayerInventory(invPlayer);

    }

    protected int PLAYER_INVENTORY_XPOS() {
        return 8;
    }

    protected int PLAYER_INVENTORY_YPOS() {
        return 125;
    }

    protected int HOTBAR_YPOS() {
        return 183;
    }

    public void addPlayerInventory(Inventory invPlayer) {

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        // Add the players hotbar to the gui - the [xpos, ypos] location of each item
        for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
            int slotNumber = x;
            addSlot(new Slot(invPlayer, slotNumber, PLAYER_INVENTORY_XPOS() + SLOT_X_SPACING * x, HOTBAR_YPOS()));
        }
        // Add the rest of the players inventory to the gui
        for (int y = 0; y < PLAYER_INVENTORY_ROW_COUNT; y++) {
            for (int x = 0; x < PLAYER_INVENTORY_COLUMN_COUNT; x++) {
                int slotNumber = HOTBAR_SLOT_COUNT + y * PLAYER_INVENTORY_COLUMN_COUNT + x;
                int xpos = PLAYER_INVENTORY_XPOS() + x * SLOT_X_SPACING;
                int ypos = PLAYER_INVENTORY_YPOS() + y * SLOT_Y_SPACING;
                addSlot(new Slot(invPlayer, slotNumber, xpos, ypos));
            }
        }
    }

    public boolean isPlayerInventory(int index) {
        return index >= getPlayerInvStart() && index <= getPlayerInvEnd();
    }

    public int getPlayerInvStart() {
        return 0;
    }

    public int getPlayerInvEnd() {
        return this.slots.size() - size - 1;
    }

    public int getContainerStart() {
        return this.slots.size() - size;
    }

    public int getContainerEnd() {
        return this.slots.size();
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {

        ItemStack itemstack = ItemStack.EMPTY;

        try {
            Slot slot = this.slots.get(index);
            if (slot != null && slot.hasItem()) {
                ItemStack itemstack1 = slot.getItem();
                itemstack = itemstack1.copy();

                if (isPlayerInventory(index)) {
                    if (!this.moveItemStackTo(itemstack1, getContainerStart(), getContainerEnd(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, getPlayerInvStart(), getPlayerInvEnd() + 1, false)) {
                    return ItemStack.EMPTY;
                }

                if (itemstack1.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            return itemstack;
        }

        return itemstack;
    }

    // detectAndSendChanges is called every tick and can be used to get listeners and update, BUT it would take a huge rewrite and it's not worth it.

}