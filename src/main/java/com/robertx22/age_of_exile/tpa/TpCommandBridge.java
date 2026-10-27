package com.robertx22.age_of_exile.tpa;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.robertx22.age_of_exile.mixin_ducks.CommandNodeAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

/**
 * Заставляет ванильный {@code /tp} работать как {@code /tpa} у игроков без прав оператора.
 *
 * <p>Ванильный {@code /tp} требует уровень прав 2, поэтому обычный игрок его просто не видит.
 * Здесь узел {@code tp} в Brigadier открывается всем, а его обработчики оборачиваются:
 * у игрока без прав запрос уходит в {@code /tpa} вместе с аргументами, поэтому действуют
 * <b>все</b> лимиты аддона — заряды и их восстановление, таймаут запроса, автопринятие для
 * доверенных, проверка мировой границы и запрет телепорта мёртвым. Операторы (уровень 2+),
 * консоль и командные блоки получают ванильное поведение без изменений.</p>
 */
public final class TpCommandBridge {

    /** Диспетчер сервера: через него аргументы {@code /tp} передаются в {@code /tpa}. */
    private static CommandDispatcher<CommandSourceStack> dispatcher;

    private TpCommandBridge() {
    }

    /**
     * Вызывается один раз после регистрации команд сервера.
     * Повторный вызов с тем же диспетчером ничего не делает.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void install(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        if (commandDispatcher == null || commandDispatcher == dispatcher) {
            return;
        }

        CommandNode<CommandSourceStack> tp = commandDispatcher.getRoot()
            .getChild("tp");
        if (!(tp instanceof CommandNodeAccessor accessor)) {
            // Ванильного /tp нет (другая версия или другой мод) либо миксин не применился —
            // тихо оставляем команду как есть, сервер продолжает работать.
            return;
        }

        dispatcher = commandDispatcher;

        // Brigadier хранит требование и обработчик в приватных полях, поэтому сеттеры
        // из миксина принимают сырые типы — приводим их здесь явно.
        Predicate<CommandSourceStack> requirement = TpCommandBridge::canUse;
        accessor.tpa$setRequirement((Predicate) requirement);

        Command<CommandSourceStack> help = TpCommandBridge::help;
        accessor.tpa$setCommand((Command) help);

        wrapChildren(tp);
    }

    /** "/tp" без аргументов у обычного игрока показывает справку TPA, у операторов — ванильную ошибку. */
    private static int help(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = tpaPlayer(context.getSource());
        if (player == null) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand()
                .create();
        }
        return TpaManager.showHelp(player) ? 1 : 0;
    }

    /** Оборачивает все исполняемые узлы поддерева /tp, не трогая структуру аргументов. */
    private static void wrapChildren(CommandNode<CommandSourceStack> node) {
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            wrapChildren(child);

            Command<CommandSourceStack> vanilla = child.getCommand();
            if (vanilla == null || vanilla instanceof TpaFallback) {
                continue;
            }
            ((CommandNodeAccessor) child).tpa$setCommand(new TpaFallback(vanilla));
        }
    }

    /**
     * Отдаёт ванильные аргументы в /tpa: "/tp Steve" -> "/tpa Steve",
     * "/tp 100 64 -250" -> "/tpa 100 64 -250". Так разбор координат, селекторов
     * и сообщения об ошибках полностью совпадают с самим /tpa.
     */
    private static int runAsTpa(CommandContext<CommandSourceStack> context, ServerPlayer player)
            throws CommandSyntaxException {
        if (dispatcher == null) {
            return 0;
        }

        String arguments = argumentsOf(context.getInput());

        try {
            // "/tp @s 100 64 -250" или "/tp Steve 100 64 -250" — телепортация игрока
            // (или себя, @s) на метку, т. е. координаты, за один заряд TPA.
            // Без этой формы селектор цели сломал бы разбор аргументов в самом /tpa.
            int targeted = teleportTargetToCoordinates(context, player, arguments);
            if (targeted != -1) {
                return targeted;
            }

            String command = arguments.isEmpty() ? "tpa" : "tpa " + arguments;
            return dispatcher.execute(command, context.getSource());
        } catch (CommandSyntaxException e) {
            // Формы ванильного /tp, которых у /tpa нет: /tp <кого> <куда>, /tp <куда> <поворот> и т.п.
            TpaManager.send(player, "Без прав оператора /tp работает как /tpa:", ChatFormatting.RED);
            TpaManager.send(player, "/tp <игрок> — запрос телепортации (игрок должен быть в сети); "
                + "/tp <x> <y> <z> — телепорт по заряду.", ChatFormatting.GRAY);
            return 0;
        }
    }

    /** Всё после имени команды: "tp Steve" -> "Steve". */
    private static String argumentsOf(String input) {
        if (input == null) {
            return "";
        }
        int space = input.indexOf(' ');
        return space < 0 ? "" : input.substring(space + 1)
            .trim();
    }

    /**
     * "/tp <цель> <x> <y> <z>" — телепортировать игрока на метку (координаты) за
     * один заряд TPA. Возвращает -1, если аргументы не похожи на эту форму, чтобы
     * вызывающий продолжил обычную обработку через /tpa ("/tp 100 64 -250" на
     * себя или "/tp Steve" как запрос).
     *
     * <p>Заряд тратит телепортируемый игрок — точно так же, как и при
     * {@code /tpa <x> <y> <z>}. Операторы не попадают сюда вообще: их /tp
     * остаётся ванильным.</p>
     */
    private static int teleportTargetToCoordinates(CommandContext<CommandSourceStack> context,
            ServerPlayer player, String arguments) throws CommandSyntaxException {
        String[] parts = arguments.trim().split("\\s+");
        if (parts.length != 4) {
            return -1;
        }
        String targetToken = parts[0];
        if (looksLikeCoordinate(targetToken)) {
            return -1; // "/tp 100 64 -250" — цели нет, телепортируем себя через /tpa
        }
        for (int i = 1; i <= 3; i++) {
            if (!looksLikeCoordinate(parts[i])) {
                return -1;
            }
        }

        ServerPlayer target = resolveTarget(context, player, targetToken);
        if (target == null) {
            return 0; // сообщение об ошибке уже отправлено игроку
        }

        String coords = parts[1] + " " + parts[2] + " " + parts[3];
        // Выполняем /tpa <x> <y> <z> уже от имени цели: заряд списывается с цели,
        // сообщения идут цели, относительные координаты считаются от цели.
        CommandSourceStack targetSource = context.getSource().withEntity(target);
        int result = dispatcher.execute("tpa " + coords, targetSource);

        if (result > 0 && target != player) {
            send(player, "Игроку " + target.getGameProfile().name() + " телепортировано на "
                    + coords + ". Осталось зарядов: " + TpaManager.chargesLeft(target)
                    + "/" + TpaData.MAX_CHARGES + ".", ChatFormatting.GOLD);
        }
        return result;
    }

    /** Похоже ли на координату: число, "~" или "^" с относительным смещением. */
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
        } catch (NumberFormatException unused) {
            return false;
        }
    }

    /** @s/@p — отправитель команды; иначе ищем онлайн-игрока по нику. */
    private static ServerPlayer resolveTarget(CommandContext<CommandSourceStack> context,
            ServerPlayer player, String token) {
        if ("@s".equals(token) || "@p".equals(token)) {
            return player;
        }
        if (token.startsWith("@")) {
            send(player, "Укажите одного игрока: @s, @p или ник игрока.", ChatFormatting.RED);
            return null;
        }
        MinecraftServer server = context.getSource().getServer();
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (candidate.getGameProfile().name().equalsIgnoreCase(token)) {
                return candidate;
            }
        }
        send(player, "Игрока \"" + token + "\" нет на сервере.", ChatFormatting.RED);
        return null;
    }

    private static boolean canUse(CommandSourceStack source) {
        return source != null && (hasVanillaPermission(source) || tpaPlayer(source) != null);
    }

    /** Ровно те права, которых ванильный /tp и требует. */
    private static boolean hasVanillaPermission(CommandSourceStack source) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)
            .test(source);
    }

    /**
     * Игрок без прав оператора — его /tp должен вести в /tpa.
     * Для операторов, консоли и командных блоков возвращает null: они остаются на ванили.
     */
    private static ServerPlayer tpaPlayer(CommandSourceStack source) {
        if (source == null || hasVanillaPermission(source)) {
            return null;
        }
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    /** Ванильный обработчик узла, который для обычного игрока заменяется на /tpa. */
    private static final class TpaFallback implements Command<CommandSourceStack> {

        private final Command<CommandSourceStack> vanilla;

        private TpaFallback(Command<CommandSourceStack> vanilla) {
            this.vanilla = vanilla;
        }

        @Override
        public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            ServerPlayer player = tpaPlayer(context.getSource());
            if (player == null) {
                return vanilla.run(context);
            }
            return runAsTpa(context, player);
        }
    }
}
