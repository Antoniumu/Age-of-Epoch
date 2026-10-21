package com.robertx22.forge_compat.fml.network.simple;

import com.robertx22.forge_compat.fml.network.NetworkDirection;
import com.robertx22.forge_compat.fml.network.NetworkEvent;
import com.robertx22.forge_compat.fml.network.PacketDistributor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Forge SimpleChannel-shaped networking backed by Fabric Networking API v1.
 * Message payloads are registered in both directions with the same id.
 */
public class SimpleChannel {

    private final Identifier id;

    public static class MsgPayload implements CustomPacketPayload {
        private final CustomPacketPayload.Type<MsgPayload> type;
        private final byte[] data;

        public MsgPayload(CustomPacketPayload.Type<MsgPayload> type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        @Override
        public CustomPacketPayload.Type<MsgPayload> type() { return type; }

        public byte[] data() { return data; }
    }

    private static class Registration {
        final Class<?> clazz;
        final BiConsumer<Object, FriendlyByteBuf> encoder;
        final Function<FriendlyByteBuf, Object> decoder;
        final BiConsumer<Object, Supplier<NetworkEvent.Context>> handler;

        Registration(Class<?> clazz, BiConsumer<Object, FriendlyByteBuf> encoder,
                     Function<FriendlyByteBuf, Object> decoder,
                     BiConsumer<Object, Supplier<NetworkEvent.Context>> handler) {
            this.clazz = clazz;
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
        }
    }

    private final Map<Class<?>, Registration> byClass = new HashMap<>();
    private final Map<Identifier, Registration> byId = new HashMap<>();

    /** connection -> player tracking for sendTo(msg, connection, dir) */
    public static final Map<Connection, ServerPlayer> CONNECTION_PLAYERS = new WeakHashMap<>();

    public SimpleChannel(Identifier id) {
        this.id = id;
    }

    public Identifier getId() { return id; }

