package com.robertx22.age_of_exile.player_skills.items.backpacks;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
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
        if (isEmpty()) {
            stack.removeTagKey("Items");
        } else {
            writeNBT(StackNBT.getOrCreate(stack));
        }
    }

    public static int getMaxSize() {
        return getSizeBackpack(5);
    }

    private void readNBT(CompoundTag compound) {
        final NonNullList<ItemStack> list = NonNullList.withSize(getMaxSize(), ItemStack.EMPTY);

        ContainerHelper.loadAllItems(compound, list);
        for (int i = 0; i < list.size(); i++) {
            setItem(i, list.get(i));
        }
    }

    private void writeNBT(CompoundTag compound) {
        final NonNullList<ItemStack> list = NonNullList.withSize(getMaxSize(), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            list.set(i, getItem(i));
        }

        ContainerHelper.saveAllItems(compound, list, false);
    }

}