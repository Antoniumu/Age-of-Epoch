package com.robertx22.age_of_exile.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.gui.screens.skill_tree.SkillTreeScreen;
import com.robertx22.age_of_exile.saveclasses.PointData;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.Button;

import java.util.HashMap;

import net.minecraft.util.Mth;
public class BaseSelectionScreen extends BaseScreen {

    public Minecraft mc;

    public BaseSelectionScreen() {
        super(Minecraft.getInstance()
            .getWindow()
            .getGuiScaledWidth(), Minecraft.getInstance()
            .getWindow()
            .getGuiScaledHeight());
        this.mc = Minecraft.getInstance();

    }

    int scrollY = 0;

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {

        // this.scrollY += deltaY;

        //scrollY = Mth.clamp(scrollY, -3333, 3333);

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public void goToCenter() {

        this.scrollY = 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 32) { // space
            this.goToCenter();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);

    }

    HashMap<Button, PointData> originalButtonLocMap = new HashMap<>();

    @Override
    protected <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addRenderableWidget(T b) {
        super.addRenderableWidget(b);
        if (b instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
            originalButtonLocMap.put((Button) b, new PointData(widget.getX(), widget.getY()));
        }
        return b;
    }

    @Override
    public void render(PoseStack matrix, int x, int y, float ticks) {

        this.buttons.forEach(b -> {
            if (originalButtonLocMap.containsKey(b)) {
                b.y = (this.originalButtonLocMap.get(b)
                    .y + scrollY);
            }
        });

        SkillTreeScreen.renderBackgroundDirt(this, 0);

        super.render(matrix, x, y, ticks);

        this.buttons.forEach(b -> b.renderToolTip(matrix, x, y));
    }

}


