package com.robertx22.age_of_exile.vanilla_mc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.age_of_exile.capability.PlayerDamageChart;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TeamUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TeamCommand {

    public static void listMembers(Player player) {
        List<Player> players = TeamUtils.getOnlineMembers(player);

        player.sendSystemMessage(Component.literal("Team members:"));

        players.forEach(e -> {
            player.sendSystemMessage(e.getDisplayName());
        });
    }

    public static void sendDpsCharts(Player player) {
        List<Player> members = TeamUtils.getOnlineMembers(player);

        player.sendSystemMessage(Component.literal("Damage Charts:"));

        members.forEach(e -> {
            Component text = Component.literal("").append(e.getDisplayName())
                .append(": " + (int) PlayerDamageChart.getDamage(e));

            player.sendSystemMessage(text);

        });

    }

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(

            literal(CommandRefs.ID)
                .then(literal("teams").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_ALL))

                    .then(literal("create").executes(x -> {
                        Load.playerRPGData(x.getSource()
                                .getPlayerOrException()).team
                            .createTeam();
                        return 0;
                    }))
                    .then(literal("leave").executes(x -> {
                        Load.playerRPGData(x.getSource()
                                .getPlayerOrException()).team
                            .leaveTeam();
                        return 0;
                    }))
                    .then(literal("dps_chart").executes(x -> {

                        Player player = x.getSource()
                            .getPlayerOrException();

                        sendDpsCharts(player);
                        return 0;
                    }))
                    .then(literal("list_members").executes(x -> {

                        Player player = x.getSource()
                            .getPlayerOrException();
                        listMembers(player);

                        return 0;
                    }))
                    .then(literal("invite")
                        .then(argument("player", EntityArgument.player())
                            .executes(x -> {
                                Player player = x.getSource()
                                    .getPlayerOrException();

                                if (!Load.playerRPGData(player).team
                                    .isOnTeam()) {
                                    player.sendSystemMessage(Component.literal("You are not in a team."));
                                    return 0;
                                }

                                Player other = EntityArgument.getPlayer(x, "player");

                                other.sendSystemMessage(Component.literal("").append(player.getDisplayName())
                                    .append(" has invited you to a team."));
                                other.sendSystemMessage(Component.literal("Do /age_of_exile teams join ").append(player.getDisplayName())
                                    .append(" to accept"));

                                Load.playerRPGData(other).team.invitedToTeam = Load.playerRPGData(player).team.team_id;

                                player.sendSystemMessage(Component.literal("You have invited ").append(other.getDisplayName())
                                    .append(Component.literal(" to join your team.")));

                                return 0;
                            })

                        ))
                    .then(literal("make_leader")
                        .then(argument("player", EntityArgument.player())
                            .executes(x -> {
                                Player player = x.getSource()
                                    .getPlayerOrException();

                                Player other = EntityArgument.getPlayer(x, "player");

                                if (!Load.playerRPGData(player).team
                                    .isOnSameTeam(other)) {
                                    player.sendSystemMessage(Component.literal("They aren't on your team"));
                                    return 0;
                                }

                                player.sendSystemMessage(Component.literal("").append(player.getDisplayName())
                                    .append(" is now a team leader."));

                                player.sendSystemMessage(Component.literal("You have been made a team leader."));

                                Load.playerRPGData(other).team.isLeader = true;

                                player.sendSystemMessage(Component.literal("You have invited ").append(other.getDisplayName())
                                    .append(Component.literal(" to join your team.")));

                                return 0;
                            })

                        ))
                    .then(literal("kick")
                        .then(argument("player", EntityArgument.player())
                            .executes(x -> {
                                Player player = x.getSource()
                                    .getPlayerOrException();

                                Player other = EntityArgument.getPlayer(x, "player");

                                if (!Load.playerRPGData(player).team.isLeader) {
                                    player.sendSystemMessage(Component.literal("You can't kick members because you aren't the leader."));
                                    return 0;
                                }
                                if (!Load.playerRPGData(player).team
                                    .isOnSameTeam(other)) {
                                    player.sendSystemMessage(Component.literal("They aren't on your team"));
                                    return 0;
                                }

                                Load.playerRPGData(other).team.team_id = "";

                                other.sendSystemMessage(Component.literal("You have been kicked from your team."));

                                player.sendSystemMessage(Component.literal("You have kicked ").append(other.getDisplayName())
                                    .append(" from your team."));

                                return 0;
                            })

                        ))
                    .then(literal("join")
                        .then(argument("player", EntityArgument.player())
                            .executes(x -> {
                                Player player = x.getSource()
                                    .getPlayerOrException();

                                Player other = EntityArgument.getPlayer(x, "player");

                                if (Load.playerRPGData(other).team
                                    .isOnTeam()) {

                                    if (Load.playerRPGData(player).team.invitedToTeam.equals(Load.playerRPGData(other).team.team_id)) {
                                        Load.playerRPGData(x.getSource()
                                                .getPlayerOrException()).team
                                            .joinTeamOf(other);

                                        player.sendSystemMessage(Component.literal("You have joined ").append(other.getDisplayName())
                                            .append(Component.literal("'s team.")));
                                    } else {
                                        player.sendSystemMessage(Component.literal("You aren't invited to their team"));
                                    }

                                } else {
                                    x.getSource()
                                        .getPlayerOrException()
                                        .sendSystemMessage(Component.literal("Player isn't in a team"));
                                }
                                return 0;
                            })

                        ))));
    }

}