    @SuppressWarnings("unchecked")
    public <T> void registerMessage(int index, Class<T> messageType,
                                    BiConsumer<T, FriendlyByteBuf> encoder,
                                    Function<FriendlyByteBuf, T> decoder,
                                    BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
        Identifier msgId = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/" + index);
        if (byId.containsKey(msgId)) {
            return;
        }
        Registration reg = new Registration(messageType,
            (BiConsumer<Object, FriendlyByteBuf>) (BiConsumer<?, ?>) encoder,
            (Function<FriendlyByteBuf, Object>) decoder,
            (BiConsumer<Object, Supplier<NetworkEvent.Context>>) (BiConsumer<?, ?>) handler);
        byClass.put(messageType, reg);
        byId.put(msgId, reg);

        CustomPacketPayload.Type<MsgPayload> payloadType = new CustomPacketPayload.Type<>(msgId);
        try {
            PayloadTypeRegistry.clientboundPlay().register(payloadType, codecFor(payloadType, reg));
        } catch (Throwable ignored) {
        }
        try {
            PayloadTypeRegistry.serverboundPlay().register(payloadType, codecFor(payloadType, reg));
        } catch (Throwable ignored) {
        }

        try {
            ServerPlayNetworking.registerGlobalReceiver(payloadType, (payload, context) -> {
                System.out.println("[AOE-PACKET] ServerPlayNetworking received payload: id=" + msgId + " class=" + reg.clazz.getSimpleName() + " bytes=" + payload.data().length);
                FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(payload.data()));
                Object msg = reg.decoder.apply(buf);
                NetworkEvent.Context ctx = new NetworkEvent.Context(context.player(), null, false);
                reg.handler.accept(msg, () -> ctx);
                ctx.runWork();
            });
        } catch (Throwable t) {
            System.out.println("[AOE-PACKET] Failed to register ServerPlayNetworking receiver for " + msgId + ": " + t);
        }
        try {
            ClientPlayNetworking.registerGlobalReceiver(payloadType, (payload, context) -> {
                FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(payload.data()));
                Object msg = reg.decoder.apply(buf);
                NetworkEvent.Context ctx = new NetworkEvent.Context(null, null, true);
                reg.handler.accept(msg, () -> ctx);
                ctx.runWork();
            });
        } catch (Throwable ignored) {
        }
    }

    private static net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, MsgPayload> codecFor(
        CustomPacketPayload.Type<MsgPayload> type, Registration reg) {
        return net.minecraft.network.codec.StreamCodec.of(
            (buf, payload) -> buf.writeBytes(payload.data()),
            (buf) -> {
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                return new MsgPayload(type, data);
            });
    }

    private static byte[] encode(Registration reg, Object msg) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        reg.encoder.accept(msg, buf);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    private static void sendToPlayer(ServerPlayer player, Identifier msgId, Registration reg, Object msg) {
        ServerPlayNetworking.send(player, new MsgPayload(new CustomPacketPayload.Type<>(msgId), encode(reg, msg)));
    }

    public void sendToPlayer(Object msg, ServerPlayer player) {
        Registration reg = byClass.get(msg.getClass());
        if (reg == null || player == null) return;
        sendToPlayer(player, idOf(reg), reg, msg);
    }

    public void sendToPlayer(ServerPlayer player, Object msg) {
        sendToPlayer(msg, player);
    }

    public void sendTo(Object msg, Connection connection, NetworkDirection direction) {
        Registration reg = byClass.get(msg.getClass());
        if (reg == null) return;
        Identifier msgId = idOf(reg);
        if (direction == NetworkDirection.PLAY_TO_CLIENT) {
            ServerPlayer player = CONNECTION_PLAYERS.get(connection);
            if (player != null) sendToPlayer(player, msgId, reg, msg);
        } else {
            ClientPlayNetworking.send(new MsgPayload(new CustomPacketPayload.Type<>(msgId), encode(reg, msg)));
        }
    }

    public void sendToServer(Object msg) {
        Registration reg = byClass.get(msg.getClass());
        if (reg == null) {
            System.out.println("[AOE-PACKET] sendToServer FAILED: no registration for " + msg.getClass().getName() + " in channel " + this.id);
            return;
        }
        Identifier msgId = idOf(reg);
        byte[] bytes = encode(reg, msg);
        System.out.println("[AOE-PACKET] sendToServer: id=" + msgId + " class=" + msg.getClass().getSimpleName() + " bytes=" + bytes.length);
        ClientPlayNetworking.send(new MsgPayload(new CustomPacketPayload.Type<>(msgId), bytes));
    }

    @SuppressWarnings("unchecked")
    public <T> void send(PacketDistributor.Directed<T> target, Object msg) {
        Registration reg = byClass.get(msg.getClass());
        if (reg == null) return;
        Identifier msgId = idOf(reg);
        PacketDistributor<T> distributor = target.getDistributor();
        T obj = target.getObject();
        if (obj == null) return;
        switch (distributor.getTarget()) {
            case TRACKING_ENTITY: {
                for (ServerPlayer p : PlayerLookup.tracking((Entity) obj)) {
                    sendToPlayer(p, msgId, reg, msg);
                }
                if (obj instanceof ServerPlayer sp) {
                    sendToPlayer(sp, msgId, reg, msg);
                }
                break;
            }
            case TRACKING_CHUNK: {
                LevelChunk chunk = (LevelChunk) obj;
                if (chunk.getLevel() instanceof ServerLevel sl) {
                    for (ServerPlayer p : PlayerLookup.tracking(sl, chunk.getPos())) {
                        sendToPlayer(p, msgId, reg, msg);
                    }
                }
                break;
            }
            case ALL: {
                MinecraftServerHolder.sendAll(msgId, reg, msg);
                break;
            }
            default:
                break;
        }
    }

    private Identifier idOf(Registration reg) {
        for (Map.Entry<Identifier, Registration> e : byId.entrySet()) {
            if (e.getValue() == reg) return e.getKey();
        }
        return id;
    }

    /** helper holder to avoid importing MinecraftServer statically in multiple places */
    private static class MinecraftServerHolder {
        static void sendAll(Identifier msgId, Registration reg, Object msg) {
            net.minecraft.server.MinecraftServer server = com.robertx22.forge_compat.fml.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                sendToPlayer(p, msgId, reg, msg);
            }
        }
    }
}
