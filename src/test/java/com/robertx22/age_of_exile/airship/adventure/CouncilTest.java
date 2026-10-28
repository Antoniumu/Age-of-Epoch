package com.robertx22.age_of_exile.airship.adventure;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CouncilTest {
    private AdventureData start(){var d=new AdventureData();d.facts.add("voyage.finished");assertEquals(Council.START,Council.act(d,"captain",60,0,0));return d;}
    private void heardTwo(AdventureData d){assertFalse(Council.act(d,"mara",61,0,0).isEmpty());assertFalse(Council.act(d,"tik",62,0,0).isEmpty());}
    @Test void gateAndDistinctVoices(){
        var d=new AdventureData();assertEquals("",Council.act(d,"captain",60,0,0));d.facts.add("voyage.finished");
        assertEquals("",Council.act(d,"mara",60,0,0));assertEquals(Council.START,Council.act(d,"captain",60,0,0));
        assertEquals("",Council.act(d,"captain",70,0,0));assertEquals("",Council.act(d,"tik",61,0,0));
        assertFalse(Council.act(d,"mara",61,0,0).isEmpty());assertEquals("",Council.act(d,"mara",61,0,0));
        assertEquals(1,Council.heard(d));assertEquals("",Council.act(d,"captain",71,2,4));
        assertFalse(Council.act(d,"veya",65,0,0).isEmpty());assertEquals(2,Council.heard(d));
        assertFalse(Council.act(d,"captain",70,0,0).isEmpty());assertEquals("",Council.act(d,"captain",71,2,4));
    }
    @Test void mapRequiresNewFishAndRealFollowup(){
        var d=start();heardTwo(d);assertFalse(Council.fish(d,"minecraft:cod",1,"old"));
        Council.act(d,"captain",70,0,0);assertEquals("",Council.act(d,"mara",73,0,0));
        assertFalse(Council.fish(d,null,1,"x"));assertFalse(Council.fish(d,"minecraft:stick",1,"x"));assertFalse(Council.fish(d,"minecraft:cod",0,"x"));
        assertFalse(Council.fish(d,"minecraft:cod",1,""));assertTrue(Council.fish(d,"minecraft:cod",1,"hook:0"));assertFalse(Council.fish(d,"minecraft:cod",1,"hook:0"));
        assertEquals("",Council.act(d,"tik",73,99,99));assertEquals(Council.MAP,Council.act(d,"mara",73,0,0));
        int value=AdventureEngine.heart(d,"mara");assertEquals("",Council.act(d,"mara",73,0,0));assertEquals(value,AdventureEngine.heart(d,"mara"));
        assertEquals(Council.CLOSE,Council.act(d,"captain",74,0,0));assertTrue(Council.finished(d));assertEquals("",Council.act(d,"captain",74,0,0));
        assertFalse(d.facts.contains("council.promise.pending"));assertTrue(d.facts.contains("council.choice.map"));
    }
    @Test void deliveriesAreSeparateAndCannotFinishWithoutEnough(){
        for(int action:new int[]{71,72}){
            var d=start();heardTwo(d);Council.act(d,"captain",action,0,0);String npc=action==71?"tik":"selin";
            assertTrue(d.facts.contains("council.promise.pending"));assertEquals("",Council.act(d,npc,73,1,3));assertFalse(d.facts.contains("council.fulfilled"));
            assertEquals("",Council.act(d,"captain",74,999,999));assertFalse(Council.act(d,npc,73,2,4).isEmpty());
            assertEquals("",Council.act(d,npc,73,2,4));assertEquals(Council.CLOSE,Council.act(d,"captain",74,0,0));
        }
    }
    @Test void completeVoiceTextsMatchRecordedLines(){
        var d=start();assertEquals(Council.START,AdventureVoice.action(d,"captain",60).text());
        for(var speaker:Council.SPEAKERS){var p=start();assertEquals(speaker.text(),Council.act(p,speaker.npc(),speaker.action(),0,0));
            assertEquals(speaker.text(),AdventureVoice.action(p,speaker.npc(),speaker.action()).text());}
        heardTwo(d);Council.act(d,"captain",70,0,0);Council.fish(d,"minecraft:cod",1,"1");Council.act(d,"mara",73,0,0);
        assertEquals(Council.MAP,AdventureVoice.action(d,"mara",73).text());Council.act(d,"captain",74,0,0);
        assertEquals(Council.CLOSE,AdventureVoice.action(d,"captain",74).text());
    }
    @Test void choiceAndJournalKeepOnlyEarnedKnowledge(){
        var d=start();Council.act(d,"oren",64,0,0);Council.act(d,"selin",66,0,0);
        assertTrue(Council.journal(d).stream().noneMatch(s->s.contains("Приёмник настроен")));
        assertEquals("",Council.act(d,"captain",74,0,0));Council.act(d,"captain",72,0,0);
        assertTrue(Council.objective(d).contains("4 хлеба"));assertTrue(Council.journal(d).stream().anyMatch(s->s.contains("обещание открыто")));
        Council.act(d,"selin",73,0,4);assertTrue(Council.journal(d).stream().anyMatch(s->s.contains("обещание выполнено")));
    }
}
