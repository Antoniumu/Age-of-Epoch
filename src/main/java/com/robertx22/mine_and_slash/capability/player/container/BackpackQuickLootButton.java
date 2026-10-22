package com.robertx22.mine_and_slash.capability.player.container;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.BackPackLootMenuPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import com.robertx22.forge_compat.client.Screen;
import com.robertx22.forge_compat.client.ContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import com.robertx22.forge_compat.client.event.ScreenEvent;

public class BackpackQuickLootButton extends ImageButton {
    static Identifier TEXTURE = SlashRef.id("textures/gui/backpack_button.png");
    public BackpackQuickLootButton(int pX, int pY, ContainerScreen screen) {
        super(pX + ClientConfigs.getConfig().QUICK_LOOT_BUTTON_X_OFFSET.get(), pY + ClientConfigs.getConfig().QUICK_LOOT_BUTTON_Y_OFFSET.get(), 14, 14, 0, 0, 16, TEXTURE, 16, 32, x -> Packets.sendToServer(new BackPackLootMenuPacket(Screen.hasShiftDown() ? BackPackLootMenuPacket.Mode.DROP : BackPackLootMenuPacket.Mode.LOOT)), Gui.MASTER_BACKPACK_LOOT_BUTTON.locName());

    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        if (Screen.hasShiftDown()){
            this.setTooltip(Tooltip.create(Gui.MASTER_BACKPACK_LOOT_BUTTON_2.locName()));
        } else {
            this.setTooltip(Tooltip.create(Gui.MASTER_BACKPACK_LOOT_BUTTON.locName()));
        }
    }

    public static void addLootButton(ScreenEvent.Init event){
        if (!(event instanceof ScreenEvent.Init.Post)) return;
        if (Minecraft.getInstance().screen instanceof ContainerScreen containerScreen && ClientConfigs.getConfig().ENABLE_QUICK_LOOT_BUTTON.get()){
            event.addListener(new BackpackQuickLootButton(containerScreen.getGuiLeft() + containerScreen.getXSize(), containerScreen.getGuiTop(), containerScreen));
        }
    }
}
