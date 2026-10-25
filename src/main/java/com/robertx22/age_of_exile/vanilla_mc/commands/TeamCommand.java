package com.robertx22.age_of_exile.vanilla_mc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.robertx22.age_of_exile.capability.PlayerDamageChart;
import com.robertx22.age_of_exile.capability.player.data.TeamData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TeamUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Team management commands exposed under /aoe teams. */
public final class TeamCommand {
    private TeamCommand() {
    }

    private static Component info(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    private static boolean isLeader(Player player) {
        return Load.playerRPGData(player).team.isLeader;
    }

    private static void showTeam(Player player) {
        TeamData data = Load.playerRPGData(player).team;
        if (!data.isOnTeam()) {
            player.sendSystemMessage(info("Вы не состоите в команде. Создайте её: /aoe teams create <название>"));
            return;
        }
        player.sendSystemMessage(Component.literal("✦ Команда «" + data.displayName() + "»"
                + (data.isLeader ? " · лидер" : ""))
            .withStyle(ChatFormatting.AQUA));
        List<Player> members = TeamUtils.getOnlineMembers(player);
        members.forEach(member -> player.sendSystemMessage(Component.literal("  • ")
            .append(member.getDisplayName()).withStyle(member == player ? ChatFormatting.WHITE : ChatFormatting.GRAY)));
    }

    public static void listMembers(Player player) {
        showTeam(player);
    }

    public static void sendDpsCharts(Player player) {
        List<Player> members = TeamUtils.getOnlineMembers(player);
        player.sendSystemMessage(Component.literal("⚔ Урон команды").withStyle(ChatFormatting.GOLD));
        members.stream()
            .sorted((a, b) -> Float.compare(PlayerDamageChart.getDamage(b), PlayerDamageChart.getDamage(a)))
            .forEach(member -> player.sendSystemMessage(Component.literal("  • ")
                .append(member.getDisplayName())
                .append(Component.literal(": " + (int) PlayerDamageChart.getDamage(member)).withStyle(ChatFormatting.GRAY))));
    }

    private static int create(ServerPlayer player, String name) {
        TeamData team = Load.playerRPGData(player).team;
        if (team.isOnTeam()) {
            player.sendSystemMessage(info("Вы уже состоите в команде. Сначала выйдите: /aoe teams leave"));
            return 0;
        }
        String cleaned = TeamData.sanitizeName(name);
        team.createTeam(cleaned);
        String label = team.displayName();
        player.sendSystemMessage(Component.literal("✦ Команда «" + label + "» создана."
                + (cleaned.isEmpty() ? " Название можно задать: /aoe teams rename <название>" : ""))
            .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(info("Пригласите союзников: /aoe teams invite <игрок>"));
        return 1;
    }

    /** Переименование команды: доступно только лидеру, имя чистится от §-кодов. */
    private static int rename(ServerPlayer player, String name) {
        TeamData team = Load.playerRPGData(player).team;
        if (!team.isOnTeam()) {
            player.sendSystemMessage(info("Вы не состоите в команде. Создайте её: /aoe teams create <название>"));
            return 0;
        }
        if (!team.isLeader) {
            player.sendSystemMessage(info("Переименовать команду может только лидер."));
            return 0;
        }
        String cleaned = TeamData.sanitizeName(name);
        if (cleaned.isEmpty()) {
            player.sendSystemMessage(info("Название пустое: используйте /aoe teams rename <название>"));
            return 0;
        }
        team.team_name = cleaned;
        player.sendSystemMessage(Component.literal("✦ Команда переименована в «" + cleaned + "»")
            .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int leave(ServerPlayer player) {
        TeamData team = Load.playerRPGData(player).team;
        if (!team.isOnTeam()) {
            player.sendSystemMessage(info("Вы не состоите в команде."));
            return 0;
        }
        List<Player> previousMembers = TeamUtils.getOnlineMembers(player);
        previousMembers.removeIf(member -> member.getUUID().equals(player.getUUID()));
        String oldId = team.team_id;
        boolean wasLeader = team.isLeader;
        team.leaveTeam();
        team.isLeader = false;
        team.invitedToTeam = "";
        player.sendSystemMessage(info("Вы покинули команду."));
        // Keep a team usable when its leader leaves: promote the first online member.
        if (wasLeader) {
            Player successor = previousMembers.stream()
                .filter(member -> oldId.equals(Load.playerRPGData(member).team.team_id))
                .findFirst().orElse(null);
            if (successor != null) {
                Load.playerRPGData(successor).team.isLeader = true;
                successor.sendSystemMessage(Component.literal("Вы стали лидером команды.").withStyle(ChatFormatting.AQUA));
            }
        }
        return 1;
    }

    private static int invite(ServerPlayer inviter, ServerPlayer invitee) {
        TeamData team = Load.playerRPGData(inviter).team;
        if (!team.isOnTeam()) {
            inviter.sendSystemMessage(info("Сначала создайте команду или вступите в неё."));
            return 0;
        }
        if (invitee.getUUID().equals(inviter.getUUID())) {
            inviter.sendSystemMessage(info("Нельзя пригласить самого себя."));
            return 0;
        }
        TeamData target = Load.playerRPGData(invitee).team;
        if (target.isOnTeam()) {
            inviter.sendSystemMessage(info("Этот игрок уже состоит в команде."));
            return 0;
        }
        target.invitedToTeam = team.team_id;
        invitee.sendSystemMessage(Component.literal("✉ ").withStyle(ChatFormatting.GOLD)
            .append(inviter.getDisplayName()).append(info(" приглашает вас в команду. Принять: /aoe teams join "))
            .append(inviter.getDisplayName()));
        inviter.sendSystemMessage(Component.literal("Приглашение отправлено игроку ").withStyle(ChatFormatting.GREEN)
            .append(invitee.getDisplayName()).append(info(".")));
        return 1;
    }

    private static int join(ServerPlayer player, ServerPlayer inviter) {
        TeamData ownTeam = Load.playerRPGData(player).team;
        TeamData otherTeam = Load.playerRPGData(inviter).team;
        if (ownTeam.isOnTeam()) {
            player.sendSystemMessage(info("Сначала покиньте текущую команду: /aoe teams leave"));
            return 0;
        }
        if (!otherTeam.isOnTeam()) {
            player.sendSystemMessage(info("Этот игрок сейчас не состоит в команде."));
            return 0;
        }
        if (!otherTeam.team_id.equals(ownTeam.invitedToTeam)) {
            player.sendSystemMessage(info("Приглашение не найдено или уже недействительно."));
            return 0;
        }
        ownTeam.joinTeamOf(inviter);
        ownTeam.isLeader = false;
        ownTeam.invitedToTeam = "";
        player.sendSystemMessage(Component.literal("Вы присоединились к команде ").withStyle(ChatFormatting.GREEN)
            .append(inviter.getDisplayName()).append(info(".")));
        inviter.sendSystemMessage(Component.literal("+ ").withStyle(ChatFormatting.GREEN)
            .append(player.getDisplayName()).append(info(" присоединился к команде.")));
        return 1;
    }

    private static int kick(ServerPlayer leader, ServerPlayer member) {
        if (!isLeader(leader)) {
            leader.sendSystemMessage(info("Исключать участников может только лидер команды."));
            return 0;
        }
        if (!Load.playerRPGData(leader).team.isOnSameTeam(member)) {
            leader.sendSystemMessage(info("Этот игрок не состоит в вашей команде."));
            return 0;
        }
        if (leader.getUUID().equals(member.getUUID())) {
            leader.sendSystemMessage(info("Лидер не может исключить самого себя. Используйте /aoe teams leave."));
            return 0;
        }
        TeamData memberTeam = Load.playerRPGData(member).team;
        memberTeam.leaveTeam();
        memberTeam.isLeader = false;
        memberTeam.invitedToTeam = "";
        member.sendSystemMessage(Component.literal("Вы исключены из команды.").withStyle(ChatFormatting.RED));
        leader.sendSystemMessage(Component.literal("Игрок ").withStyle(ChatFormatting.GREEN)
            .append(member.getDisplayName()).append(info(" исключён из команды.")));
        return 1;
    }

    private static int makeLeader(ServerPlayer leader, ServerPlayer nextLeader) {
        TeamData team = Load.playerRPGData(leader).team;
        if (!team.isLeader) {
            leader.sendSystemMessage(info("Передать лидерство может только лидер команды."));
            return 0;
        }
        if (leader.getUUID().equals(nextLeader.getUUID()) || !team.isOnSameTeam(nextLeader)) {
            leader.sendSystemMessage(info("Выберите другого участника своей команды."));
            return 0;
        }
        team.isLeader = false;
        Load.playerRPGData(nextLeader).team.isLeader = true;
        leader.sendSystemMessage(Component.literal("Лидерство передано игроку ").withStyle(ChatFormatting.GREEN)
            .append(nextLeader.getDisplayName()).append(info(".")));
        nextLeader.sendSystemMessage(Component.literal("Вы назначены лидером команды.").withStyle(ChatFormatting.AQUA));
        return 1;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal(CommandRefs.ID)
            .then(literal("teams")
                .then(literal("help").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("Команды команды: /aoe teams create [название], rename <название>, invite <игрок>, join <пригласивший>, leave, members, dps. Лидер: kick <игрок>, make_leader <игрок>.")
                        .withStyle(ChatFormatting.AQUA), false);
                    return 1;
                }))
                .then(literal("create")
                    .executes(ctx -> create(ctx.getSource().getPlayerOrException(), ""))
                    .then(argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> create(ctx.getSource().getPlayerOrException(),
                            StringArgumentType.getString(ctx, "name")))))
                .then(literal("rename")
                    .then(argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> rename(ctx.getSource().getPlayerOrException(),
                            StringArgumentType.getString(ctx, "name")))))
                .then(literal("leave").executes(ctx -> leave(ctx.getSource().getPlayerOrException())))
                .then(literal("members").executes(ctx -> { showTeam(ctx.getSource().getPlayerOrException()); return 1; }))
                .then(literal("list_members").executes(ctx -> { showTeam(ctx.getSource().getPlayerOrException()); return 1; }))
                .then(literal("status").executes(ctx -> { showTeam(ctx.getSource().getPlayerOrException()); return 1; }))
                .then(literal("dps").executes(ctx -> { sendDpsCharts(ctx.getSource().getPlayerOrException()); return 1; }))
                .then(literal("dps_chart").executes(ctx -> { sendDpsCharts(ctx.getSource().getPlayerOrException()); return 1; }))
                .then(literal("invite").then(argument("player", EntityArgument.player())
                    .executes(ctx -> invite(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player")))))
                .then(literal("join").then(argument("player", EntityArgument.player())
                    .executes(ctx -> join(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player")))))
                .then(literal("kick").then(argument("player", EntityArgument.player())
                    .executes(ctx -> kick(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player")))))
                .then(literal("make_leader").then(argument("player", EntityArgument.player())
                    .executes(ctx -> makeLeader(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player")))))));
    }
}
