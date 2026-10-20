package com.robertx22.infinite_dungeons.main;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.infinite_dungeons.events.OnMobKill;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.main.ForgeEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import com.robertx22.forge_compat.fml.event.server.FMLServerStartedEvent;

public class DungeonPortalsEvents {

    public static void reg() {

        ExileEvents.CHECK_IF_DEV_TOOLS_SHOULD_RUN.register(new EventConsumer<ExileEvents.OnCheckIsDevToolsRunning>() {
            @Override
            public void accept(ExileEvents.OnCheckIsDevToolsRunning event) {
                if (MainID.RUN_DEV_TOOLS()) {
                    event.run = true;
                }
            }
        });

        ForgeEvents.registerForgeEvent(FMLServerStartedEvent.class, event -> {
            MinecraftServer server = event.getServer();
            CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands()
                .getDispatcher();

        });

        ExileEvents.MOB_DEATH.register(new OnMobKill());

    }

}