package com.robertx22.age_of_exile.airship;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
public class AirshipHubRequestPacket extends MyPacket<AirshipHubRequestPacket> {
    private int action;private String nonce="";
    public AirshipHubRequestPacket() {}
    public AirshipHubRequestPacket(int action,String nonce) {this.action=action;this.nonce=nonce;}
    public Identifier getIdentifier(){return Identifier.fromNamespaceAndPath("mmorpg","airship_hub_request");}
    public void loadFromData(FriendlyByteBuf b){action=b.readVarInt();nonce=b.readUtf(64);}
    public void saveToData(FriendlyByteBuf b){b.writeVarInt(action);b.writeUtf(nonce,64);}
    public void onReceived(ExilePacketContext c){if(c.getPlayer() instanceof ServerPlayer p)AirshipHubRitual.request(p,action,nonce);}
    public MyPacket<AirshipHubRequestPacket> newInstance(){return new AirshipHubRequestPacket();}
}
