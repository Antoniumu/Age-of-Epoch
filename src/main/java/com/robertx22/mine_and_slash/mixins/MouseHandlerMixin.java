package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.mixin_ducks.MouseHandlerDuck;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MouseHandler.class)
public class MouseHandlerMixin implements MouseHandlerDuck {


    @Shadow private double xpos;

    @Shadow private double ypos;

    @Override
    public void setXPos(double pos) {
        this.xpos = pos;
    }

    @Override
    public void setYPos(double pos) {
        this.ypos = pos;
    }
}
