package com.robertx22.divine_missions.main;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.divine_missions.commands.*;
import com.robertx22.divine_missions.components.PlayerMissionCap;
import com.robertx22.divine_missions.events.OnChestLooted;
import com.robertx22.divine_missions.events.OnMobKill;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.main.ForgeEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStartedEvent;
import com.robertx22.forge_compat.fml.server.ServerLifecycleHooks;

public class DivineEvents {

    static int ticks = 0;

    public static void reg() {

        ForgeEvents.registerForgeEvent(TickEvent.ServerTickEvent.class, event -> {
            if (event.phase == TickEvent.Phase.END) {
                ticks++;
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (ticks % 20 == 0) {
                    server.getPlayerList()
                        .getPlayers()
                        .forEach(p -> {
                            PlayerMissionCap.get(p).missionData.onTick(20, p);
                        });
                }
            }
        });

        ExileEvents.CHECK_IF_DEV_TOOLS_SHOULD_RUN.register(new EventConsumer<ExileEvents.OnCheckIsDevToolsRunning>() {
            @Override
            public void accept(ExileEvents.OnCheckIsDevToolsRunning event) {
                if (DivineMissions.RUN_DEV_TOOLS()) {
                    event.run = true;
                }
            }
        });

        ForgeEvents.registerForgeEvent(FMLServerStartedEvent.class, event -> {
            MinecraftServer server = event.getServer();
            CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands()
                .getDispatcher();
            GiveReputation.register(dispatcher);
            ResetReputations.register(dispatcher);
            GiveMission.register(dispatcher);
            CompleteAllMissions.register(dispatcher);
            RefreshMissions.register(dispatcher);
        });

        ExileEvents.MOB_DEATH.register(new OnMobKill());
        ExileEvents.ON_CHEST_LOOTED.register(new OnChestLooted());

    }

}