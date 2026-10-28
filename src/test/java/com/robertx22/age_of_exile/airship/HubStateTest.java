package com.robertx22.age_of_exile.airship;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HubStateTest {
    @Test void sharedCoordinatesNeverCopyAnotherPlayersStory() {
        HubState h=new HubState();h.exists=true;h.x=20;h.y=100;h.z=30;h.groundY=64;
        var a=new AirshipData();a.stage=1;a.captainTrust=7;a.storyDecision=1;
        var b=new AirshipData();b.stage=9;b.captainTrust=62;b.storyDecision=2;b.favorCompleted=5;
        assertTrue(h.mirror(a));assertTrue(h.mirror(b));
        assertEquals(a.x,b.x);assertEquals(1,a.stage);assertEquals(9,b.stage);
        assertEquals(7,a.captainTrust);assertEquals(62,b.captainTrust);assertEquals(2,b.storyDecision);assertEquals(5,b.favorCompleted);
        assertFalse(h.mirror(a));
    }
    @Test void protectedHullAndLiftHaveFiniteBounds() {
        HubState h=new HubState();h.exists=true;h.x=100;h.y=130;h.z=200;h.groundY=65;
        assertTrue(h.contains(100,130,200));assertTrue(h.contains(100,65,204));
        assertFalse(h.contains(100,64,204-10));assertFalse(h.contains(1000,130,200));
        h.exists=false;assertFalse(h.contains(100,130,200));
    }
    @Test void invalidSaveDoesNotPassValidation() {
        HubState h=new HubState();assertTrue(h.valid());h.schemaVersion=88;assertFalse(h.valid());
        h.schemaVersion=1;h.layout=null;assertFalse(h.valid());
    }
}
