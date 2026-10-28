package com.robertx22.age_of_exile.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.AbstractGui;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

/** Rotated 3D cube and enlarged outer skin layer, projected into GUI texture quads. */
public final class AirshipPortrait {
    private AirshipPortrait() {}
    public static void draw(PoseStack ignored,int x,int y,int size,String texture,int mouseX,int mouseY) {
        var graphics=AbstractGui.currentGraphics();
        if(graphics==null) return;
        Identifier skin;
        try { var id=Identifier.parse(texture);skin=Identifier.fromNamespaceAndPath(id.getNamespace(),"textures/"+id.getPath()+".png"); }
        catch(Exception e) { skin=Identifier.fromNamespaceAndPath("mmorpg","textures/entity/airship/captain.png"); }
        double yaw=HeadCube.cursorYaw(mouseX-x-size/2.0);
        double pitch=HeadCube.cursorPitch(mouseY-y-size/2.0);
        AbstractGui.bindTexture(skin);
        for(boolean hat:new boolean[]{false,true}) for(var face:HeadCube.visible(yaw,pitch,size*.72,hat)) {
            var a=face.origin();var b=face.right();var c=face.down();
            Matrix3x2f projection=new Matrix3x2f((float)((b.x()-a.x())/8),(float)((b.y()-a.y())/8),
                (float)((c.x()-a.x())/8),(float)((c.y()-a.y())/8),(float)(x+size/2.0+a.x()),(float)(y+size/2.0+a.y()));
            graphics.pose().pushMatrix();
            graphics.pose().mul(projection);
            AbstractGui.blit(new PoseStack(),0,0,8,8,(float)face.u(),(float)face.v(),8,8,64,64);
            graphics.pose().popMatrix();
        }
    }
}
