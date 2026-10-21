package com.robertx22.age_of_exile.gamemaster;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.robertx22.age_of_exile.vanilla_mc.commands.CommandRefs;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /mine_and_slash gm ... — управление системой GameMaster из игры.
 */
public class GameMasterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> gm = literal("gm")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .executes(ctx -> status(ctx.getSource()))
            .then(literal("status").executes(ctx -> status(ctx.getSource())))
            .then(literal("catchup")
                .executes(ctx -> toggle(ctx.getSource(), "catch_up"))
                .then(argument("value", BoolArgumentType.bool())
                    .executes(ctx -> setBool(ctx.getSource(), "catch_up", BoolArgumentType.getBool(ctx, "value")))))
            .then(literal("pacing")
                .executes(ctx -> toggle(ctx.getSource(), "pacing"))
                .then(argument("value", BoolArgumentType.bool())
                    .executes(ctx -> setBool(ctx.getSource(), "pacing", BoolArgumentType.getBool(ctx, "value")))))
            .then(literal("web")
                .executes(ctx -> {
                    GameMasterConfig config = GameMaster.get()
                        .config();
                    config.web_enabled = !config.web_enabled;
                    GameMaster.get()
                        .onConfigEdited();
                    send(ctx.getSource(), "Веб-панель: " + onOff(config.web_enabled)
                        + (config.web_enabled ? " — http://" + config.web_host + ":" + config.web_port + "/" : ""));
                    return 1;
                })
                .then(argument("value", BoolArgumentType.bool())
                    .executes(ctx -> {
                        GameMasterConfig config = GameMaster.get()
                            .config();
                        config.web_enabled = BoolArgumentType.getBool(ctx, "value");
                        GameMaster.get()
                            .onConfigEdited();
                        send(ctx.getSource(), "Веб-панель: " + onOff(config.web_enabled));
                        return 1;
                    })))
            .then(literal("enabled")
                .executes(ctx -> toggle(ctx.getSource(), "enabled"))
                .then(argument("value", BoolArgumentType.bool())
                    .executes(ctx -> setBool(ctx.getSource(), "enabled", BoolArgumentType.getBool(ctx, "value")))))
            .then(literal("hours")
                .then(argument("hours", DoubleArgumentType.doubleArg(1, 100000))
                    .executes(ctx -> {
                        GameMasterConfig config = GameMaster.get()
                            .config();
                        config.target_hours = DoubleArgumentType.getDouble(ctx, "hours");
                        GameMaster.get()
                            .onConfigEdited();
                        send(ctx.getSource(), "Цель: " + GameMaster.fmt(config.target_hours)
                            + " реальных часов до макс. уровня. Прогноз: "
                            + GameMaster.fmt(GameMaster.get()
                            .predictedHours()) + " ч.");
                        return 1;
                    })))
            .then(literal("startlevel")
                .then(argument("level", IntegerArgumentType.integer(1, 1000))
                    .executes(ctx -> {
                        GameMasterConfig config = GameMaster.get()
                            .config();
                        config.pacing_start_level = IntegerArgumentType.getInteger(ctx, "level");
                        GameMaster.get()
                            .onConfigEdited();
                        send(ctx.getSource(), "Штраф темпа начинается с уровня " + config.pacing_start_level + ".");
                        return 1;
                    })))
            .then(literal("penalty")
                .then(argument("value", DoubleArgumentType.doubleArg(0, 0.99))
                    .executes(ctx -> {
                        GameMasterConfig config = GameMaster.get()
                            .config();
                        config.pacing_max_penalty = DoubleArgumentType.getDouble(ctx, "value");
                        GameMaster.get()
                            .onConfigEdited();
                        send(ctx.getSource(), "Максимальный штраф: "
                            + GameMaster.fmt(config.pacing_max_penalty * 100) + "%.");
                        return 1;
                    })))
            .then(literal("player")
                .then(argument("target", EntityArgument.player())
                    .executes(ctx -> playerInfo(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
            .then(literal("list").executes(ctx -> listPlayers(ctx.getSource())))
            .then(literal("reload").executes(ctx -> {
                GameMaster.get()
                    .reloadConfig();
                send(ctx.getSource(), "Конфиг GameMaster перечитан.");
                return 1;
            }))
            .then(literal("save").executes(ctx -> {
                GameMaster.get()
                    .save();
                send(ctx.getSource(), "Данные GameMaster сохранены в " + GameMaster.dataPath() + ".");
                return 1;
            }))
            .then(literal("resetdata")
                .executes(ctx -> {
                    ctx.getSource()
                        .sendSystemMessage(Component.literal("Это удалит всю статистику игроков. Повторите: /"
                                + CommandRefs.ID + " gm resetdata confirm")
                            .withStyle(ChatFormatting.RED));
                    return 1;
                })
                .then(literal("confirm").executes(ctx -> {
                    GameMaster.get()
                        .resetData();
                    send(ctx.getSource(), "Статистика GameMaster очищена.");
                    return 1;
                })));

        dispatcher.register(literal(CommandRefs.ID).then(gm));
    }

    private static int status(CommandSourceStack source) {
        GameMaster gm = GameMaster.get();
        GameMasterConfig config = gm.config();

        source.sendSystemMessage(Component.literal("GameMaster — " + (gm.isLoaded() ? "активен" : "не запущен"))
            .withStyle(ChatFormatting.GOLD));
        send(source, "Мастер-выключатель: " + onOff(config.enabled)
            + ", уравнивание: " + onOff(config.catch_up)
            + ", темп: " + onOff(config.pacing));
        send(source, "Цель: " + GameMaster.fmt(config.target_hours) + " ч, старт штрафа: "
            + config.pacing_start_level + " уровень, макс. штраф: "
            + GameMaster.fmt(config.pacing_max_penalty * 100) + "%");
        send(source, "Базовое время до максимума: " + GameMaster.fmt(gm.baselineHours()) + " ч"
            + (gm.measuredBaselineHours() > 0
            ? " (замер: " + GameMaster.fmt(gm.measuredBaselineHours()) + " ч)"
            : " (оценка, замеров пока мало)"));
        send(source, "Прогноз с настройкой: " + GameMaster.fmt(gm.predictedHours()) + " ч, k="
            + GameMaster.fmt(gm.calibrationK()));
        send(source, "Ориентир группы: " + GameMaster.fmt(gm.referenceLevel()) + " уровень (игроков в расчёте: "
            + gm.peerCount() + ")");
        if (config.web_enabled) {
            send(source, "Панель: http://" + config.web_host + ":" + config.web_port + "/");
        } else {
            send(source, "Веб-панель выключена (web_enabled=false).");
        }
        return 1;
    }

    private static int toggle(CommandSourceStack source, String key) {
        GameMaster gm = GameMaster.get();
        GameMasterConfig config = gm.config();
        boolean value;
        String title;
        switch (key) {
            case "catch_up" -> {
                config.catch_up = !config.catch_up;
                value = config.catch_up;
                title = "Уравнивание игроков";
            }
            case "pacing" -> {
                config.pacing = !config.pacing;
                value = config.pacing;
                title = "Темп развития";
            }
            default -> {
                config.enabled = !config.enabled;
                value = config.enabled;
                title = "GameMaster";
            }
        }
        gm.onConfigEdited();
        send(source, title + ": " + onOff(value));
        return 1;
    }

    private static int setBool(CommandSourceStack source, String key, boolean value) {
        GameMasterConfig config = GameMaster.get()
            .config();
        switch (key) {
            case "catch_up" -> config.catch_up = value;
            case "pacing" -> config.pacing = value;
            default -> config.enabled = value;
        }
        GameMaster.get()
            .onConfigEdited();
        send(source, key + " = " + onOff(value));
        return 1;
    }

    private static int playerInfo(CommandSourceStack source, ServerPlayer target) {
        GameMaster gm = GameMaster.get();
        PlayerProgress progress = gm.data()
            .get(target.getUUID()
                .toString());
        if (progress == null) {
            send(source, "По игроку " + target.getGameProfile()
                .getName() + " ещё нет данных.");
            return 1;
        }
        GameMaster.MultiplierBreakdown breakdown = gm.breakdown(progress);
        send(source, target.getGameProfile()
            .getName() + ": уровень " + progress.level + ", время " + GameMaster.fmt(progress.playtimeHours()) + " ч");
        send(source, "Множитель: x" + GameMaster.fmt(breakdown.total)
            + " (темп x" + GameMaster.fmt(breakdown.pacing)
            + ", догоняющий x" + GameMaster.fmt(breakdown.catchUp) + ")");
        send(source, "Опыт: " + Math.round(progress.exp_gained) + " (без GameMaster было бы "
            + Math.round(progress.raw_exp_gained) + ")");
        return 1;
    }

    private static int listPlayers(CommandSourceStack source) {
        GameMaster gm = GameMaster.get();
        List<PlayerProgress> players = new java.util.ArrayList<>(gm.data().players.values());
        if (players.isEmpty()) {
            send(source, "Статистики пока нет.");
            return 1;
        }
        players.sort((a, b) -> Integer.compare(b.level, a.level));
        send(source, "Игроки (" + players.size() + "):");
        for (PlayerProgress progress : players) {
            GameMaster.MultiplierBreakdown breakdown = gm.breakdown(progress);
            send(source, progress.name + " — " + progress.level + " ур., "
                + GameMaster.fmt(progress.playtimeHours()) + " ч, x" + GameMaster.fmt(breakdown.total)
                + (progress.online ? " (в сети)" : ""));
        }
        return 1;
    }

    private static String onOff(boolean value) {
        return value ? "вкл" : "выкл";
    }

    private static void send(CommandSourceStack source, String text) {
        source.sendSystemMessage(Component.literal(text));
    }
}
