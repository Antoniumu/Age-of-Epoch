package com.robertx22.age_of_exile.airship;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HeadCubeTest {
    @Test void gazeFollowsCursorNotTheOppositeDirection() {
        for(double dx:new double[]{-100,100}) {
            double yaw=HeadCube.cursorYaw(dx);
            // Rotated front outward normal X = -sin(yaw).
            assertEquals(Math.signum(dx),Math.signum(-Math.sin(yaw)));
        }
        for(double dy:new double[]{-100,100})
            assertEquals(Math.signum(dy),Math.signum(Math.sin(HeadCube.cursorPitch(dy))));
        assertEquals(0,HeadCube.cursorYaw(0),.00001);assertEquals(0,HeadCube.cursorPitch(0),.00001);
        assertEquals(-.72,HeadCube.cursorYaw(10000));
    }
    @Test void frontViewHasOneRealFace() {
        var faces=HeadCube.visible(0,0,40,false);
        assertEquals(1,faces.size());assertEquals(8,faces.getFirst().u());
        assertEquals(40,faces.getFirst().right().x()-faces.getFirst().origin().x(),.001);
    }
    @Test void yawAndPitchRevealThreeFacesWithRealDepth() {
        var faces=HeadCube.visible(.5,.3,40,false);
        assertEquals(3,faces.size());
        assertTrue(faces.stream().anyMatch(f->Math.abs(f.right().y()-f.origin().y())>.1));
        assertTrue(faces.stream().allMatch(f->Double.isFinite(f.depth())));
    }
    @Test void outerLayerIsAnEnlargedCubeNotAFlatIcon() {
        var a=HeadCube.visible(0,0,40,false).getFirst();var b=HeadCube.visible(0,0,40,true).getFirst();
        assertEquals(a.u()+32,b.u());assertTrue(b.origin().z()<a.origin().z());
    }
}
