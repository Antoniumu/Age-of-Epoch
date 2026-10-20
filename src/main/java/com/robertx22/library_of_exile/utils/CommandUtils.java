package com.robertx22.library_of_exile.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.commands.CommandSourceStack;
public class CommandUtils {

    public static void execute(Player player, String command) {

        CommandSourceStack source = getCommandSource(player);

        player
            .getServer()
            .getCommands()
            .performCommand(source, command);
    }

    public static CommandSourceStack getCommandSource(Entity entity) {
        return new CommandSourceStack(
            // this doesnt send messages to spam server
            CommandSourceStack.NULL,
            entity.position(),
            entity.getRotationVector(),
            entity.level instanceof ServerLevel ? (ServerLevel) entity.level : null,
            4,
            entity.getName()
                .getString(),
            entity.getDisplayName(),
            entity.level.getServer(),
            entity);
    }
}
