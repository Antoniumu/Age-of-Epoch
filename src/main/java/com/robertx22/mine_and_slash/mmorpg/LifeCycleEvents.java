package com.robertx22.mine_and_slash.mmorpg;

import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.registers.server.CommandRegister;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.LevelUtils;
import net.minecraft.world.level.gamerules.GameRules;
import com.robertx22.forge_compat.event.RegisterCommandsEvent;
import com.robertx22.forge_compat.event.entity.player.PlayerEvent;
import com.robertx22.forge_compat.event.server.ServerStartedEvent;
import com.robertx22.forge_compat.event.server.ServerStoppingEvent;

public class LifeCycleEvents {

    static boolean regenDefault = true;

    public static void register() {


        ForgeEvents.registerForgeEvent(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (MMORPG.RUN_DEV_TOOLS) {
                DataGeneration.generateAll();
            }
        });

        ForgeEvents.registerForgeEvent(RegisterCommandsEvent.class, event -> {
            CommandRegister.Register(event.getDispatcher());
        });

        ForgeEvents.registerForgeEvent(ServerStartedEvent.class, event -> {

            LevelUtils.runTests();

            if (CompatConfig.get().disableVanillaHealthRegen()) {
                regenDefault = event.getServer().getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);

                event.getServer().getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION, false, event.getServer());
            }

            ExileDB.checkAllDatabasesHaveDefaultEmpty();

        });

        ForgeEvents.registerForgeEvent(ServerStoppingEvent.class, event -> {
            if (CompatConfig.get().disableVanillaHealthRegen()) {
                event.getServer().getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION, regenDefault, event.getServer());

            }
        });

    }
}
