package com.robertx22.age_of_exile.simulation;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class SimulationPacket extends MyPacket<SimulationPacket> {

    public enum Action {
        TOGGLE_SIMULATION,
        ENTER_SIMULATION,
        EXIT_SIMULATION,
        SET_LEVEL,
        GRANT_POINTS,
        RESET_BUILD,
        GIVE_GEAR_KIT,
        SPAWN_DUMMY,
        SPAWN_TEST_MOB,
        CLEAR_MOBS,
        HEAL_FULL,
        TEST_DEFENSE_BURST,
        PRINT_MOB_SCALING_TABLE,
        RUN_FULL_AUDIT,
        /** собрать активный гайд на Полигоне (param1: выдать экипировку) */
        GUIDE_BUILD,
        /** сброс билда и сразу сборка активного гайда */
        RESET_AND_GUIDE
    }

    public Action action = Action.TOGGLE_SIMULATION;
    public int param1 = 0;
    public int param2 = 0;

    public SimulationPacket() {
    }

    public SimulationPacket(Action action) {
        this(action, 0, 0);
    }

    public SimulationPacket(Action action, int param1) {
        this(action, param1, 0);
    }

    public SimulationPacket(Action action, int param1, int param2) {
        this.action = action;
        this.param1 = param1;
        this.param2 = param2;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "simulation_action");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.param1 = buf.readInt();
        this.param2 = buf.readInt();
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        buf.writeInt(this.param1);
        buf.writeInt(this.param2);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        switch (this.action) {
            case TOGGLE_SIMULATION -> SimulationManager.toggleSimulation(serverPlayer);
            case ENTER_SIMULATION -> SimulationManager.enterSimulation(serverPlayer);
            case EXIT_SIMULATION -> SimulationManager.exitSimulation(serverPlayer);
            case SET_LEVEL -> SimulationManager.setSimulationLevel(serverPlayer, this.param1);
            case GRANT_POINTS -> SimulationManager.grantBonusPoints(serverPlayer);
            case RESET_BUILD -> SimulationManager.resetBuildInSimulation(serverPlayer);
            case GIVE_GEAR_KIT -> SimulationManager.giveGearKit(serverPlayer, this.param1);
            case SPAWN_DUMMY -> SimulationManager.spawnTargetDummy(serverPlayer, this.param1);
            case SPAWN_TEST_MOB -> SimulationManager.spawnTestMob(serverPlayer, this.param1, this.param2);
            case CLEAR_MOBS -> SimulationManager.clearSimulationMobs(serverPlayer, true);
            case HEAL_FULL -> {
                SimulationManager.healPlayerFull(serverPlayer);
                SimulationManager.syncAll(serverPlayer);
            }
            case TEST_DEFENSE_BURST -> SimulationManager.runDefenseStressTest(serverPlayer, this.param1, this.param2);
            case PRINT_MOB_SCALING_TABLE -> SimulationManager.printMobScalingTable(serverPlayer);
            case RUN_FULL_AUDIT -> SimulationManager.runFullAudit(serverPlayer);
            case GUIDE_BUILD -> SimulationManager.buildActiveGuide(serverPlayer, true, this.param1 > 0);
            case RESET_AND_GUIDE -> SimulationManager.resetBuildAndApplyGuide(serverPlayer);
        }
    }

    @Override
    public MyPacket<SimulationPacket> newInstance() {
        return new SimulationPacket();
    }
}
