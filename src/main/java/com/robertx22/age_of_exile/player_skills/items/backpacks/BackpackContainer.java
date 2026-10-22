package com.robertx22.age_of_exile.player_skills.items.backpacks;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashContainers;
import com.robertx22.age_of_exile.vanilla_mc.blocks.BaseTileContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
public class BackpackContainer extends BaseTileContainer {

    ItemStack stack;
    BackpackInventory backpackInv;
    Player player;
    BackpackInfo info;
    net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;

    private static final ThreadLocal<net.minecraft.world.InteractionHand> LAST_READ_HAND =
        ThreadLocal.withInitial(() -> net.minecraft.world.InteractionHand.MAIN_HAND);

    private static ItemStack resolveHandStack(Player player, FriendlyByteBuf buf) {
        net.minecraft.world.InteractionHand h = (buf != null && buf.readableBytes() > 0)
            ? buf.readEnum(net.minecraft.world.InteractionHand.class)
            : net.minecraft.world.InteractionHand.MAIN_HAND;
        LAST_READ_HAND.set(h);
        return player.getItemInHand(h);
    }

    public BackpackContainer(int syncid, Inventory playerinv, FriendlyByteBuf buf) {
        this(resolveHandStack(playerinv.player, buf), syncid, playerinv, LAST_READ_HAND.get());
    }

    public static BackpackInfo getInfo(Player player, ItemStack stack) {
        return new BackpackInfo(player, stack);
    }

    public BackpackContainer(ItemStack stack, int id, Inventory invPlayer) {
        this(stack, id, invPlayer, net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    public BackpackContainer(ItemStack stack, int id, Inventory invPlayer, net.minecraft.world.InteractionHand hand) {
        super(BackpackInventory.getSizeBackpack(stack), SlashContainers.BACKPACK.get(), id, invPlayer);

        try {
            this.player = invPlayer.player;
            this.hand = hand != null ? hand : net.minecraft.world.InteractionHand.MAIN_HAND;

            if (!StackNBT.has(stack)) {
                StackNBT.set(stack, new CompoundTag());
            }
            if (!StackNBT.get(stack)
                .contains("id")) {
                CompoundTag tag = StackNBT.getOrCreate(stack);
                tag.putString("id", UUID.randomUUID().toString());
                StackNBT.set(stack, tag); // so 2 backpacks can never be the same
            }
            this.stack = stack.copy(); // copy so i can later check if its same stack

            this.info = getInfo(player, stack);

            this.backpackInv = new BackpackInventory(invPlayer.player, stack);

            int rows = 1 + info.extraRows;

            int num = 0;
            for (int y = 0; y < rows; ++y) {
                for (int x = 0; x < 9; ++x) {

                    int xpos = 8 + x * 18;

                    int ypos = 37 + (y) * 18;
                    this.addSlot(new BackpackSlot(backpackInv, num, xpos, ypos));

                    num++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected int PLAYER_INVENTORY_YPOS() {
        return 150;
    }

    @Override
    protected int HOTBAR_YPOS() {
        return 208;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (backpackInv != null) {
            backpackInv.writeItemStack();
        }
    }

    @Override
    public void removed(Player p) {
        if (backpackInv != null) {
            backpackInv.writeItemStack();
        }
        super.removed(p);
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack held = player.getItemInHand(hand);
        try {
            if (!StackNBT.getOrCreate(held)
                .getStringOr("id", "")
                .equals(StackNBT.get(stack)
                    .getStringOr("id", ""))) {
                return false;
            }
        } catch (Exception e) {
        }

        if (held.getItem() != stack.getItem()) {
            return false;
        }
        return true;
    }

    private class BackpackSlot extends Slot {

        public BackpackSlot(BackpackInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            if (getItem().getItem() instanceof BackpackItem) {
                return false;
            }
            return true;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.getItem() instanceof BackpackItem) {
                return false;
            }

            return true;
        }
    }
}
