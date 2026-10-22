package com.robertx22.mine_and_slash.capability.player.container;

import com.robertx22.mine_and_slash.capability.player.helper.JewelInvHelper;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class JewelsMenu extends ChestMenu {
    private Container container;
    private int maxJewels;
    private int firstPlayerInventoryIndex;

    public JewelsMenu(MenuType<?> pType, int pContainerId, Inventory pPlayerInventory, Container pContainer, int pRows, int maxJewels) {
        super(pType, pContainerId, pPlayerInventory, pContainer, pRows);
        this.container = pContainer;
        this.maxJewels = maxJewels;
        this.firstPlayerInventoryIndex = pRows * 9;
        ItemStack itemStack = new ItemStack(JewelInvHelper.GetPlaceholder());
        itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(Words.JEWEL_SOCKET_NOT_AVAILABLE.translate()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (i >= maxJewels) {
                container.setItem(i, itemStack);
            } else if (JewelInvHelper.IsPlaceholder(itemStack)) {
                container.setItem(i, new ItemStack(Items.AIR));
            }
        }
        for (int i = 0; i < this.slots.size(); i++) {
            final int idx = i;
            if (isJewelSlot(idx)) {
                Slot orig = this.slots.get(idx);
                Slot locked = new Slot(orig.container, orig.getContainerSlot(), orig.x, orig.y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                };
                locked.index = idx;
                this.slots.set(idx, locked);
            }
        }
    }

    private boolean isJewelSlot(int pIndex) {
        return pIndex >= maxJewels && pIndex < firstPlayerInventoryIndex;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        if (isJewelSlot(pIndex)) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(pPlayer, pIndex);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
        if (isJewelSlot(pSlot.index)) {
            return false;
        }
        return super.canTakeItemForPickAll(pStack, pSlot);
    }
}
