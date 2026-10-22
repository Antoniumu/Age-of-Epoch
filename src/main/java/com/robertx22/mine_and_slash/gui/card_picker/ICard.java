package com.robertx22.mine_and_slash.gui.card_picker;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public interface ICard {

    Identifier getIcon();

    void onClick(Player p);

    List<MutableComponent> getTooltip(Player p);

    List<MutableComponent> getScreenText(Player p);

    MutableComponent getName();
}
