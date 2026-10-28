package com.robertx22.age_of_exile.airship;
import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.network.chat.Component;
/** H opens a server quote, not an unchecked client-side spawn. */
public class AirshipHubScreen extends BaseScreen {
    private final String nonce,message;
    public AirshipHubScreen(String nonce,String message){super(380,208);this.nonce=nonce;this.message=message;}
    public boolean isPauseScreen(){return false;}
    protected void init(){
        super.init();int x=(width-360)/2,y=(height-208)/2;
        if(!nonce.isEmpty()) addButton(new Button(x+10,y+152,340,20,Component.literal(nonce.startsWith("jump:")?"Прыжок: 16 изумрудов + 8 эндер-жемчужин":"Подтвердить: 32 изумруда + 16 железа"),b->{Packets.sendToServer(new AirshipHubRequestPacket(1,nonce));onClose();}));
        addButton(new Button(x+10,y+178,340,20,Component.literal("Закрыть / отменить"),b->onClose()));
    }
    public void onClose(){Packets.sendToServer(new AirshipHubRequestPacket(2,nonce));mc.gui.setScreen(null);}
    public void render(PoseStack pose,int mx,int my,float delta){
        int x=(width-360)/2,y=(height-208)/2;AbstractGui.fill(pose,x,y,x+360,y+208,0xEE111B20);
        AbstractGui.drawShadow(mc.font,pose,"ПОСЛЕДНИЙ РАССВЕТ • ОБЩИЙ ХАБ",x+12,y+12,0xE4C897);
        int yy=y+34;for(var line:mc.font.split(Component.literal(message),336)){if(yy>y+140)break;AbstractGui.drawShadow(mc.font,pose,line,x+12,yy,0xE7E4DD);yy+=11;}
        super.render(pose,mx,my,delta);
    }
}
