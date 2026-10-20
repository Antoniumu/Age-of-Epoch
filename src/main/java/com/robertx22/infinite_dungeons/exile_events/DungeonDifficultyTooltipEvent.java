package com.robertx22.infinite_dungeons.exile_events;

import com.robertx22.infinite_dungeons.database.db_types.DungeonDifficulty;
import com.robertx22.library_of_exile.events.base.ExileEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import java.util.List;

public class DungeonDifficultyTooltipEvent extends ExileEvent {

    public Player player;
    public DungeonDifficulty difficulty;
    public List<Component> tooltip;

    public DungeonDifficultyTooltipEvent(Player player, DungeonDifficulty difficulty, List<Component> tooltip) {
        this.player = player;
        this.difficulty = difficulty;
        this.tooltip = tooltip;
    }
}
