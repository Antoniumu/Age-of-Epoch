package com.robertx22.age_of_exile.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.network.chat.Component;

/** In-mod dialogue UI: world stays visible, brass rules, portrait, explicit quest acceptance. */
public class AirshipDialogueScreen extends BaseScreen {
    private final int entityId;
    private final String npc;
    private final String texture;
    public AirshipDialogueScreen(int entityId,String npc,String texture) { super(350,204); this.entityId=entityId; this.npc=npc; this.texture=texture; }
    private int stage() { return mc.player==null?0:Load.playerRPGData(mc.player).airship.stage; }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() {
        super.init();
        int next=AirshipStory.advance(npc,stage());
        if(next!=stage()) addButton(new Choice(guiLeft+14,guiTop+153,322,
            stage()==0?"Принять поручение":stage()==3?"Доложить о готовности":"Продолжить поручение",b->{
                Packets.sendToServer(new AirshipAcceptPacket(entityId)); mc.gui.setScreen(null);
            }));
        addButton(new Choice(guiLeft+14,guiTop+177,322,"Мне пора. До встречи.",b->mc.gui.setScreen(null)));
    }
    @Override public void render(PoseStack pose,int mx,int my,float partial) {
        int x=guiLeft,y=guiTop;
        AbstractGui.fill(pose,x-2,y-2,x+352,y+206,0xD00A1219);
        AbstractGui.fill(pose,x,y,x+350,y+204,0xE01A1A1A);
        AbstractGui.fill(pose,x,y,x+350,y+2,0xFF888888);
        AbstractGui.fill(pose,x,y+202,x+350,y+204,0xFF555555);
        AirshipPortrait.draw(pose,x+14,y+15,52,texture,mx,my);
        text(pose,"ПОСЛЕДНИЙ РАССВЕТ",x+78,y+15,0xDDDDDD);
        text(pose,AirshipStory.name(npc),x+78,y+30,0xF0E6D4);
        text(pose,"Команда дирижабля • Глава I",x+78,y+45,0x8AA8B5);
        AbstractGui.fill(pose,x+14,y+78,x+336,y+79,0xFF58605A);
        int lineY=y+89;
        for(var line:mc.font.split(Component.literal(AirshipStory.dialogue(npc,stage())),320)) {
            AbstractGui.drawShadow(mc.font,pose,line,x+15,lineY,0xEEE5D5); lineY+=10;
        }
        super.render(pose,mx,my,partial);
    }
    private void text(PoseStack p,String s,int x,int y,int color) { AbstractGui.drawShadow(mc.font,p,s,x,y,color); }
    private static class Choice extends Button {
        Choice(int x,int y,int w,String label,net.minecraft.client.gui.components.Button.OnPress press) {
            super(x,y,w,20,Component.literal(label),press);
        }
        @Override public void renderButton(PoseStack p,int mx,int my,float delta) {
            AbstractGui.fill(p,x,y,x+width,y+height,isHovered()?0xFF505050:0xFF303030);
            AbstractGui.fill(p,x,y,x+2,y+height,0xFF888888);
            AbstractGui.drawShadow(net.minecraft.client.Minecraft.getInstance().font,p,getMessage(),x+10,y+6,0xF0E6D4);
        }
    }
}
