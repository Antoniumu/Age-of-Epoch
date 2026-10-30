package com.robertx22.age_of_exile.airship;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirshipConversationLeaseTest {
    @Test void expirationIsExclusiveAndDoesNotDependOnWallClock(){
        assertTrue(AirshipConversationLease.unexpired(100,180));
        assertTrue(AirshipConversationLease.unexpired(179,180));
        assertFalse(AirshipConversationLease.unexpired(180,180));
        assertFalse(AirshipConversationLease.unexpired(181,180));
    }
}
