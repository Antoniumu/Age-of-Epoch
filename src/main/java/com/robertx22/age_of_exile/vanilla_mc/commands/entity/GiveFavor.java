package com.robertx22.age_of_exile.vanilla_mc.commands.entity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.vanilla_mc.commands.CommandRefs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.player.Player;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GiveFavor {

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
            literal(CommandRefs.ID)
                .then(literal("give").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                    .then(literal("favor")
                        .requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                        .then(argument("target", EntityArgument.player())
                            .then(argument("favor", IntegerArgumentType.integer())
                                .executes(ctx -> run(EntityArgument.getPlayer(ctx, "target"), IntegerArgumentType
                                    .getInteger(ctx, "favor"))))))));
    }

    private static int run(Player player, int favor) {

        try {
            Load.playerRPGData(player).favor
                .addFavor(favor);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 1;
    }
}