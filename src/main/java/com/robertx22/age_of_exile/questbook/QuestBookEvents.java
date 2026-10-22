package com.robertx22.age_of_exile.questbook;

import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.library_of_exile.main.ApiForgeEvents;
import net.minecraft.server.level.ServerPlayer;

public final class QuestBookEvents {
    private static boolean registered = false;

    private QuestBookEvents() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ApiForgeEvents.registerForgeEvent(TickEvent.PlayerTickEvent.class, event -> {
            if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer sp) {
                QuestBookService.tickPlayer(sp);
            }
        });
    }
}
