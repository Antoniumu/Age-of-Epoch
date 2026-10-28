package com.robertx22.age_of_exile.airship;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AirshipCrewVoiceTest {
    @Test void everyCrewMemberHasReachableSelectedVoice() {
        for(String id:AirshipStory.IDS) {
            int stage=id.equals("mara")?1:id.equals("quartermaster")?2:0;
            var cues=AirshipCrewVoice.cues(id,stage,AirshipStory.dialogue(id,stage));
            assertFalse(cues.isEmpty(),id);
            for(var cue:cues) assertNotNull(getClass().getResource("/assets/mmorpg/sounds/airship/"+cue.sound()+".ogg"),cue.sound());
        }
    }
    @Test void allCurrentDialogueBranchesAreVoiced() {
        for(String id:AirshipStory.IDS) for(int stage=0;stage<=11;stage++)
            assertFalse(AirshipCrewVoice.cues(id,stage,AirshipStory.dialogue(id,stage)).isEmpty(),id+" stage "+stage);
        for(int stage:new int[]{5,6,9}) {
            String id=stage==5?"mara":"quartermaster";
            var text=AirshipCrewVoice.waitingDialogue(id,stage);
            var cues=AirshipCrewVoice.cues(id,stage,text);
            assertEquals(1,cues.size());assertTrue(cues.getFirst().sound().contains("waiting"));
        }
    }
    @Test void successAudioCannotContradictNotReadyTranscript() {
        assertTrue(AirshipCrewVoice.cues("mara",5,"Мы ещё не готовы").isEmpty());
        assertTrue(AirshipCrewVoice.cues("quartermaster",6,"Мы ещё не готовы").isEmpty());
    }
    @Test void newCrewCannotAcceptUnfinishableOriginalFavors() {
        for(String id:new String[]{"oren","veya","selin","lysander","tik"}) assertFalse(AirshipStory.hasFavor(id));
    }
}
