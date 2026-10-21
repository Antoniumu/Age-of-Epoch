package com.robertx22.age_of_exile.questbook;

import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.main.ForgeEvents;
import com.robertx22.forge_compat.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;

/** Hooks measured quest progress into the mod's existing event buses. */
public final class QuestBookEvents {

    private QuestBookEvents() {
    }

    public static void register() {
        ExileEvents.MOB_DEATH.register(new EventConsumer<ExileEvents.OnMobDeath>() {
            @Override
            public void accept(ExileEvents.OnMobDeath event) {
                QuestBookService.onMobKilled(event.mob, event.killer);
            }
        });

        ExileEvents.ON_CHEST_LOOTED.register(new EventConsumer<ExileEvents.OnChestLooted>() {
            @Override
            public void accept(ExileEvents.OnChestLooted event) {
                if (!event.canceled) {
                    QuestBookService.onChestLooted(event.player);
                }
            }
        });

        ForgeEvents.registerForgeEvent(TickEvent.PlayerTickEvent.class, event -> {
            if (event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player
                && !player.level().isClientSide()
                && player.tickCount % 20 == 0) {
                QuestBookService.onPlayerSecond(player);
            }
        });

        ForgeEvents.registerForgeEvent(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                QuestBookService.onLogout(player);
            }
        });
    }
}
