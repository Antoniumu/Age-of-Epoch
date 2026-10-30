package com.robertx22.age_of_exile.airship;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirshipStoryTest {
    @Test void campaignRequiresEveryCrewMemberInOrder() {
        int stage=0;
        stage=AirshipStory.advance("captain",stage); assertEquals(1,stage);
        assertEquals(1,AirshipStory.advance("captain",stage));
        assertEquals(1,AirshipStory.advance("quartermaster",stage));
        stage=AirshipStory.advance("mara",stage); assertEquals(2,stage);
        stage=AirshipStory.advance("quartermaster",stage); assertEquals(3,stage);
        stage=AirshipStory.advance("captain",stage); assertEquals(4,stage);
        for(;stage<AirshipStory.LAST_STAGE;) stage=AirshipStory.advance(AirshipStory.actor(stage),stage);
        for(String npc:AirshipStory.IDS) assertEquals(11,AirshipStory.advance(npc,stage));
    }
    @Test void unknownNpcCannotAdvanceAnything() {
        for(int stage=0;stage<=AirshipStory.LAST_STAGE;stage++) assertEquals(stage,AirshipStory.advance("forged",stage));
    }
    @Test void everyStageHasTextAndValidPortraitOwner() {
        for(int stage=0;stage<=AirshipStory.LAST_STAGE;stage++) {
            assertFalse(AirshipStory.objective(stage).isBlank());
            assertTrue(AirshipStory.index(AirshipStory.owner(stage))>=0);
            for(String npc:AirshipStory.IDS) assertFalse(AirshipStory.dialogue(npc,stage).isBlank());
        }
    }
    @org.junit.jupiter.api.Test void earlyCampaignDoesNotAssumePlayerHeardOptionalCall() {
        String bram=AirshipStory.dialogue("quartermaster",6);
        String captain=AirshipStory.dialogue("captain",7);
        assertFalse(bram.contains("слышал зов"));
        assertFalse(captain.contains("Ты слышал"));
        assertTrue(captain.contains("не знаем"));
        assertFalse(AirshipStory.objective(10).contains("снова звучит"));
    }
}
