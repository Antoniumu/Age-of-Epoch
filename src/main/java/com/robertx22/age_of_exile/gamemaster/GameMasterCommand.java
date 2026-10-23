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
            .then(spawnRateCommand())
            .then(autoCommand())
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

    private static LiteralArgumentBuilder<CommandSourceStack> spawnRateCommand() {
        return literal("spawnrate")
            .executes(ctx -> spawnRateStatus(ctx.getSource()))
            .then(literal("status").executes(ctx -> spawnRateStatus(ctx.getSource())))
            .then(literal("enabled")
                .executes(ctx -> toggleSpawnRate(ctx.getSource(), null))
                .then(argument("value", BoolArgumentType.bool())
                    .executes(ctx -> toggleSpawnRate(ctx.getSource(), BoolArgumentType.getBool(ctx, "value")))))
            .then(literal("dynamic")
                .executes(ctx -> toggleSpawnRateDynamic(ctx.getSource(), null))
                .then(argument("value", BoolArgumentType.bool())
                    .executes(ctx -> toggleSpawnRateDynamic(ctx.getSource(), BoolArgumentType.getBool(ctx, "value")))))
            .then(literal("manual")
                .then(argument("multiplier", DoubleArgumentType.doubleArg(0, 1))
                    .executes(ctx -> setManualSpawnRate(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "multiplier")))))
            .then(literal("player")
                .then(argument("target", EntityArgument.player())
                    .executes(ctx -> spawnRatePlayerInfo(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))
                    .then(argument("multiplier", DoubleArgumentType.doubleArg(0, 1))
                        .executes(ctx -> setPlayerSpawnRate(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"),
                            DoubleArgumentType.getDouble(ctx, "multiplier"))))
                    .then(literal("reset")
                        .executes(ctx -> clearPlayerSpawnRate(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))))));
    }

    private static int spawnRateStatus(CommandSourceStack source) {
        GameMaster gm = GameMaster.get();
        GameMasterConfig config = gm.config();
        send(source, "Спавн мобов: " + onOff(config.enabled && config.spawn_rate_enabled)
            + " (динамика: " + onOff(config.spawn_rate_dynamic) + ").");
        send(source, "Ручной множитель: x" + GameMaster.fmt(config.spawn_rate_manual_multiplier)
            + ", база Благосклонности: " + GameMaster.fmt(config.spawn_rate_favor_reference)
            + ", динамический минимум: x" + GameMaster.fmt(config.spawn_rate_dynamic_min_multiplier) + ".");
        send(source, "Динамика: 0 Благосклонности → x1, " + GameMaster.fmt(config.spawn_rate_favor_reference)
            + " → x" + GameMaster.fmt(gm.spawnRateDynamicMultiplier((int) config.spawn_rate_favor_reference))
            + "; ручной множитель игрока заменяет глобальный, динамика затем умножается.");
        return 1;
    }

    private static int toggleSpawnRate(CommandSourceStack source, Boolean requested) {
        GameMaster gm = GameMaster.get();
        GameMasterConfig config = gm.config();
        config.spawn_rate_enabled = requested == null ? !config.spawn_rate_enabled : requested;
        gm.onConfigEdited();
        send(source, "Спавн мобов: " + onOff(config.spawn_rate_enabled));
        return 1;
    }

    private static int toggleSpawnRateDynamic(CommandSourceStack source, Boolean requested) {
        GameMaster gm = GameMaster.get();
        GameMasterConfig config = gm.config();
        config.spawn_rate_dynamic = requested == null ? !config.spawn_rate_dynamic : requested;
        gm.onConfigEdited();
        send(source, "Динамика спавна по Благосклонности: " + onOff(config.spawn_rate_dynamic));
        return 1;
    }

    private static int setManualSpawnRate(CommandSourceStack source, double multiplier) {
        GameMaster gm = GameMaster.get();
        gm.config().spawn_rate_manual_multiplier = multiplier;
        gm.onConfigEdited();
        send(source, "Глобальный ручной множитель спавна: x" + GameMaster.fmt(multiplier)
            + ". Динамический множитель Благосклонности применяется отдельно.");
        return 1;
    }

    private static int spawnRatePlayerInfo(CommandSourceStack source, ServerPlayer target) {
        GameMaster gm = GameMaster.get();
        String uuid = target.getUUID().toString();
        Double override = gm.spawnRatePlayerOverride(uuid);
        int favor = 100;
        try {
            favor = com.robertx22.age_of_exile.uncommon.datasaving.Load.playerRPGData(target).favor.getFavor();
        } catch (Exception ignored) {
            PlayerProgress progress = gm.data().get(uuid);
            if (progress != null) {
                favor = progress.favor;
            }
        }
        send(source, target.getGameProfile().name() + ": Благосклонность " + favor
            + ", итоговый spawn-rate x" + GameMaster.fmt(gm.spawnRateMultiplier(target)) + ".");
        send(source, "Ручной множитель: " + (override == null ? "глобальный x"
            + GameMaster.fmt(gm.config().spawn_rate_manual_multiplier) : "override x" + GameMaster.fmt(override))
            + ", динамический x" + GameMaster.fmt(gm.spawnRateDynamicMultiplier(favor)) + ".");
        return 1;
    }

    private static int setPlayerSpawnRate(CommandSourceStack source, ServerPlayer target, double multiplier) {
        GameMaster gm = GameMaster.get();
        gm.setSpawnRatePlayerOverride(target.getUUID().toString(), multiplier);
        send(source, "Ручной override спавна для " + target.getGameProfile().name() + ": x"
            + GameMaster.fmt(multiplier) + ". Динамика по Благосклонности продолжает применяться.");
        return 1;
    }

    private static int clearPlayerSpawnRate(CommandSourceStack source, ServerPlayer target) {
        GameMaster gm = GameMaster.get();
        boolean removed = gm.clearSpawnRatePlayerOverride(target.getUUID().toString());
        send(source, removed
            ? "Ручной override спавна для " + target.getGameProfile().name() + " удалён."
            : "У " + target.getGameProfile().name() + " нет ручного override спавна.");
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> autoCommand() {
        return literal("auto")
            .executes(ctx -> autoStatus(ctx.getSource()))
            .then(literal("status").executes(ctx -> autoStatus(ctx.getSource())))
            .then(literal("on").executes(ctx -> setAuto(ctx.getSource(), true)))
            .then(literal("off").executes(ctx -> setAuto(ctx.getSource(), false)))
            .then(literal("mobs")
                .executes(ctx -> toggleAutoKey(ctx.getSource(), "mobs"))
                .then(argument("value", BoolArgumentType.bool())
                    .executes(ctx -> setAutoKey(ctx.getSource(), "mobs", BoolArgumentType.getBool(ctx, "value")))))
            .then(literal("loot")
                .executes(ctx -> toggleAutoKey(ctx.getSource(), "loot"))
                .then(argument("value", BoolArgumentType.bool())
                    .executes(ctx -> setAutoKey(ctx.getSource(), "loot", BoolArgumentType.getBool(ctx, "value")))))
            .then(literal("minutes")
                .then(argument("start", DoubleArgumentType.doubleArg(1, 600))
                    .then(argument("end", DoubleArgumentType.doubleArg(1, 600))
                        .executes(ctx -> {
                            GameMasterConfig config = GameMaster.get()
                                .config();
                            config.auto_start_minutes = DoubleArgumentType.getDouble(ctx, "start");
                            config.auto_end_minutes = DoubleArgumentType.getDouble(ctx, "end");
                            GameMaster.get()
                                .onConfigEdited();
                            send(ctx.getSource(), "Коридор: " + GameMaster.fmt(config.auto_start_minutes) + "–"
                                + GameMaster.fmt(config.auto_end_minutes) + " мин/уровень, суммарно ~"
                                + GameMaster.fmt(GameMaster.get()
                                .autoTargetHours()) + " ч.");
                            return 1;
                        }))))
            .then(argument("value", BoolArgumentType.bool())
                .executes(ctx -> setAuto(ctx.getSource(), BoolArgumentType.getBool(ctx, "value"))));
    }

    private static int setAuto(CommandSourceStack source, boolean value) {
        GameMasterConfig config = GameMaster.get()
            .config();
        config.auto_balance = value;
        GameMaster.get()
            .onConfigEdited();
        send(source, "Автобаланс: " + onOff(value)
            + (value ? " — мастер сам держит темп, сложность и лут в коридоре." : "."));
        return 1;
    }

    private static int toggleAutoKey(CommandSourceStack source, String key) {
        GameMasterConfig config = GameMaster.get()
            .config();
        boolean value = "mobs".equals(key) ? !config.auto_touch_mobs : !config.auto_touch_loot;
        return setAutoKey(source, key, value);
    }

    private static int setAutoKey(CommandSourceStack source, String key, boolean value) {
        GameMasterConfig config = GameMaster.get()
            .config();
        if ("mobs".equals(key)) {
            config.auto_touch_mobs = value;
        } else {
            config.auto_touch_loot = value;
        }
        GameMaster.get()
            .onConfigEdited();
        send(source, ("mobs".equals(key) ? "Автоподстройка мобов (HP/урон)" : "Автоподстройка лута") + ": "
            + onOff(value));
        return 1;
    }

    private static int autoStatus(CommandSourceStack source) {
        GameMaster gm = GameMaster.get();
        GameMasterConfig config = gm.config();
        send(source, "Автобаланс: " + onOff(config.enabled && config.auto_balance)
            + ", мобы: " + onOff(config.auto_touch_mobs)
            + ", лут: " + onOff(config.auto_touch_loot));
        send(source, "Коридор темпа: " + GameMaster.fmt(config.auto_start_minutes) + " мин на 1 ур. → "
            + GameMaster.fmt(config.auto_end_minutes) + " мин на макс., мёртвая зона ±"
            + GameMaster.fmt(config.auto_dead_zone * 100) + "%");
        send(source, "Суммарно до максимума: ~" + GameMaster.fmt(gm.autoTargetHours()) + " ч (цель подстраивается)");
        send(source, "Опыт: x" + GameMaster.fmt(config.auto_exp_min) + "…x" + GameMaster.fmt(config.auto_exp_max)
            + ", шаг " + GameMaster.fmt(config.auto_exp_step_per_minute) + "/мин; тормоз по смертям: "
            + GameMaster.fmt(config.auto_deaths_brake) + "/ч");
        send(source, "Сейчас мобы: HP x" + GameMaster.fmt(gm.mobHealthMultiplier()) + ", урон x"
            + GameMaster.fmt(gm.mobDamageMultiplier()) + ", лут x" + GameMaster.fmt(gm.lootMultiplier()));
        return 1;
    }

    private static int status(CommandSourceStack source) {
        GameMaster gm = GameMaster.get();
        GameMasterConfig config = gm.config();

        source.sendSystemMessage(Component.literal("GameMaster — " + (gm.isLoaded() ? "активен" : "не запущен"))
            .withStyle(ChatFormatting.GOLD));
        send(source, "Мастер-выключатель: " + onOff(config.enabled)
            + ", уравнивание: " + onOff(config.catch_up)
            + ", темп: " + onOff(config.pacing));
        send(source, "Спавн мобов: " + onOff(config.enabled && config.spawn_rate_enabled)
            + ", динамика по Благосклонности: " + onOff(config.spawn_rate_dynamic)
            + ", ручной множитель: x" + GameMaster.fmt(config.spawn_rate_manual_multiplier)
            + ", игроков с override: " + config.spawn_rate_player_overrides.size());
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
        if (config.auto_balance && config.enabled) {
            send(source, "Автобаланс: вкл, коридор " + GameMaster.fmt(config.auto_start_minutes) + "–"
                + GameMaster.fmt(config.auto_end_minutes) + " мин/уровень (±"
                + GameMaster.fmt(config.auto_dead_zone * 100) + "%), суммарно ~"
                + GameMaster.fmt(gm.autoTargetHours()) + " ч до максимума");
            send(source, "Мобы: HP x" + GameMaster.fmt(gm.mobHealthMultiplier()) + ", урон x"
                + GameMaster.fmt(gm.mobDamageMultiplier()) + ", лут x" + GameMaster.fmt(gm.lootMultiplier()));
        } else {
            send(source, "Автобаланс: выкл (/gm auto on).");
        }
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
                .name() + " ещё нет данных.");
            return 1;
        }
        GameMaster.MultiplierBreakdown breakdown = gm.breakdown(progress);
        send(source, target.getGameProfile()
            .name() + ": уровень " + progress.level + ", время " + GameMaster.fmt(progress.playtimeHours())
            + " ч, Благосклонность " + progress.favor);
        send(source, "Spawn-rate мобов: x" + GameMaster.fmt(gm.spawnRateMultiplier(target))
            + " (динамический x" + GameMaster.fmt(gm.spawnRateDynamicMultiplier(progress.favor)) + ")");
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
                + GameMaster.fmt(progress.playtimeHours()) + " ч, опыт x" + GameMaster.fmt(breakdown.total)
                + ", спавн x" + GameMaster.fmt(gm.spawnRateMultiplier(progress.uuid, progress.favor))
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
