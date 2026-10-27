package com.robertx22.age_of_exile.airship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Actual 3D cube geometry, orthographically projected for the GUI. No Minecraft dependency. */
public final class HeadCube {
    private HeadCube() {}
    public record Vec(double x,double y,double z) {}
    public record Face(Vec origin,Vec right,Vec down,int u,int v,double depth) {}
    private static Vec turn(double x,double y,double z,double yaw,double pitch) {
        double xx=Math.cos(yaw)*x+Math.sin(yaw)*z,zz=-Math.sin(yaw)*x+Math.cos(yaw)*z;
        return new Vec(xx,Math.cos(pitch)*y-Math.sin(pitch)*zz,Math.sin(pitch)*y+Math.cos(pitch)*zz);
    }
    public static List<Face> visible(double yaw,double pitch,double scale,boolean hat) {
        // origin, upper right, lower left, outward normal, UV.
        double[][] faces={
            {-.5,-.5,-.5, .5,-.5,-.5, -.5,.5,-.5, 0,0,-1, 8,8},
            {.5,-.5,.5, -.5,-.5,.5, .5,.5,.5, 0,0,1, 24,8},
            {-.5,-.5,.5, -.5,-.5,-.5, -.5,.5,.5, -1,0,0, 0,8},
            {.5,-.5,-.5, .5,-.5,.5, .5,.5,-.5, 1,0,0, 16,8},
            {-.5,-.5,.5, .5,-.5,.5, -.5,-.5,-.5, 0,-1,0, 8,0},
            {-.5,.5,-.5, .5,.5,-.5, -.5,.5,.5, 0,1,0, 16,0}};
        List<Face> out=new ArrayList<>();
        double amount=scale*(hat?1.10:1.0);
        for(double[] f:faces) {
            Vec normal=turn(f[9],f[10],f[11],yaw,pitch);
            if(normal.z()>=-0.00001) continue;
            Vec a=turn(f[0]*amount,f[1]*amount,f[2]*amount,yaw,pitch);
            Vec b=turn(f[3]*amount,f[4]*amount,f[5]*amount,yaw,pitch);
            Vec c=turn(f[6]*amount,f[7]*amount,f[8]*amount,yaw,pitch);
            out.add(new Face(a,b,c,(int)f[12]+(hat?32:0),(int)f[13],(b.z()+c.z())/2));
        }
        out.sort(Comparator.comparingDouble(Face::depth).reversed());
        return out;
    }
}
