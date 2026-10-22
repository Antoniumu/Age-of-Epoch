package com.robertx22.dungeon_realm.event.listeners;

import com.robertx22.dungeon_realm.api.CanEnterMapEvent;
import com.robertx22.dungeon_realm.main.DungeonEntries;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import net.minecraft.world.entity.player.Player;

public class NeedPearlListener extends EventConsumer<CanEnterMapEvent> {
    @Override
    public void accept(CanEnterMapEvent canEnterMapEvent) {
        if (!pearlCheck(canEnterMapEvent.p)) {
            canEnterMapEvent.canEnter = false;
        }
    }

    private boolean pearlCheck(Player p){
        if (p.getInventory().countItem(DungeonEntries.HOME_TP_BACK.get()) < 1) {
            p.sendSystemMessage(DungeonWords.NEED_HOME_PEARL.get(DungeonEntries.HOME_TP_BACK.get().getDefaultInstance().getHoverName()));
            return false;
        }
        return true;
    }
}
