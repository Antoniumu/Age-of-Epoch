package com.robertx22.age_of_exile.airship;
import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.forge_compat.client.AbstractGui;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Compact H-screen quest giver card; read-only mirror of server progress. */
public final class AirshipQuestCard {
    private AirshipQuestCard() {}
    public static void render(PoseStack pose,int screenWidth,int screenHeight,int mx,int my) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null) return;
        AirshipData d=Load.playerRPGData(mc.player).airship;
        if(d.stage==0) return;
        int w=Math.min(230,screenWidth-12),x=screenWidth-w-6,y=6;
        AbstractGui.fill(pose,x,y,x+w,y+91,0xDF1A1A1A);
        AbstractGui.fill(pose,x,y,x+w,y+1,0xFF888888);
        AirshipPortrait.draw(pose,x+7,y+9,44,d.giverSkin,mx,my);
        AbstractGui.drawShadow(mc.font,pose,"§6"+AirshipStory.chapter(d.stage),x+53,y+8,0xFFFFFF);
        AbstractGui.drawShadow(mc.font,pose,AirshipStory.name(AirshipStory.owner(d.stage)).split(" • ")[0],x+53,y+21,0xB8C6CA);
        int lineY=y+35;
        for(var line:mc.font.split(Component.literal(AirshipStory.objective(d.stage)),w-62)) {
            AbstractGui.drawShadow(mc.font,pose,line,x+53,lineY,0xEEE5D5); lineY+=10;
        }
        String trust="Капитан "+d.captainTrust+"  Мара "+d.maraTrust+"  Брам "+d.bramTrust;
        AbstractGui.drawShadow(mc.font,pose,trust,x+7,y+78,0xA8BDA9);
    }
}
