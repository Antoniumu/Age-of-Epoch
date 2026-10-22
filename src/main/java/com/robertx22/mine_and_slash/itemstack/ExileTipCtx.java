package com.robertx22.mine_and_slash.itemstack;

import com.mojang.blaze3d.platform.InputConstants;

import com.robertx22.forge_compat.client.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ExileTipCtx {

    List<Component> tip = new ArrayList<>();

    public Player player;
    public ExileStack stack;

    public boolean shift = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT);
    public boolean alt = InputConstants.isKeyDown(InputConstants.KEY_LALT);

    public ExileTipCtx(List<Component> tip, Player player, ExileStack stack) {
        this.tip = tip;
        this.player = player;
        this.stack = stack;
    }
}
