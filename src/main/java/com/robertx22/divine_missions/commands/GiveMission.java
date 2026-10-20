package com.robertx22.divine_missions.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.mission_gen.MissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.player.Player;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GiveMission {

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
            literal(DivineMissions.MODID)
                .then(literal("give_random_mission").requires(e -> e.hasPermission(2))
                    .requires(e -> e.hasPermission(2))
                    .then(argument("target", EntityArgument.player())
                        .executes(e -> execute(e.getSource(), EntityArgument.getPlayer(e, "target"))))));
    }

    private static int execute(CommandSourceStack commandSource, Player player) {

        player.addItem(MissionUtil.createRandomMissionItem(player));

        return 0;
    }
}