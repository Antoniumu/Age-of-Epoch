package com.robertx22.age_of_exile.airship;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import static net.minecraft.commands.Commands.literal;

public final class AirshipCommand {
    private AirshipCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("airship").requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .executes(c -> {AirshipHubRitual.request(c.getSource().getPlayerOrException(),0,"");return 1;})
            .then(literal("summon").executes(c -> {AirshipHubRitual.request(c.getSource().getPlayerOrException(),0,"");return 1;}))
            .then(literal("recover").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->AirshipJump.recover(c.getSource().getPlayerOrException())))
            .then(literal("navigator").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->{var p=c.getSource().getPlayerOrException();var server=((net.minecraft.server.level.ServerLevel)p.level()).getServer();var h=AirshipHub.state(server);if(!h.phase.equals("IDLE"))return 0;h.navigator=p.getUUID().toString();AirshipHub.save(server);p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Ты назначен навигатором общего хаба."));return 1;}))
            .then(literal("refresh").executes(c -> AirshipCrewPlacement.refresh(c.getSource().getPlayerOrException())))
            .then(literal("descend").executes(c -> AirshipService.descend(c.getSource().getPlayerOrException())))
            .then(literal("winged").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> WingedSailorService.summon(c.getSource().getPlayerOrException())))
            .then(literal("demo").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> AirshipService.summon(c.getSource().getPlayerOrException(),true))));
    }
}
