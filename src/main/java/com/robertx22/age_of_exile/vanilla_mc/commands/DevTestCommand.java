package com.robertx22.age_of_exile.vanilla_mc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.age_of_exile.uncommon.testing.CombatProbe;
import com.robertx22.age_of_exile.uncommon.testing.SpellAuditProbe;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

/**
 * {@code /aoe devtest <dps|heal|gear|all>} — замеры боевых чисел в живом мире.
 *
 * <p>Зонд работает по тикам (см. {@link CombatProbe}), поэтому команда только
 * ставит прогон в очередь, а результат появляется в логе строками
 * {@code AOETEST ...} и в {@code aoe-devtest.txt}. Те же строки разбирает
 * CI-сценарий {@code tools/e2e/scenarios/combat.txt}.
 */
public final class DevTestCommand {

    private DevTestCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal(CommandRefs.ID)
            .then(literal("devtest")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> run(ctx.getSource().getPlayerOrException(), "all"))
                .then(literal("dps").executes(ctx -> run(ctx.getSource().getPlayerOrException(), "dps")))
                .then(literal("heal").executes(ctx -> run(ctx.getSource().getPlayerOrException(), "heal")))
                .then(literal("gear").executes(ctx -> run(ctx.getSource().getPlayerOrException(), "gear")))
                .then(literal("all").executes(ctx -> run(ctx.getSource().getPlayerOrException(), "all"))))
            .then(literal("spellaudit")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> audit(ctx.getSource().getPlayerOrException(), "all"))
                .then(literal("all").executes(ctx -> audit(ctx.getSource().getPlayerOrException(), "all")))
                .then(literal("no_school")
                    .executes(ctx -> audit(ctx.getSource().getPlayerOrException(), "school=no_school")))
                .then(Commands.argument("what", com.mojang.brigadier.arguments.StringArgumentType.string())
                    .executes(ctx -> audit(ctx.getSource().getPlayerOrException(),
                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "what")))))
            .then(literal("profaudit")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> profaudit(ctx.getSource().getPlayerOrException(), "all"))
                .then(literal("all").executes(ctx -> profaudit(ctx.getSource().getPlayerOrException(), "all")))
                .then(Commands.argument("what", com.mojang.brigadier.arguments.StringArgumentType.string())
                    .executes(ctx -> profaudit(ctx.getSource().getPlayerOrException(),
                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "what"))))));
    }

    private static int audit(ServerPlayer player, String what) {
        String target = what == null || what.isBlank() ? "all" : what.trim();
        SpellAuditProbe.request(target);
        player.sendSystemMessage(Component.literal(
            "§a[Аудит умений] " + target + ": запущено, результат — строки AOEAI в логе и spell-audit.md"));
        return 1;
    }

    private static int profaudit(ServerPlayer player, String what) {
        String target = what == null || what.isBlank() ? "all" : what.trim();
        com.robertx22.age_of_exile.uncommon.testing.ProfessionAuditProbe.request(target);
        player.sendSystemMessage(Component.literal(
            "§a[Аудит профессий] " + target + ": запущено, результат — строки AOEPR в логе и profession-audit.md"));
        return 1;
    }

    private static int run(ServerPlayer player, String what) {
        CombatProbe.request(what);
        player.sendSystemMessage(Component.literal(
            "§a[Проба] " + what + ": запущено, результат — строки AOETEST в логе"));
        return 1;
    }
}
