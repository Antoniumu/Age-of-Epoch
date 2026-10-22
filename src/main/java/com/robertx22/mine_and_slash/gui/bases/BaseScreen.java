package com.robertx22.mine_and_slash.gui.bases;

import com.robertx22.mine_and_slash.event_hooks.player.OnKeyPress;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.SkillTreeScreen;
import com.robertx22.mine_and_slash.gui.screens.stat_gui.StatScreen;
import com.robertx22.mine_and_slash.mmorpg.registers.client.KeybindsRegister;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import com.robertx22.forge_compat.client.Screen;
import net.minecraft.network.chat.Component;

public class BaseScreen extends Screen {

    public BaseScreen(int width, int height) {
        super(Component.literal(""));
        this.sizeX = width;
        this.sizeY = height;
    }

    public Minecraft mc = Minecraft.getInstance();

    public int guiLeft = 0;
    public int guiTop = 0;

    public int sizeX = 0;
    public int sizeY = 0;


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        // todo make this less wack
        // MC 26.3: KeyMapping.matches takes an InputConstants.Key (the compat layer passes the SDL scancode as keyCode)
        if (KeybindsRegister.HUB_SCREEN_KEY.matches(InputConstants.Type.KEYBOARD.getOrCreate(keyCode)) && !StatScreen.SEARCH.isFocused() && !SkillTreeScreen.SEARCH.isFocused()) {
            Minecraft.getInstance().gui.setScreen(null);
            OnKeyPress.cooldown = 5;
            return false;

        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - this.sizeX) / 2;
        this.guiTop = (this.height - this.sizeY) / 2;
    }

    public <T extends AbstractWidget> T publicAddButton(T w) {
        return this.addRenderableWidget(w);
    }

}
