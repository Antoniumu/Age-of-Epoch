package top.theillusivec4.curios.api.type.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class IDynamicStackHandler {
    private final Container container;

    public IDynamicStackHandler(Container container) {
        this.container = container;
    }

    public int getSlots() {
        return container != null ? container.getContainerSize() : 0;
    }

    public ItemStack getStackInSlot(int slot) {
        if (container == null || slot < 0 || slot >= container.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        ItemStack s = container.getItem(slot);
        return s != null ? s : ItemStack.EMPTY;
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (container != null && slot >= 0 && slot < container.getContainerSize()) {
            container.setItem(slot, stack != null ? stack : ItemStack.EMPTY);
        }
    }
}
