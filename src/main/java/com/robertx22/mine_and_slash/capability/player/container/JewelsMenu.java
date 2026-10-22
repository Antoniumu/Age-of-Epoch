package com.robertx22.mine_and_slash.capability.player.container;

import com.robertx22.mine_and_slash.capability.player.helper.JewelInvHelper;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
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
        com.robertx22.forge_compat.common.NbtStackCompat.setHoverName(itemStack, Component.literal(Words.JEWEL_SOCKET_NOT_AVAILABLE.translate()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (i >= maxJewels) {
                container.setItem(i, itemStack);
            } else if (JewelInvHelper.IsPlaceholder(itemStack)) {
                container.setItem(i, new ItemStack(Items.AIR));
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
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        if (isJewelSlot(pSlotId) || pClickType == ClickType.PICKUP_ALL) {
            return;
        }
        super.clicked(pSlotId, pButton, pClickType, pPlayer);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
        if (isJewelSlot(pSlot.index)) {
            return false;
        }
        return super.canTakeItemForPickAll(pStack, pSlot);
    }
}
