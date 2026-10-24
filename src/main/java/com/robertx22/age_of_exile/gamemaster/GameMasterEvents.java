package com.robertx22.age_of_exile.gamemaster;

import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;
import com.robertx22.forge_compat.event.entity.player.PlayerEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStartedEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStoppedEvent;
import com.robertx22.library_of_exile.main.ForgeEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Подключение GameMaster к жизненному циклу сервера и тикам.
 */
public class GameMasterEvents {

    public static void register() {

        ForgeEvents.registerForgeEvent(FMLServerStartedEvent.class, event -> {
            try {
                GameMaster.get()
                    .start(event.getServer());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        ForgeEvents.registerForgeEvent(FMLServerStoppedEvent.class, event -> {
            try {
                GameMaster.get()
                    .stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        ForgeEvents.registerForgeEvent(TickEvent.ServerTickEvent.class, event -> {
            if (event.phase == TickEvent.Phase.END) {
                try {
                    GameMaster.get()
                        .onServerTick(event.server);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Телеметрия: смерти мобов и игроков (с причиной и убийцей).
        ForgeEvents.registerForgeEvent(LivingDeathEvent.class, event -> {
            try {
                if (event.getEntityLiving() == null || event.getEntityLiving()
                    .level()
                    .isClientSide()) {
                    return;
                }
                String cause = event.getSource() == null ? "unknown" : event.getSource()
                    .getMsgId();
                ServerPlayer killer = null;
                if (event.getSource() != null && event.getSource()
                    .getEntity() instanceof ServerPlayer player) {
                    killer = player;
                }
                GameMaster.get()
                    .telemetry()
                    .onLivingDeath(event.getEntityLiving(), cause, killer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        ForgeEvents.registerForgeEvent(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                try {
                    GameMaster.get()
                        .onLogin(player);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        ForgeEvents.registerForgeEvent(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                try {
                    GameMaster.get()
                        .onLogout(player);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
