import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
package com.robertx22.library_of_exile.packets.defaults;

public class EntityPacket {

    /*
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Ref.MODID, "spawn_entity");

    public static Packet<?> createPacket(Entity entity) {
        FriendlyByteBuf buf = createBuffer();
        buf.writeVarInt(BuiltInRegistries.ENTITY_TYPE.getId(entity.getType()));
        buf.writeUUID(entity.getUUID());
        buf.writeVarInt(entity.getId());
        buf.writeDouble(entity.getX());
        buf.writeDouble(entity.getY());
        buf.writeDouble(entity.getZ());
        buf.writeByte(Mth.floor(entity.xRot * 256.0F / 360.0F));
        buf.writeByte(Mth.floor(entity.yRot * 256.0F / 360.0F));
        buf.writeFloat(entity.xRot);
        buf.writeFloat(entity.yRot);
        return ServerSidePacketRegistry.INSTANCE.toPacket(ID, buf);
    }

    private static FriendlyByteBuf createBuffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

*/

}

