package com.robertx22.infinite_dungeons.exile_events;

import com.robertx22.infinite_dungeons.database.db_types.DungeonDifficulty;
import com.robertx22.infinite_dungeons.database.db_types.layout.DungeonLayout;
import com.robertx22.library_of_exile.events.base.ExileEvent;
import net.minecraft.world.entity.player.Player;

public class TryStartDungeonEvent extends ExileEvent {

    public Player player;
    public DungeonLayout layout;
    public DungeonDifficulty difficulty;

    public boolean canStart = true;

    public TryStartDungeonEvent(Player player, DungeonLayout layout, DungeonDifficulty difficulty) {
        this.player = player;
        this.layout = layout;
        this.difficulty = difficulty;
    }

    public void doNotAllowStartingDungeon() {
        this.canStart = false;
    }

}
