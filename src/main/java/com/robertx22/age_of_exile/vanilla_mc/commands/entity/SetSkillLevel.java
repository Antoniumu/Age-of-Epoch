package com.robertx22.age_of_exile.vanilla_mc.commands.entity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.vanilla_mc.commands.CommandRefs;
import com.robertx22.age_of_exile.vanilla_mc.commands.suggestions.DatabaseSuggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class SetSkillLevel {

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
            literal(CommandRefs.ID)
                .then(literal("set").requires(e -> e.hasPermission(2))
                    .then(literal("skill_level")
                        .then(argument("skill", StringArgumentType.string())
                            .suggests(new DatabaseSuggestions(ExileRegistryTypes.PLAYER_SKILLS))
                            .requires(e -> e.hasPermission(2))
                            .then(argument("target", EntityArgument.player())
                                .then(argument("level", IntegerArgumentType.integer())
                                    .executes(e -> execute(e.getSource(), EntityArgument.getPlayer(e, "target"), IntegerArgumentType
                                        .getInteger(e, "level"), StringArgumentType.getString(e, "skill")))))))));
    }

    private static int execute(CommandSourceStack commandSource, Player player,
                               int lvl, String id) {
        if (Objects.isNull(player)) {
            try {
                player = commandSource.getPlayerOrException();
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                return 1;
            }
        }

        Load.playerRPGData(player).professions
            .getDataFor(id)
            .setLvl(lvl);

        return 0;
    }
}