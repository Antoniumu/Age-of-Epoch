package com.robertx22.mine_and_slash.itemstack;

import com.robertx22.forge_compat.client.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ExileTipCtx {

    List<Component> tip = new ArrayList<>();

    public Player player;
    public ExileStack stack;

    public boolean shift = Screen.hasShiftDown();
    public boolean alt = Screen.hasAltDown();

    public ExileTipCtx(List<Component> tip, Player player, ExileStack stack) {
        this.tip = tip;
        this.player = player;
        this.stack = stack;
    }
}
