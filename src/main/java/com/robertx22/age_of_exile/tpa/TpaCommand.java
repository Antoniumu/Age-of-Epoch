package com.robertx22.age_of_exile.tpa;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Registers player-facing TPA commands. All commands are available without OP. */
public final class TpaCommand {

    private TpaCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("tpa")
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .executes(context -> result(TpaManager.showHelp(context.getSource().getPlayerOrException())))
            .then(literal("help")
                .executes(context -> result(TpaManager.showHelp(context.getSource().getPlayerOrException()))))
            .then(literal("charges")
                .executes(context -> result(TpaManager.showCharges(context.getSource().getPlayerOrException()))))
            .then(argument("target", EntityArgument.player())
                .executes(context -> result(TpaManager.sendRequest(
                    context.getSource().getPlayerOrException(),
                    EntityArgument.getPlayer(context, "target")
                ))))
            .then(argument("coordinates", Vec3Argument.vec3())
                .executes(context -> result(TpaManager.teleportToCoordinates(
                    context.getSource().getPlayerOrException(),
                    Vec3Argument.getVec3(context, "coordinates")
                ))))
            .then(literal("trust")
                .then(argument("trusted", EntityArgument.player())
                    .executes(context -> result(TpaManager.trust(
                        context.getSource().getPlayerOrException(),
                        EntityArgument.getPlayer(context, "trusted")
                    )))))
            .then(literal("untrust")
                .then(argument("trusted", EntityArgument.player())
                    .executes(context -> result(TpaManager.untrust(
                        context.getSource().getPlayerOrException(),
                        EntityArgument.getPlayer(context, "trusted")
                    )))))
            .then(literal("trusted")
                .executes(context -> result(TpaManager.showTrusted(context.getSource().getPlayerOrException()))))
            .then(literal("cancel")
                .executes(context -> result(TpaManager.cancel(context.getSource().getPlayerOrException()))))
            .then(literal("accept")
                .executes(context -> result(TpaManager.accept(context.getSource().getPlayerOrException(), null)))
                .then(argument("sender", EntityArgument.player())
                    .executes(context -> result(TpaManager.accept(
                        context.getSource().getPlayerOrException(),
                        EntityArgument.getPlayer(context, "sender")
                    )))))
            .then(literal("deny")
                .executes(context -> result(TpaManager.deny(context.getSource().getPlayerOrException(), null)))
                .then(argument("sender", EntityArgument.player())
                    .executes(context -> result(TpaManager.deny(
                        context.getSource().getPlayerOrException(),
                        EntityArgument.getPlayer(context, "sender")
                    ))))));

        dispatcher.register(literal("tpaccept")
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .executes(context -> result(TpaManager.accept(context.getSource().getPlayerOrException(), null)))
            .then(argument("sender", EntityArgument.player())
                .executes(context -> result(TpaManager.accept(
                    context.getSource().getPlayerOrException(),
                    EntityArgument.getPlayer(context, "sender")
                )))));

        dispatcher.register(literal("tpdeny")
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .executes(context -> result(TpaManager.deny(context.getSource().getPlayerOrException(), null)))
            .then(argument("sender", EntityArgument.player())
                .executes(context -> result(TpaManager.deny(
                    context.getSource().getPlayerOrException(),
                    EntityArgument.getPlayer(context, "sender")
                )))));

        dispatcher.register(literal("tpacancel")
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .executes(context -> result(TpaManager.cancel(context.getSource().getPlayerOrException()))));
    }

    private static int result(boolean success) {
        return success ? 1 : 0;
    }
}
