package com.robertx22.age_of_exile.simulation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.robertx22.age_of_exile.vanilla_mc.commands.CommandRefs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class SimCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildSimNode("sim"));
        dispatcher.register(literal(CommandRefs.ID).then(buildSimNode("sim")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildSimNode(String rootName) {
        return literal(rootName)
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                SimulationManager.toggleSimulation(p);
                return 1;
            })
            .then(literal("enter").executes(ctx -> {
                SimulationManager.enterSimulation(ctx.getSource().getPlayerOrException());
                return 1;
            }))
            .then(literal("exit").executes(ctx -> {
                SimulationManager.exitSimulation(ctx.getSource().getPlayerOrException());
                return 1;
            }))
            .then(literal("level")
                .then(argument("lvl", IntegerArgumentType.integer(1, 100)).executes(ctx -> {
                    int lvl = IntegerArgumentType.getInteger(ctx, "lvl");
                    SimulationManager.setSimulationLevel(ctx.getSource().getPlayerOrException(), lvl);
                    return 1;
                })))
            .then(literal("points").executes(ctx -> {
                SimulationManager.grantBonusPoints(ctx.getSource().getPlayerOrException());
                return 1;
            }))
            .then(literal("reset").executes(ctx -> {
                SimulationManager.resetBuildInSimulation(ctx.getSource().getPlayerOrException());
                return 1;
            }))
            .then(literal("kit")
                .then(literal("warrior").executes(ctx -> {
                    SimulationManager.giveGearKit(ctx.getSource().getPlayerOrException(), 0);
                    return 1;
                }))
                .then(literal("hunter").executes(ctx -> {
                    SimulationManager.giveGearKit(ctx.getSource().getPlayerOrException(), 1);
                    return 1;
                }))
                .then(literal("mage").executes(ctx -> {
                    SimulationManager.giveGearKit(ctx.getSource().getPlayerOrException(), 2);
                    return 1;
                }))
                .then(literal("trinkets").executes(ctx -> {
                    SimulationManager.giveGearKit(ctx.getSource().getPlayerOrException(), 3);
                    return 1;
                })))
            .then(literal("dummy")
                .executes(ctx -> {
                    SimulationManager.spawnTargetDummy(ctx.getSource().getPlayerOrException(), 0);
                    return 1;
                })
                .then(argument("lvl", IntegerArgumentType.integer(1, 100)).executes(ctx -> {
                    int lvl = IntegerArgumentType.getInteger(ctx, "lvl");
                    SimulationManager.spawnTargetDummy(ctx.getSource().getPlayerOrException(), lvl);
                    return 1;
                })))
            .then(literal("mob")
                .executes(ctx -> {
                    SimulationManager.spawnTestMob(ctx.getSource().getPlayerOrException(), 0, 2);
                    return 1;
                })
                .then(argument("lvl", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> {
                        int lvl = IntegerArgumentType.getInteger(ctx, "lvl");
                        SimulationManager.spawnTestMob(ctx.getSource().getPlayerOrException(), lvl, 2);
                        return 1;
                    })
                    .then(argument("rarity", IntegerArgumentType.integer(0, 5)).executes(ctx -> {
                        int lvl = IntegerArgumentType.getInteger(ctx, "lvl");
                        int rar = IntegerArgumentType.getInteger(ctx, "rarity");
                        SimulationManager.spawnTestMob(ctx.getSource().getPlayerOrException(), lvl, rar);
                        return 1;
                    }))))
            .then(literal("clear").executes(ctx -> {
                SimulationManager.clearSimulationMobs(ctx.getSource().getPlayerOrException(), true);
                return 1;
            }))
            .then(literal("defense")
                .executes(ctx -> {
                    SimulationManager.runDefenseStressTest(ctx.getSource().getPlayerOrException(), 0, 0);
                    return 1;
                })
                .then(literal("melee").executes(ctx -> {
                    SimulationManager.runDefenseStressTest(ctx.getSource().getPlayerOrException(), 0, 0);
                    return 1;
                }))
                .then(literal("ranged").executes(ctx -> {
                    SimulationManager.runDefenseStressTest(ctx.getSource().getPlayerOrException(), 0, 1);
                    return 1;
                }))
                .then(literal("spell").executes(ctx -> {
                    SimulationManager.runDefenseStressTest(ctx.getSource().getPlayerOrException(), 0, 2);
                    return 1;
                })))
            .then(literal("scaling").executes(ctx -> {
                SimulationManager.printMobScalingTable(ctx.getSource().getPlayerOrException());
                return 1;
            }))
            .then(literal("audit").executes(ctx -> {
                SimulationManager.runFullAudit(ctx.getSource().getPlayerOrException());
                return 1;
            }))
            .then(literal("guide").executes(ctx -> {
                SimulationManager.buildActiveGuide(ctx.getSource().getPlayerOrException(), true, true);
                return 1;
            }).then(argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String id = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id");
                    com.robertx22.age_of_exile.guides.GuideDefinition guide =
                        com.robertx22.age_of_exile.guides.GuideData.byId(id);
                    if (guide == null) {
                        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§c[Гайд] Неизвестный гайд: " + id));
                        return 0;
                    }
                    com.robertx22.age_of_exile.guides.GuideAdvisor.setActiveGuide(p, guide.id);
                    SimulationManager.buildActiveGuide(p, true, true);
                    return 1;
                })));
    }
}
