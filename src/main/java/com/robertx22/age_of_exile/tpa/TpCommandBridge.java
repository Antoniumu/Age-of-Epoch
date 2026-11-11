package com.robertx22.age_of_exile.tpa;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.robertx22.age_of_exile.mixin_ducks.CommandNodeAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

/**
 * Makes vanilla {@code /tp} use the TPA rules for players without operator-level
 * command access. Operators and command sources keep vanilla teleport behavior.
 */
public final class TpCommandBridge {

    /** Dispatcher used to forward arguments to the registered TPA/vanilla commands. */
    private static CommandDispatcher<CommandSourceStack> dispatcher;

    private TpCommandBridge() {
    }

    /**
     * Vanilla {@code /tp} is an alias node redirecting to {@code /teleport}. Merely
     * changing the alias command does not work: Brigadier follows that redirect before
     * it ever visits the alias's children. Detach the alias and install a greedy argument
     * handler so normal players can use the same parsing and charge checks as {@code /tpa}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void install(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        if (commandDispatcher == null || commandDispatcher == dispatcher) {
            return;
        }

        CommandNode<CommandSourceStack> tp = commandDispatcher.getRoot().getChild("tp");
        if (!(tp instanceof CommandNodeAccessor accessor)) {
            // No vanilla alias or the mixin did not apply: leave the server usable.
            return;
        }

        dispatcher = commandDispatcher;

        // Remove vanilla's redirect to /teleport. Its destination still has the normal
        // operator permission check, so leaving the redirect in place bypasses our handler.
        accessor.tpa$setRedirect(null);
        accessor.tpa$setRequirement((Predicate<CommandSourceStack>) TpCommandBridge::canUse);
        accessor.tpa$setCommand((Command<CommandSourceStack>) TpCommandBridge::help);

        // Preserve the vanilla /tp name but parse its full tail in one place. Dispatching
        // the text to /tpa gives coordinates, selectors, messages, and charge consumption
        // the exact same implementation as the dedicated TPA command.
        tp.addChild(Commands.argument("arguments", StringArgumentType.greedyString())
            .executes(TpCommandBridge::run)
            .build());
    }

    private static boolean canUse(CommandSourceStack source) {
        return source != null
            && (hasVanillaPermission(source) || playerWithoutVanillaPermission(source) != null);
    }

    private static int help(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = playerWithoutVanillaPermission(context.getSource());
        if (player != null) {
            return TpaManager.showHelp(player) ? 1 : 0;
        }
        // For operators, /tp without arguments keeps vanilla's normal syntax error.
        return executeVanilla(context.getSource(), "");
    }

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String arguments = StringArgumentType.getString(context, "arguments").trim();
        ServerPlayer player = playerWithoutVanillaPermission(context.getSource());
        if (player == null) {
            return executeVanilla(context.getSource(), arguments);
        }
        return runAsTpa(context, player, arguments);
    }

    private static int executeVanilla(CommandSourceStack source, String arguments) throws CommandSyntaxException {
        String command = arguments.isEmpty() ? "teleport" : "teleport " + arguments;
        return dispatcher.execute(command, source);
    }

    /** Forwards unprivileged player arguments through the actual TPA command. */
    private static int runAsTpa(CommandContext<CommandSourceStack> context, ServerPlayer player,
                                String arguments) throws CommandSyntaxException {
        if (dispatcher == null) {
            return 0;
        }

        try {
            // Treat an explicit self selector as if it were omitted: `/tp @s` is `/tpa`,
            // and `/tp @s <x> <y> <z>` is `/tpa <x> <y> <z>` (including the same charge).
            if (arguments.equals("@s")) {
                arguments = "";
            } else if (arguments.startsWith("@s ") || arguments.startsWith("@s\t")) {
                arguments = arguments.substring(2).trim();
            }

            // Support the vanilla-shaped target-to-coordinate form as well. The
            // teleported player executes TPA, and therefore pays their own charge.
            int targeted = teleportTargetToCoordinates(context, player, arguments);
            if (targeted != -1) {
                return targeted;
            }

            String command = arguments.isEmpty() ? "tpa" : "tpa " + arguments;
            return dispatcher.execute(command, context.getSource());
        } catch (CommandSyntaxException e) {
            // Unsupported vanilla forms must not fall through to unrestricted /teleport.
            TpaManager.send(player, "Без прав оператора /tp работает как /tpa:", ChatFormatting.RED);
            TpaManager.send(player, "/tp <игрок> — запрос телепортации; /tp <x> <y> <z> — телепорт по заряду.",
                ChatFormatting.GRAY);
            return 0;
        }
    }

    /**
     * Handles {@code /tp <player> <x> <y> <z>}. Returns -1 when this is not that
     * syntax so that {@code /tp} can simply forward the arguments to {@code /tpa}.
     */
    private static int teleportTargetToCoordinates(CommandContext<CommandSourceStack> context,
                                                    ServerPlayer sender, String arguments)
            throws CommandSyntaxException {
        String[] parts = arguments.isBlank() ? new String[0] : arguments.split("\\s+");
        if (parts.length != 4 || looksLikeCoordinate(parts[0])) {
            return -1;
        }
        for (int i = 1; i < parts.length; i++) {
            if (!looksLikeCoordinate(parts[i])) {
                return -1;
            }
        }

        ServerPlayer target = resolveTarget(context, sender, parts[0]);
        if (target == null) {
            return 0;
        }

        String coordinates = parts[1] + " " + parts[2] + " " + parts[3];
        // Relative coordinates are resolved from the teleported player, and its TPA data
        // performs all charge, death, and world-border checks before spending a charge.
        int result = dispatcher.execute("tpa " + coordinates,
            context.getSource().withEntity(target));

        if (result > 0 && target != sender) {
            TpaManager.send(sender, "Игроку " + target.getGameProfile().name() + " телепортировано на "
                + coordinates + ". Осталось зарядов: " + TpaManager.chargesLeft(target)
                + "/" + TpaData.MAX_CHARGES + ".", ChatFormatting.GOLD);
        }
        return result;
    }

    private static boolean looksLikeCoordinate(String token) {
        if (token.isEmpty()) {
            return false;
        }
        char first = token.charAt(0);
        if (first == '~' || first == '^') {
            return true;
        }
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static ServerPlayer resolveTarget(CommandContext<CommandSourceStack> context,
                                               ServerPlayer sender, String token) {
        if ("@s".equals(token) || "@p".equals(token)) {
            return sender;
        }
        if (token.startsWith("@")) {
            TpaManager.send(sender, "Укажите одного игрока: @s, @p или ник игрока.", ChatFormatting.RED);
            return null;
        }
        MinecraftServer server = context.getSource().getServer();
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (candidate.getGameProfile().name().equalsIgnoreCase(token)) {
                return candidate;
            }
        }
        TpaManager.send(sender, "Игрока \"" + token + "\" нет на сервере.", ChatFormatting.RED);
        return null;
    }

    private static boolean hasVanillaPermission(CommandSourceStack source) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
    }

    /** Returns only players who must use TPA; operators and non-player sources get null. */
    private static ServerPlayer playerWithoutVanillaPermission(CommandSourceStack source) {
        if (source == null || hasVanillaPermission(source)) {
            return null;
        }
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException ignored) {
            return null;
        }
    }
}
