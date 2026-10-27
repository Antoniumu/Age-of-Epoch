package com.robertx22.age_of_exile.airship;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import static net.minecraft.commands.Commands.literal;

public final class AirshipCommand {
    private AirshipCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("airship").requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .executes(c -> AirshipService.summon(c.getSource().getPlayerOrException(),false))
            .then(literal("summon").executes(c -> AirshipService.summon(c.getSource().getPlayerOrException(),false)))
            .then(literal("descend").executes(c -> AirshipService.descend(c.getSource().getPlayerOrException())))
            .then(literal("winged").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> WingedSailorService.summon(c.getSource().getPlayerOrException())))
            .then(literal("demo").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> AirshipService.summon(c.getSource().getPlayerOrException(),true))));
    }
}
