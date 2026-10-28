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
        var book=Load.playerRPGData(mc.player).questBook;int iron=0;
        for(int i=0;i<mc.player.getInventory().getContainerSize();i++)if(mc.player.getInventory().getItem(i).is(net.minecraft.world.item.Items.IRON_INGOT))iron+=mc.player.getInventory().getItem(i).getCount();
        var sides=AirshipQuestTracker.active(d,book,iron);
        var lines=mc.font.split(Component.literal(AirshipQuestTracker.main(d,book)),w-62);
        int hearts=Math.max(78,35+lines.size()*10+4),bottom=hearts+17+sides.size()*22;
        AbstractGui.fill(pose,x,y,x+w,y+bottom,0xDF1A1A1A);
        AbstractGui.fill(pose,x,y,x+w,y+1,0xFF888888);
        AirshipPortrait.draw(pose,x+7,y+9,44,d.giverSkin,mx,my);
        AbstractGui.drawShadow(mc.font,pose,"§6"+AirshipStory.chapter(d.stage),x+53,y+8,0xFFFFFF);
        AbstractGui.drawShadow(mc.font,pose,AirshipStory.name(AirshipStory.owner(d.stage)).split(" • ")[0],x+53,y+21,0xB8C6CA);
        int lineY=y+35;
        for(var line:mc.font.split(Component.literal(AirshipQuestTracker.main(d,book)),w-62)) {
            AbstractGui.drawShadow(mc.font,pose,line,x+53,lineY,0xEEE5D5); lineY+=10;
        }
        String[] ids={"captain","mara","quartermaster"};
        for(int i=0;i<ids.length;i++) {
            AirshipHeart.draw(pose,x+7+i*73,y+hearts,1,d.relationshipValue(ids[i]));
            AbstractGui.drawShadow(mc.font,pose,new String[]{"Кап.","Мара","Брам"}[i]+" "+d.relationshipValue(ids[i]),x+18+i*73,y+hearts,0xA8BDA9);
        }
        int sideY=y+hearts+15;
        for(String side:sides){int n=0;for(var line:mc.font.split(Component.literal(side),w-14)){if(n++>=2)break;AbstractGui.drawShadow(mc.font,pose,line,x+7,sideY,0x80D9D1);sideY+=10;}sideY+=2;}
    }
}
