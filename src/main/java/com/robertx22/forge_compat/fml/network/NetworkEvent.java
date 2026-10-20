package com.robertx22.forge_compat.fml.network;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ConcurrentLinkedQueue;

public class NetworkEvent extends Event {

    public static class Context {
        private final ServerPlayer sender;
        private final Connection connection;
        private final boolean clientSide;
        private final ConcurrentLinkedQueue<Runnable> workQueue = new ConcurrentLinkedQueue<>();
        private volatile boolean packetHandled = true;

        public Context(ServerPlayer sender, Connection connection, boolean clientSide) {
            this.sender = sender;
            this.connection = connection;
            this.clientSide = clientSide;
        }

        public ServerPlayer getSender() { return sender; }

        public Connection getNetworkManager() { return connection; }

        public void enqueueWork(Runnable runnable) {
            workQueue.add(runnable);
        }

        public void setPacketHandled(boolean handled) { this.packetHandled = handled; }

        public boolean isPacketHandled() { return packetHandled; }

        public void runWork() {
            Runnable r;
            while ((r = workQueue.poll()) != null) {
                r.run();
            }
        }

        public boolean isClientSide() { return clientSide; }
    }
}
