package com.robertx22.age_of_exile.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.AbstractGui;
import net.minecraft.resources.Identifier;

/** Skin head (face + hat + visible side), gently oriented toward the cursor. No independent moving eyes. */
public final class AirshipPortrait {
    private AirshipPortrait() {}
    public static void draw(PoseStack pose,int x,int y,int size,String texture,int mouseX,int mouseY) {
        Identifier skin;
        try { Identifier id=Identifier.parse(texture); skin=Identifier.fromNamespaceAndPath(id.getNamespace(),"textures/"+id.getPath()+".png"); }
        catch(Exception e) { skin=Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png"); }
        int dx=Math.max(-2,Math.min(2,(mouseX-x-size/2)/50));
        int dy=Math.max(-2,Math.min(2,(mouseY-y-size/2)/60));
        int side=Math.abs(dx)*Math.max(1,size/12),front=size-side;
        int left=x+dx+(dx<0?side:0),top=y+dy;
        AbstractGui.bindTexture(skin);
        // Eight-pixel vanilla head UVs; the outer hat layer comes from the same skin.
        part(pose,left,top,front,size,8,8);
        part(pose,left,top,front,size,40,8);
        if(side>0) {
            int sx=dx<0?x+dx:left+front;
            part(pose,sx,top,side,size,dx<0?0:16,8);
            part(pose,sx,top,side,size,dx<0?32:48,8);
        }
    }
    private static void part(PoseStack p,int x,int y,int w,int h,int u,int v) {
        AbstractGui.blit(p,x,y,w,h,(float)u,(float)v,8,8,64,64);
    }
}
