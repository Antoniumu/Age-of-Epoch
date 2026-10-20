package com.robertx22.library_of_exile.packets.defaults;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;

public class EntityPacket {

    /*
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Ref.MODID, "spawn_entity");

    public static Packet<?> createPacket(Entity entity) {
        FriendlyByteBuf buf = createBuffer();
        buf.writeVarInt(BuiltInRegistries.ENTITY_TYPE.getId(entity.getType()));
        buf.writeUUID(entity.getUUID());
        buf.writeVarInt(entity.getId());
        buf.writeDouble(entity.getX());
        buf.writeDouble(entity.getY());
        buf.writeDouble(entity.getZ());
        buf.writeByte(Mth.floor(entity.getXRot() * 256.0F / 360.0F));
        buf.writeByte(Mth.floor(entity.getYRot() * 256.0F / 360.0F));
        buf.writeFloat(entity.getXRot());
        buf.writeFloat(entity.getYRot());
        return ServerSidePacketRegistry.INSTANCE.toPacket(ID, buf);
    }

    private static FriendlyByteBuf createBuffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

*/

}

