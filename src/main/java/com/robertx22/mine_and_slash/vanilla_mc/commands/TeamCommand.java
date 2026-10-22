package com.robertx22.mine_and_slash.vanilla_mc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.capability.player.data.TeamData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Command;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class TeamCommand {

    public static List<Component> playerNameColorize(Player asker, List<Player> allPlayer){
        List<Component> allPlayersName = allPlayer.stream().map(Player::getDisplayName).collect(Collectors.toList());

        for (int i = 0; i < allPlayersName.size(); i++) {
            Component playerName = allPlayersName.get(i);
            Player player = allPlayer.get(i);
            if (asker.getDisplayName().equals(playerName)) {
                if (Load.player(asker).team.isLeader){
                    allPlayersName.set(i, playerName.copy().withStyle(ChatFormatting.GOLD).append(" \u2605"));
                }
                Collections.swap(allPlayersName, 0, i);
                Collections.swap(allPlayer, 0, i);
            } else {
                boolean onSameTeam = Load.player(asker).team.isOnSameTeam(player);
                boolean autoPVE = Load.player(asker).config.isConfigEnabled(PlayerConfigData.Config.AUTO_PVE) && Load.player(player).config.isConfigEnabled(PlayerConfigData.Config.AUTO_PVE);
                boolean tooFar = asker.distanceTo(player) > ServerContainer.get().MAX_TEAM_DISTANCE.get();
                if (onSameTeam || autoPVE){
                    if (onSameTeam){
                        if (tooFar){
                            allPlayersName.set(i, Component.literal("").append(playerName.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)).append(Command.NOT_IN_SAME_TEAM_DUE_TO_DISTANCE.locName().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
                        } else {
                            if (Load.player(player).team.isLeader){
                                allPlayersName.set(i, playerName.copy().withStyle(ChatFormatting.GOLD).append(" \u2605"));
                            } else {
                                allPlayersName.set(i, playerName.copy().withStyle(ChatFormatting.WHITE));
                            }
                        }
                    } else {
                        if (tooFar) {
                            allPlayersName.remove(i);
                            i--;
                        } else {
                            allPlayersName.set(i, Component.literal("").append(playerName.copy().withStyle(ChatFormatting.AQUA)).append(Command.AUTO_PVE_TEAMMATE.locName().withStyle(ChatFormatting.AQUA)));
                        }
                    }

                } else {
                    allPlayersName.remove(i);
                    i--;
                }
            }
        }
        return allPlayersName;
    }

    public static void listMembers(Player player) {
        List<Component> components = playerNameColorize(player, player.level().players().stream().map(x -> (Player)x).collect(Collectors.toList()));

        player.sendSystemMessage(Command.TEAM_MEMBER.locName());

        components.forEach(e -> {
            player.sendSystemMessage(e);
        });
    }


    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
                literal(CommandRefs.ID).requires(x -> true)
                        .then(literal("teams").requires(e -> true)
                                .then(literal("create").executes(x -> {
                                    ServerPlayer player = x.getSource()
                                            .getPlayerOrException();

                                    Load.player(player).team
                                            .createTeam();
                                    player.sendSystemMessage(Command.TEAM_CREATED.locName());
                                    return 0;
                                }))
                                .then(literal("leave").executes(x -> {
                                    ServerPlayer player = x.getSource()
                                            .getPlayerOrException();
                                    Load.player(player).team
                                            .leaveTeam();
                                    player.sendSystemMessage(Command.LEAVE_TEAM.locName());

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

                                                    if (!Load.player(player).team
                                                            .isOnTeam()) {
                                                        player.sendSystemMessage(Command.NOT_IN_TEAM.locName());
                                                        player.sendSystemMessage(Command.CLICK_TO_CREATE.locName().withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand( "/" + CommandRefs.ID + " teams create")).withUnderlined(true).withColor(ChatFormatting.GREEN)));
                                                        return 0;
                                                    }

                                                    Player other = EntityArgument.getPlayer(x, "player");

                                                    if (player.getDisplayName().equals(other.getDisplayName())){
                                                        player.sendSystemMessage(Command.CANT_INVITE_YOURSELF.locName());
                                                        return 0;
                                                    }

                                                    other.sendSystemMessage(Command.BEEN_INVITED.locName(player.getDisplayName()));
                                                    other.sendSystemMessage(Command.JOIN_TIP.locName(player.getDisplayName()));
                                                    other.sendSystemMessage(Command.CLICK_TO_JOIN.locName().withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand( "/" + CommandRefs.ID + " teams join " + player.getDisplayName().getString())).withUnderlined(true).withColor(ChatFormatting.GREEN)));

                                                    Load.player(other).team.invitedToTeam = Load.player(player).team.team_id;

                                                    player.sendSystemMessage(Command.INVITE_INFO.locName(other.getDisplayName()));

                                                    return 0;
                                                })

                                        ))
                                .then(literal("make_leader")
                                        .then(argument("player", EntityArgument.player())
                                                .executes(x -> {
                                                    Player player = x.getSource()
                                                            .getPlayerOrException();

                                                    Player other = EntityArgument.getPlayer(x, "player");
                                                    TeamData team = Load.player(player).team;

                                                    if (!team.isLeader){
                                                        player.sendSystemMessage(Command.YOU_ARE_NOT_LEADER.locName());
                                                        return 0;
                                                    }
                                                    if (player.getDisplayName().equals(other.getDisplayName())){
                                                        player.sendSystemMessage(Command.CANT_MAKE_LEADER.locName());
                                                        return 0;
                                                    }

                                                    if (!team.isOnSameTeam(other)) {
                                                        player.sendSystemMessage(Command.PLAYER_NOT_IN_TEAM.locName());

                                                        return 0;
                                                    }

                                                    player.sendSystemMessage(Command.TRANSFER_LEADERSHIP.locName(player.getDisplayName()));

                                                    other.sendSystemMessage(Command.YOU_ARE_LEADER.locName());

                                                    Load.player(other).team.isLeader = true;

                                                    return 0;
                                                })

                                        ))
                                .then(literal("kick")
                                        .then(argument("player", EntityArgument.player())
                                                .executes(x -> {
                                                    Player player = x.getSource()
                                                            .getPlayerOrException();

                                                    Player other = EntityArgument.getPlayer(x, "player");

                                                    if (!Load.player(player).team.isLeader) {
                                                        player.sendSystemMessage(Command.YOU_ARE_NOT_LEADER.locName());
                                                        return 0;
                                                    }
                                                    if (player.getDisplayName().equals(other.getDisplayName())){
                                                        player.sendSystemMessage(Command.LEAVE_TEAM.locName());
                                                        Load.player(player).team.team_id = "";
                                                        return 0;
                                                    } else {
                                                        if (!Load.player(player).team
                                                                .isOnSameTeam(other)) {
                                                            player.sendSystemMessage(Command.PLAYER_NOT_IN_TEAM.locName());
                                                            return 0;
                                                        }
                                                    }


                                                    Load.player(other).team.team_id = "";

                                                    other.sendSystemMessage(Command.BEEN_KICKED.locName());

                                                    player.sendSystemMessage(Command.KICK_INFO.locName(other.getDisplayName()));

                                                    return 0;
                                                })

                                        ))
                                .then(literal("join")
                                        .then(argument("player", EntityArgument.player())
                                                .executes(x -> {
                                                    Player player = x.getSource()
                                                            .getPlayerOrException();

                                                    Player other = EntityArgument.getPlayer(x, "player");

                                                    if (Load.player(other).team
                                                            .isOnTeam()) {

                                                        if (Load.player(player).team.invitedToTeam.equals(Load.player(other).team.team_id)) {
                                                            Load.player(player).team.joinTeamOf(other);

                                                            player.sendSystemMessage(Command.JOIN_TEAM.locName(other.getDisplayName()));
                                                            other.sendSystemMessage(Command.SOMEBODY_JOINED_YOUR_TEAM.locName(player.getDisplayName()));

                                                        } else {
                                                            player.sendSystemMessage(Command.NOT_INVITED.locName());
                                                        }

                                                    } else {
                                                        player.sendSystemMessage(Command.NOT_IN_A_TEAM_CURRENTLY.locName());
                                                    }
                                                    return 0;
                                                })

                                        ))));
    }

}
