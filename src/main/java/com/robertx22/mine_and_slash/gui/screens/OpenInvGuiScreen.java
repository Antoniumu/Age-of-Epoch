package com.robertx22.mine_and_slash.gui.screens;

import com.robertx22.mine_and_slash.gui.bases.IContainerNamedScreen;
import com.robertx22.mine_and_slash.gui.inv_gui.InvGuiGrid;
import com.robertx22.mine_and_slash.gui.inv_gui.InvGuiScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class OpenInvGuiScreen implements IContainerNamedScreen {
    Words word;
    String icon;
    InvGuiGrid grid;

    public OpenInvGuiScreen(Words word, String icon, InvGuiGrid grid) {
        this.word = word;
        this.icon = icon;
        this.grid = grid;
    }

    @Override
    public Identifier iconLocation() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/icons/" + icon + ".png");
    }

    @Override
    public Words screenName() {
        return word;
    }

    @Override
    public void openContainer() {
        Minecraft.getInstance().setScreen(new InvGuiScreen(grid));
    }
}
