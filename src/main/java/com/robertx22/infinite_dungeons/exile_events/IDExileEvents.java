package com.robertx22.infinite_dungeons.exile_events;

import com.robertx22.library_of_exile.events.base.ExileEventCaller;

public class IDExileEvents {

    public static ExileEventCaller<OnItemGivenEvent> ON_ITEM_GIVE = new ExileEventCaller();
    public static ExileEventCaller<TryStartDungeonEvent> TRY_START_DUNGEON = new ExileEventCaller();
    public static ExileEventCaller<DungeonDifficultyTooltipEvent> DIFFICULTY_TOOLTIP = new ExileEventCaller();

    public static void register() {

    }
}
