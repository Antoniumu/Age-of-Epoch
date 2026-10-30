package com.robertx22.age_of_exile.airship;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AirshipHeartTest {
    @Test void relationshipFillIsBoundedAndMonotonic() {
        int last=-1;for(int trust=0;trust<=100;trust++){int r=AirshipHeart.filledRows(trust);assertTrue(r>=last && r<=7);last=r;}
        assertEquals(0,AirshipHeart.filledRows(-1));assertEquals(7,AirshipHeart.filledRows(101));
    }
    @org.junit.jupiter.api.Test void bundledArmModelsMatchTheSourceAtlases(){
        for(String id:java.util.List.of("mara","veya","selin"))org.junit.jupiter.api.Assertions.assertEquals("slim",AirshipCrew.bundledModel(id));
        for(String id:java.util.List.of("captain","quartermaster","oren","lysander","tik"))org.junit.jupiter.api.Assertions.assertEquals("wide",AirshipCrew.bundledModel(id));
    }
}
