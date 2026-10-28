package com.robertx22.age_of_exile.airship;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** C2S contains an entity reference, never trusted progress/rewards. */
public class AirshipAcceptPacket extends MyPacket<AirshipAcceptPacket> {
    private int entityId;
    private int stage;
    private int choice;
    public AirshipAcceptPacket() {}
    public AirshipAcceptPacket(int entityId,int stage,int choice) { this.entityId=entityId;this.stage=stage;this.choice=choice; }
    @Override public Identifier getIdentifier() { return Identifier.fromNamespaceAndPath("mmorpg","airship_accept"); }
    @Override public void loadFromData(FriendlyByteBuf b) { entityId=b.readVarInt();stage=b.readVarInt();choice=b.readVarInt(); }
    @Override public void saveToData(FriendlyByteBuf b) { b.writeVarInt(entityId);b.writeVarInt(stage);b.writeVarInt(choice); }
    @Override public void onReceived(ExilePacketContext c) {
        if(c.getPlayer() instanceof ServerPlayer p) AirshipService.accept(p,entityId,stage,choice);
    }
    @Override public MyPacket<AirshipAcceptPacket> newInstance() { return new AirshipAcceptPacket(); }
}
