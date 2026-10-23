package com.robertx22.library_of_exile.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class CommandUtils {

    public static void execute(Player player, String command) {

        CommandSourceStack source = getCommandSource(player);

        player.level()
            .getServer()
            .getCommands()
            .performPrefixedCommand(source, command);
    }

    public static CommandSourceStack getCommandSource(Entity entity) {
        return entity.level().getServer()
            .createCommandSourceStack()
            .withEntity(entity)
            .withPosition(entity.position())
            .withRotation(entity.getRotationVector())
            .withLevel(entity.level() instanceof ServerLevel sl ? sl : null)
            .withSuppressedOutput();
    }
}
