package com.robertx22.age_of_exile.vanilla_mc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.robertx22.age_of_exile.uncommon.testing.CommandTests;
import com.robertx22.age_of_exile.vanilla_mc.commands.suggestions.CommandsSuggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class RunTestCommand {
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
            literal(CommandRefs.ID)
                .then(literal("runtest").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                    .then(argument("test", StringArgumentType.string()).suggests(new CommandsSuggestions())
                        .executes(
                            ctx -> run(ctx.getSource()
                                .getPlayerOrException(), StringArgumentType.getString(ctx, "test"))))));
    }

    private static int run(ServerPlayer player, String test) {

        CommandTests.run(test, player);

        player.sendSystemMessage(Component.literal("Тест завершён."));

        return 1;
    }
}
