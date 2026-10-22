package com.robertx22.age_of_exile.vanilla_mc.blocks.runeword_station;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashContainers;
import com.robertx22.age_of_exile.vanilla_mc.blocks.BaseTileContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.minecraft.world.entity.player.Inventory;
public class RuneWordStationContainer extends BaseTileContainer {

    Container tile;

    public RuneWordStationContainer(int num, Inventory invPlayer, FriendlyByteBuf packet) {
        this(num, invPlayer, new SimpleContainer(RuneWordStationTile.SLOT_COUNT), packet.readBlockPos());
    }

    public RuneWordStationContainer(int i, Inventory invPlayer, Container inventory,
                                    BlockPos pos) {

        super(RuneWordStationTile.SLOT_COUNT, SlashContainers.RUNEWORD.get(), i, invPlayer);
        this.tile = inventory;
        this.pos = pos;
        int count = 0;

        for (int runeslot : RuneWordStationTile.RUNE_SLOTS) {
            int spacing = runeslot > 2 ? 31 : 0;
            addSlot(new Slot(inventory, count++, 111 + spacing + runeslot * 21, 27));
        }

        addSlot(new Slot(inventory, count++, 179, 26));

        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 9; y++) {
                int xpos = PLAYER_INVENTORY_XPOS() + y * 18;
                int ypos = PLAYER_INVENTORY_YPOS() - 40 + x * 18;
                addSlot(new Slot(inventory, count++, xpos, ypos));
            }
        }

    }

    @Override
    public int PLAYER_INVENTORY_XPOS() {
        return 108;
    }

    @Override
    public int PLAYER_INVENTORY_YPOS() {
        return 113;
    }

    @Override
    protected int HOTBAR_YPOS() {
        return 171;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player playerIn, int index) {
        net.minecraft.world.item.ItemStack result = net.minecraft.world.item.ItemStack.EMPTY;
        try {
            Slot slot = this.slots.get(index);
            if (slot != null && slot.hasItem()) {
                net.minecraft.world.item.ItemStack stackInSlot = slot.getItem();
                result = stackInSlot.copy();

                int runeStart = getContainerStart();
                int runeEnd = runeStart + RuneWordStationTile.RUNE_SLOTS.size();
                int gearSlot = runeEnd;
                int storageStart = gearSlot + RuneWordStationTile.ITEM_SLOT.size();
                int storageEnd = getContainerEnd();

                if (isPlayerInventory(index)) {
                    boolean moved = false;
                    if (stackInSlot.getItem() instanceof com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.RuneItem) {
                        moved = this.moveItemStackTo(stackInSlot, runeStart, runeEnd, false);
                    } else if (com.robertx22.age_of_exile.database.data.gear_slots.GearSlot.getSlotOf(stackInSlot.getItem()) != null) {
                        moved = this.moveItemStackTo(stackInSlot, gearSlot, gearSlot + 1, false);
                    }
                    if (!moved && !this.moveItemStackTo(stackInSlot, storageStart, storageEnd, false)) {
                        return net.minecraft.world.item.ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stackInSlot, getPlayerInvStart(), getPlayerInvEnd() + 1, false)) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }

                if (stackInSlot.isEmpty()) {
                    slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            }
        } catch (Exception e) {
            return result;
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return tile.stillValid(player);
    }

}