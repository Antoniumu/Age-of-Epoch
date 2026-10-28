package com.robertx22.age_of_exile.airship;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrewMotionTest {
    @Test void yawFacesActualPlayerDirection() {
        assertEquals(0,CrewMotion.yaw(0,1),.001);assertEquals(-90,CrewMotion.yaw(1,0),.001);
        assertEquals(90,CrewMotion.yaw(-1,0),.001);assertEquals(180,Math.abs(CrewMotion.yaw(0,-1)),.001);
    }
    @Test void shortestTurnCrossesSeamWithoutSpinning() {
        assertEquals(181,CrewMotion.approach(179,-170,2),.001);
        assertEquals(-181,CrewMotion.approach(-179,170,2),.001);
        assertEquals(5,CrewMotion.approach(4,5,6),.001);
    }
    @Test void pitchIsBoundedAndLooksUpAtTallerPlayer() {
        assertTrue(CrewMotion.pitch(1,3)<0);assertEquals(-35,CrewMotion.pitch(100,1),.001);
        assertEquals(35,CrewMotion.pitch(-100,1),.001);
    }
}
