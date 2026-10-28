package com.robertx22.age_of_exile.airship;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AirshipHeartTest {
    @Test void relationshipFillIsBoundedAndMonotonic() {
        int last=-1;for(int trust=0;trust<=100;trust++){int r=AirshipHeart.filledRows(trust);assertTrue(r>=last && r<=7);last=r;}
        assertEquals(0,AirshipHeart.filledRows(-1));assertEquals(7,AirshipHeart.filledRows(101));
    }
}
