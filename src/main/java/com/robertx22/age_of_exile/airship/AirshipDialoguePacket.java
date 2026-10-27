package com.robertx22.age_of_exile.airship;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

/** S2C: only the server can open a conversation. Client rendering lives behind a proxy. */
public class AirshipDialoguePacket extends MyPacket<AirshipDialoguePacket> {
    private int entityId;
    private String npc = "captain";
    public AirshipDialoguePacket() {}
    public AirshipDialoguePacket(int entityId,String npc) { this.entityId=entityId; this.npc=npc; }
    @Override public Identifier getIdentifier() { return Identifier.fromNamespaceAndPath("mmorpg","airship_dialogue"); }
    @Override public void loadFromData(FriendlyByteBuf b) { entityId=b.readVarInt(); npc=b.readUtf(32); }
    @Override public void saveToData(FriendlyByteBuf b) { b.writeVarInt(entityId); b.writeUtf(npc,32); }
    @Override public void onReceived(ExilePacketContext c) { AirshipClient.open(entityId,npc); }
    @Override public MyPacket<AirshipDialoguePacket> newInstance() { return new AirshipDialoguePacket(); }
}
