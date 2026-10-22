package com.robertx22.mine_and_slash.vanilla_mc.commands.entity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType; // Added import for StringArgumentType
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.loot.LootModifiersList; // Kept import, though not used in the corrected run method
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component; // Added import for sending messages

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class GiveProfessionExp {

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
                literal(CommandRefs.ID)
                        .then(literal("give").requires(e -> e.hasPermission(2))
                                .then(literal("prof_exp")
                                        .requires(e -> e.hasPermission(2))
                                        .then(argument("target", EntityArgument.player())
                                                .then(argument("professionId", StringArgumentType.string()) // Changed argument type to string and name to professionId
                                                        .then(argument("exp", IntegerArgumentType.integer())
                                                                .executes(ctx -> run(EntityArgument.getPlayer(ctx, "target"),
                                                                        StringArgumentType.getString(ctx, "professionId"), // Get the professionId string
                                                                        IntegerArgumentType.getInteger(ctx, "exp"))))))))); // Pass all arguments to run
    }

    private static int run(Player player, String professionId, int exp) { // Added professionId parameter

        try {
            PlayerData data = Load.player(player);
            data.professions.addExp(player, professionId, exp);

            //player.sendSystemMessage(Component.literal("Gained " + exp + " " + professionId + " Experience")); // Success message

        } catch (Exception e) {
            e.printStackTrace();
            player.sendSystemMessage(Component.literal("Failed to give experience: " + e.getMessage())); // Error message
        }

        return 1; // Return 1 to indicate success
    }
}
