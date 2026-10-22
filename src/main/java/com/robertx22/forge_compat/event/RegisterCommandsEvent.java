package com.robertx22.forge_compat.event;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class RegisterCommandsEvent extends Event {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final CommandBuildContext context;
    private final Commands.CommandSelection environment;

    public RegisterCommandsEvent(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection environment) {
        this.dispatcher = dispatcher;
        this.context = context;
        this.environment = environment;
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() { return dispatcher; }
    public CommandBuildContext getBuildContext() { return context; }
    public Commands.CommandSelection getCommandSelection() { return environment; }
}
