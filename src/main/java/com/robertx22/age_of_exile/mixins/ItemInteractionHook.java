package com.robertx22.age_of_exile.mixins;

import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.ItemStackedOnOtherEvent;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ItemInteractionHook {

    @Inject(method = "doClick", cancellable = true, at = @At(value = "HEAD"))
    public void ageOfEpoch$onContainerClick(int slotId, int button, ContainerInput actionType, Player player, CallbackInfo ci) {
        try {
            if (actionType != ContainerInput.PICKUP || slotId < 0) {
                return;
            }
            AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
            if (slotId >= menu.slots.size()) {
                return;
            }
            Slot slot = menu.slots.get(slotId);
            ItemStack carried = menu.getCarried();
            ItemStack stackedOn = slot.getItem();
            if (carried.isEmpty() || stackedOn.isEmpty()) {
                return;
            }
            ClickAction clickAction = button == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
            SlotAccess carriedAccess = new SlotAccess() {
                @Override public ItemStack get() { return menu.getCarried(); }
                @Override public boolean set(ItemStack s) { menu.setCarried(s); return true; }
            };
            ItemStackedOnOtherEvent event = new ItemStackedOnOtherEvent(carried, stackedOn, slot, clickAction, player, carriedAccess);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                menu.broadcastChanges();
                ci.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
