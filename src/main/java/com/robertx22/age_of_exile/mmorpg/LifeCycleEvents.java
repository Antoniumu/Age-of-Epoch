package com.robertx22.age_of_exile.mmorpg;

import com.robertx22.age_of_exile.gamemaster.GameMasterEvents;
import com.robertx22.age_of_exile.guides.GuideLimit;
import com.robertx22.age_of_exile.mmorpg.registers.server.CommandRegister;
import com.robertx22.age_of_exile.uncommon.testing.TestManager;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.library_of_exile.main.ForgeEvents;
import net.minecraft.world.level.gamerules.GameRules;
import com.robertx22.forge_compat.fml.event.server.FMLServerStartedEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStartingEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStoppedEvent;

public class LifeCycleEvents {

    static boolean regenDefault = true;

    public static void register() {

        GameMasterEvents.register();

        ForgeEvents.registerForgeEvent(FMLServerStartingEvent.class, event -> {
            if (MMORPG.RUN_DEV_TOOLS) {
                DataGeneration.generateAll();
            }
            MMORPG.server = event.getServer();
        });

        ForgeEvents.registerForgeEvent(FMLServerStartedEvent.class, event -> {

            // Гайды ведут игрока только до определённого уровня (по умолчанию 20).
            // Файл читаем после загрузки датапаков: он перекрывает game_balance.
            GuideLimit.loadFromDisk();

            LevelUtils.runTests();

            CommandRegister.Register(event.getServer());

            regenDefault = event.getServer()
                .getGameRules()
                .get(GameRules.NATURAL_HEALTH_REGENERATION);

            event.getServer()
                .getGameRules()
                .set(GameRules.NATURAL_HEALTH_REGENERATION, false, event.getServer());

            if (MMORPG.RUN_DEV_TOOLS) { // CHANGE ON PUBLIC BUILDS TO FALSE
                TestManager.RunAllTests(event.getServer()
                    .overworld());
            }
        });

        ForgeEvents.registerForgeEvent(FMLServerStoppedEvent.class, event -> {
            event.getServer()
                .getGameRules()
                .set(GameRules.NATURAL_HEALTH_REGENERATION, regenDefault, event.getServer());
        });

    }
}
