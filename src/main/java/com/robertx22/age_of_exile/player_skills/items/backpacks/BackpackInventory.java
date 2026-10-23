package com.robertx22.age_of_exile.player_skills.items.backpacks;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class BackpackInventory extends SimpleContainer {

    protected final ItemStack stack;

    int size;
    Player player;

    public BackpackInventory(Player player, ItemStack stack) {
        super(getMaxSize());
        this.stack = stack;
        this.size = getMaxSize();
        this.player = player;
        setup();
    }

    public static int getSizeBackpack(ItemStack stack) {
        BackpackInfo info = BackpackContainer.getInfo(null, stack);
        return getSizeBackpack(info.extraRows);
    }

    public static int getSizeBackpack(int extraRows) {
        return 9 + extraRows * 9;
    }

    public void setup() {
        readItemStack();
    }

    public ItemStack getStack() {
        return stack;
    }

    public void readItemStack() {
        if (StackNBT.get(stack) != null) {
            readNBT(StackNBT.get(stack));
        }
    }

    public void writeItemStack() {
        CompoundTag tag = StackNBT.getOrCreate(stack);
        if (isEmpty()) {
            tag.remove("Items");
        } else {
            writeNBT(tag);
        }
        StackNBT.set(stack, tag);
    }

    public static int getMaxSize() {
        return getSizeBackpack(5);
    }

    private void readNBT(CompoundTag compound) {
        for (int i = 0; i < getMaxSize(); i++) {
            setItem(i, ItemStack.EMPTY);
        }
        ListTag listtag = compound.getListOrEmpty("Items");
        for (int i = 0; i < listtag.size(); ++i) {
            CompoundTag itemTag = listtag.getCompoundOrEmpty(i);
            int slot = itemTag.getByteOr("Slot", (byte) 0) & 255;
            if (slot >= 0 && slot < getMaxSize()) {
                setItem(slot, StackNBT.loadStack(itemTag));
            }
        }
    }

    private void writeNBT(CompoundTag compound) {
        ListTag listtag = new ListTag();
        for (int i = 0; i < getMaxSize(); ++i) {
            ItemStack itemstack = getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag itemTag = StackNBT.saveStack(itemstack);
                itemTag.putByte("Slot", (byte) i);
                listtag.add(itemTag);
            }
        }
        compound.put("Items", listtag);
    }

}
