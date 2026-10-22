package com.robertx22.age_of_exile.simulation;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class SimulationPacket extends MyPacket<SimulationPacket> {

    public String action = "";
    public int value = 0;

    public SimulationPacket() {}

    public SimulationPacket(String action, int value) {
        this.action = action;
        this.value = value;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath("mmorpg", "simulation_action");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        this.action = buf.readUtf();
        this.value = buf.readInt();
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeUtf(this.action == null ? "" : this.action);
        buf.writeInt(this.value);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (ctx.getPlayer() instanceof ServerPlayer sp) {
            switch (action) {
                case "enter" -> SimulationManager.enterSimulation(sp);
                case "exit" -> SimulationManager.exitSimulation(sp);
                case "level" -> SimulationManager.setLevel(sp, value);
                case "reset" -> SimulationManager.resetAllAllocations(sp);
                case "kit" -> SimulationManager.grantTestKit(sp);
                case "dummy" -> SimulationManager.spawnDummy(sp, value > 0);
                case "hit" -> SimulationManager.testIncomingHit(sp, value);
                case "heal" -> SimulationManager.healPlayerFull(sp);
            }
        }
    }

    @Override
    public MyPacket<SimulationPacket> newInstance() {
        return new SimulationPacket();
    }
}
