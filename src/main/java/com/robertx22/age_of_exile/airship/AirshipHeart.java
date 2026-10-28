package com.robertx22.age_of_exile.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.AbstractGui;

/** Filled from bottom to top; a relationship indicator, not a romance flag. */
public final class AirshipHeart {
    private AirshipHeart() {}
    private static final String[] MASK={"01100110","11111111","11111111","11111111","01111110","00111100","00011000"};
    public static int filledRows(int trust) { return Math.clamp((int)Math.ceil(Math.clamp(trust,0,100)*7/100.0),0,7); }
    public static void draw(PoseStack pose,int x,int y,int scale,int trust) {
        int rows=filledRows(trust);
        for(int r=0;r<7;r++) for(int c=0;c<8;c++) if(MASK[r].charAt(c)=='1') {
            int color=r>=7-rows?0xFFE98298:0xFF49373F;
            AbstractGui.fill(pose,x+c*scale,y+r*scale,x+(c+1)*scale,y+(r+1)*scale,color);
        }
    }
}
