package com.robertx22.age_of_exile.airship;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirshipConversationLeaseTest {
    @Test void concurrentListenersHaveStableNearestAndUuidOrdering(){
        var low=java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        var high=java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertTrue(AirshipConversationLease.preferred(25,high,Double.POSITIVE_INFINITY,null));
        assertTrue(AirshipConversationLease.preferred(4,high,9,low));
        assertFalse(AirshipConversationLease.preferred(9,low,4,high));
        assertTrue(AirshipConversationLease.preferred(4,low,4,high));
        assertFalse(AirshipConversationLease.preferred(4,high,4,low));
    }
    @Test void expirationIsExclusiveAndDoesNotDependOnWallClock(){
        assertTrue(AirshipConversationLease.unexpired(100,180));
        assertTrue(AirshipConversationLease.unexpired(179,180));
        assertFalse(AirshipConversationLease.unexpired(180,180));
        assertFalse(AirshipConversationLease.unexpired(181,180));
    }
}
