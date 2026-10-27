package com.robertx22.age_of_exile.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.AbstractGui;

/** Original pixel portraits; the head and pupils follow the cursor, not a world companion. */
public final class AirshipPortrait {
    private AirshipPortrait() {}
    public static void draw(PoseStack pose,int x,int y,int size,String npc,int mouseX,int mouseY) {
        int dx=Math.max(-2,Math.min(2,(mouseX-x-size/2)/45));
        int dy=Math.max(-1,Math.min(1,(mouseY-y-size/2)/50));
        pose.pushPose();
        pose.translate(x,y,0);
        pose.scale(size/32F,size/32F,1);
        rect(pose,0,0,32,32,0xFF101C26);
        rect(pose,1,1,31,31,0xFF263A43);
        rect(pose,7+dx,6+dy,25+dx,28+dy,0xFFB88764);
        rect(pose,9+dx,8+dy,23+dx,25+dy,0xFFD2A078);
        rect(pose,9+dx,14+dy,15+dx,17+dy,0xFFE3D7C0);
        rect(pose,18+dx,14+dy,23+dx,17+dy,0xFFE3D7C0);
        rect(pose,12+dx,14+dy,14+dx,17+dy,0xFF20323A);
        rect(pose,19+dx,14+dy,21+dx,17+dy,0xFF20323A);
        rect(pose,15+dx,17+dy,18+dx,22+dy,0xFF9F6A50);
        if(npc.equals("captain")) {
            rect(pose,6+dx,4+dy,26+dx,11+dy,0xFF192C37);
            rect(pose,5+dx,10+dy,27+dx,12+dy,0xFFD6AF5E);
            rect(pose,9+dx,23+dy,23+dx,28+dy,0xFFB8B6AC);
            rect(pose,14+dx,24+dy,20+dx,26+dy,0xFF6B4B3C);
        } else if(npc.equals("mara")) {
            rect(pose,6+dx,5+dy,26+dx,10+dy,0xFF78442F);
            rect(pose,6+dx,10+dy,9+dx,29+dy,0xFF78442F);
            rect(pose,23+dx,10+dy,26+dx,29+dy,0xFF78442F);
            rect(pose,12+dx,25+dy,20+dx,26+dy,0xFF985D50);
        } else {
            rect(pose,6+dx,5+dy,26+dx,11+dy,0xFF665043);
            rect(pose,8+dx,22+dy,24+dx,29+dy,0xFF72523D);
            rect(pose,14+dx,23+dy,19+dx,25+dy,0xFFBD8B66);
        }
        rect(pose,1,0,31,1,0xFFD6AF5E);
        rect(pose,1,31,31,32,0xFF8E7447);
        pose.popPose();
    }
    private static void rect(PoseStack p,int x,int y,int x2,int y2,int c) { AbstractGui.fill(p,x,y,x2,y2,c); }
}
