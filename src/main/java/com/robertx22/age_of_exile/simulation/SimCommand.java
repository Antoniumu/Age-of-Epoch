package com.robertx22.age_of_exile.simulation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class SimCommand {
    private SimCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sim")
            .then(Commands.literal("enter").executes(ctx -> {
                ServerPlayer sp = ctx.getSource().getPlayerOrException();
                SimulationManager.enterSimulation(sp);
                return 1;
            }))
            .then(Commands.literal("exit").executes(ctx -> {
                ServerPlayer sp = ctx.getSource().getPlayerOrException();
                SimulationManager.exitSimulation(sp);
                return 1;
            }))
            .then(Commands.literal("kit").executes(ctx -> {
                ServerPlayer sp = ctx.getSource().getPlayerOrException();
                SimulationManager.grantTestKit(sp);
                return 1;
            }))
            .then(Commands.literal("reset").executes(ctx -> {
                ServerPlayer sp = ctx.getSource().getPlayerOrException();
                SimulationManager.resetAllAllocations(sp);
                return 1;
            }))
            .then(Commands.literal("dummy").executes(ctx -> {
                ServerPlayer sp = ctx.getSource().getPlayerOrException();
                SimulationManager.spawnDummy(sp, false);
                return 1;
            }))
            .then(Commands.literal("boss").executes(ctx -> {
                ServerPlayer sp = ctx.getSource().getPlayerOrException();
                SimulationManager.spawnDummy(sp, true);
                return 1;
            }))
            .then(Commands.literal("level").then(Commands.argument("lvl", IntegerArgumentType.integer(1, 200)).executes(ctx -> {
                ServerPlayer sp = ctx.getSource().getPlayerOrException();
                SimulationManager.setLevel(sp, IntegerArgumentType.getInteger(ctx, "lvl"));
                return 1;
            }))));
    }
}
