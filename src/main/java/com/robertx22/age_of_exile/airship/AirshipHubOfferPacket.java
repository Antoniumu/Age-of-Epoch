package com.robertx22.age_of_exile.airship;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
public class AirshipHubOfferPacket extends MyPacket<AirshipHubOfferPacket> {
    private String nonce="",message="";
    public AirshipHubOfferPacket() {}
    public AirshipHubOfferPacket(String nonce,String message){this.nonce=nonce;this.message=message;}
    public Identifier getIdentifier(){return Identifier.fromNamespaceAndPath("mmorpg","airship_hub_offer");}
    public void loadFromData(FriendlyByteBuf b){nonce=b.readUtf(64);message=b.readUtf(4096);}
    public void saveToData(FriendlyByteBuf b){b.writeUtf(nonce,64);b.writeUtf(message,4096);}
    public void onReceived(ExilePacketContext c){AirshipClient.openHub(nonce,message);}
    public MyPacket<AirshipHubOfferPacket> newInstance(){return new AirshipHubOfferPacket();}
}
