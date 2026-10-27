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
        AbstractGui.fill(pose,x,y,x+w,y+73,0xEF15232C);
        AbstractGui.fill(pose,x,y,x+w,y+1,0xFFD1AE66);
        AirshipPortrait.draw(pose,x+7,y+9,38,AirshipStory.owner(d.stage),mx,my);
        AbstractGui.drawShadow(mc.font,pose,d.stage==4?"§aПринят на борт":"§6Нить Рассвета",x+53,y+8,0xFFFFFF);
        AbstractGui.drawShadow(mc.font,pose,AirshipStory.owner(d.stage).equals("mara")?"Мара":"Капитан",x+53,y+21,0xB8C6CA);
        int lineY=y+35;
        for(var line:mc.font.split(Component.literal(AirshipStory.objective(d.stage)),w-62)) {
            AbstractGui.drawShadow(mc.font,pose,line,x+53,lineY,0xEEE5D5); lineY+=10;
        }
    }
}
