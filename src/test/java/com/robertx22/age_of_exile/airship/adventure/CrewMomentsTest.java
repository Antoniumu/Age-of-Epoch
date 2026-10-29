package com.robertx22.age_of_exile.airship.adventure;

import com.robertx22.age_of_exile.airship.AirshipStory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrewMomentsTest {
    @Test void everyCharacterHasAOneTimePersonalChoiceAndAnIndependentMemory(){
        for(String npc:AirshipStory.IDS){
            var d=new AdventureData();var other=new AdventureData();var q=AdventureCatalog.personal(npc);
            assertNotNull(CrewMoments.moment(npc));assertFalse(CrewMoments.available(d,npc));
            assertEquals("",CrewMoments.choose(d,npc,0));
            d.completed.add(q.id());assertFalse(CrewMoments.available(d,npc));
            d.facts.add("memory:"+npc+":confidence");assertTrue(CrewMoments.available(d,npc));
            assertEquals("",CrewMoments.choose(d,npc,2));assertTrue(CrewMoments.available(d,npc));
            assertEquals(CrewMoments.moment(npc).secondReply(),CrewMoments.choose(d,npc,1));
            assertEquals(1,CrewMoments.chosen(d,npc));assertEquals(-1,CrewMoments.chosen(other,npc));
            assertEquals(CrewMoments.moment(npc).secondReply(),CrewMoments.remembered(d,npc));
            int heart=AdventureEngine.heart(d,npc);assertEquals("",CrewMoments.choose(d,npc,0));
            assertEquals(heart,AdventureEngine.heart(d,npc));
        }
    }
    @Test void invalidNpcsCannotCreateMemories(){
        var d=new AdventureData();assertFalse(CrewMoments.available(d,"unknown"));
        assertEquals("",CrewMoments.choose(d,"unknown",0));assertTrue(d.facts.isEmpty());
    }
    @Test void recordedMomentsRequireMatchingSpeakerChoiceAndConfirmedLine(){
        for(String npc:new String[]{"oren","tik","mara","quartermaster","veya","selin","lysander"})for(int choice=0;choice<2;choice++){
            var d=new AdventureData();d.completed.add(AdventureCatalog.personal(npc).id());d.facts.add("memory:"+npc+":confidence");
            assertNull(AdventureVoice.action(d,npc,90+choice));
            d.lastInvestigationLine=CrewMoments.choose(d,npc,choice);
            assertNotNull(AdventureVoice.action(d,npc,90+choice));
            assertEquals(d.lastInvestigationLine,AdventureVoice.action(d,npc,90+choice).text());
            assertNull(AdventureVoice.action(d,npc,91-choice));
            assertNull(AdventureVoice.action(d,npc.equals("oren")?"tik":"oren",90+choice));
            d.lastInvestigationLine="different";assertNull(AdventureVoice.action(d,npc,90+choice));
        }
    }
}
