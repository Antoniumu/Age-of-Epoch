package top.theillusivec4.curios.api.type.inventory;

import net.minecraft.world.Container;

public class ICurioStacksHandler {
    private final IDynamicStackHandler stacks;

    public ICurioStacksHandler(Container container) {
        this.stacks = new IDynamicStackHandler(container);
    }

    public int getSlots() {
        return stacks.getSlots();
    }

    public IDynamicStackHandler getStacks() {
        return stacks;
    }

    public IDynamicStackHandler getCosmeticStacks() {
        return new IDynamicStackHandler(null);
    }
}
