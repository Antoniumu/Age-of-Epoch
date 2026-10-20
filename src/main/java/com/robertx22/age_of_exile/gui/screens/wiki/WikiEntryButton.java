package com.robertx22.age_of_exile.gui.screens.wiki;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.gui.ItemSlotButton;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.resources.Identifier;

public class WikiEntryButton extends ImageButton {

    public static int xSize = 240;
    public static int ySize = 24;

    static Identifier buttonLoc = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/wiki/wiki_entry_button.png");

    WikiEntry entry;

    public WikiEntryButton(WikiScreen screen, WikiEntry entry, int xPos, int yPos) {
        super(xPos, yPos, xSize, ySize, 0, 0, 0, buttonLoc, (button) -> {
        });
        this.entry = entry;

        ItemSlotButton item = new ItemSlotButton(entry.createMainStack(), this.x + 7, this.y + 3);
        item.renderFancyBorder = true;
        screen.publicAddButton(item);

        screen.publicAddButton(new ExtraInfoButton(entry.getExtraInfo(), this.x + 30, this.y + 3));

    }

    @Override
    public void renderButton(PoseStack matrix, int x, int y, float ticks) {
        super.renderButton(matrix, x, y, ticks);

        Minecraft mc = Minecraft.getInstance();

        com.robertx22.forge_compat.client.AbstractGui.drawShadow(mc.font, matrix, entry.getName(), this.x + 60, this.y + 6, com.robertx22.forge_compat.common.CompatChatFormatting.getColor(this.entry.getFormat()));
    }

}

