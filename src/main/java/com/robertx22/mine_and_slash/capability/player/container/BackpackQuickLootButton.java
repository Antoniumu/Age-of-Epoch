package com.robertx22.mine_and_slash.capability.player.container;

import com.robertx22.forge_compat.client.ImageButton;
import com.robertx22.forge_compat.client.event.ScreenEvent;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.BackPackLootMenuPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.resources.Identifier;

public class BackpackQuickLootButton extends ImageButton {
    static Identifier TEXTURE = SlashRef.id("textures/gui/backpack_button.png");

    public BackpackQuickLootButton(int pX, int pY, ContainerScreen screen) {
        super(
            pX + ClientConfigs.getConfig().QUICK_LOOT_BUTTON_X_OFFSET.get(),
            pY + ClientConfigs.getConfig().QUICK_LOOT_BUTTON_Y_OFFSET.get(),
            14, 14, 0, 0, 16, TEXTURE, 16, 32,
            x -> Packets.sendToServer(new BackPackLootMenuPacket(
                Minecraft.getInstance().hasShiftDown() ? BackPackLootMenuPacket.Mode.DROP : BackPackLootMenuPacket.Mode.LOOT
            )),
            Gui.MASTER_BACKPACK_LOOT_BUTTON.locName()
        );
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.extractContents(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        if (Minecraft.getInstance().hasShiftDown()) {
            this.setTooltip(Tooltip.create(Gui.MASTER_BACKPACK_LOOT_BUTTON_2.locName()));
        } else {
            this.setTooltip(Tooltip.create(Gui.MASTER_BACKPACK_LOOT_BUTTON.locName()));
        }
    }

    public static void addLootButton(ScreenEvent.Init event) {
        if (!(event instanceof ScreenEvent.Init.Post)) return;
        if (Minecraft.getInstance().gui.screen() instanceof ContainerScreen containerScreen
            && ClientConfigs.getConfig().ENABLE_QUICK_LOOT_BUTTON.get()) {
            int guiW = 176;
            int guiH = 114 + containerScreen.getMenu().getRowCount() * 18;
            int guiLeft = (containerScreen.width - guiW) / 2;
            int guiTop = (containerScreen.height - guiH) / 2;
            event.addListener(new BackpackQuickLootButton(guiLeft + guiW, guiTop, containerScreen));
        }
    }
}
