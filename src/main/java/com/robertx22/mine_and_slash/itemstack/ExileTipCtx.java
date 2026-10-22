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

    public boolean shift = net.minecraft.client.Minecraft.getInstance().hasShiftDown();
    public boolean alt = net.minecraft.client.Minecraft.getInstance().hasAltDown();

    public ExileTipCtx(List<Component> tip, Player player, ExileStack stack) {
        this.tip = tip;
        this.player = player;
        this.stack = stack;
    }
}
