package com.robertx22.mine_and_slash.vanilla_mc.commands.entity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class GivePerLvlProfessionExp {

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
                literal(CommandRefs.ID)
                        .then(literal("give").requires(e -> e.hasPermission(2))
                                .then(literal("prof_xp_times_lvl")
                                        .requires(e -> e.hasPermission(2))
                                        .then(argument("target", EntityArgument.player())
                                                .then(argument("professionId", StringArgumentType.string())
                                                        .then(argument("exp", IntegerArgumentType.integer())
                                                                .executes(ctx -> run(EntityArgument.getPlayer(ctx, "target"),
                                                                        StringArgumentType.getString(ctx, "professionId"),
                                                                        IntegerArgumentType.getInteger(ctx, "exp")))))))));
    }

    private static int run(Player player, String professionId, int exp) {

        try {
            PlayerData data = Load.player(player);

            // Get the profession level for the given profession
            int professionLevel = data.professions.getLevel(professionId);

            int total = exp * professionLevel;

            data.professions.addExp(player, professionId, total);
            // player.sendSystemMessage(Component.literal("Gained " + total + " " + professionId + " Experience (based on level " + professionLevel + ")"));

        } catch (Exception e) {
            e.printStackTrace();
            player.sendSystemMessage(Component.literal("Failed to give experience: " + e.getMessage()));
        }

        return 1;
    }
}
