package com.robertx22.age_of_exile.airship.adventure;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VoyageTest {
    private AdventureData start(){var d=new AdventureData();d.facts.add("truth.shared");assertEquals(Voyage.START,Voyage.act(d,"captain",40));return d;}
    @Test void unfinishedInvestigationCannotStartOrRevealPreparation(){
        var d=new AdventureData();assertTrue(Voyage.options(d,"captain").isEmpty());assertEquals("",Voyage.act(d,"captain",40));
        d.facts.add("truth.review");assertEquals("",Voyage.act(d,"captain",40));assertEquals("",Voyage.act(d,"mara",41));
        assertFalse(Voyage.record(d,Voyage.Metric.PROFESSION,"cooking","old"));
    }
    @Test void activityOnlyCountsAfterExplicitAcceptanceAndNeedsAReport(){
        var d=start();var r=Voyage.ROUTES.getFirst();
        assertFalse(Voyage.record(d,r.metric(),"already_collected_skill","before"));
        assertEquals(r.request(),Voyage.act(d,r.npc(),r.action()));assertTrue(Voyage.activeGoals(d).getFirst().contains("0/1"));assertEquals("",Voyage.act(d,r.npc(),r.action()));
        assertTrue(Voyage.record(d,r.metric(),"already_collected_skill","new_event"));
        assertFalse(Voyage.record(d,r.metric(),"already_collected_skill","new_event"));assertEquals(0,Voyage.completed(d));assertTrue(Voyage.activeGoals(d).getFirst().contains("1/1"));
        assertEquals(r.debrief(),Voyage.act(d,r.npc(),r.action()));int heart=AdventureEngine.heart(d,r.npc());
        assertEquals("",Voyage.act(d,r.npc(),r.action()));assertEquals(heart,AdventureEngine.heart(d,r.npc()));
    }
    @Test void anyTwoDifferentApproachesWorkWithoutAllFive(){
        for(int a=0;a<5;a++)for(int b=a+1;b<5;b++){
            var d=start();
            for(int index:new int[]{a,b}){var r=Voyage.ROUTES.get(index);Voyage.act(d,r.npc(),r.action());
                assertTrue(Voyage.record(d,r.metric(),"native_result","event"+index));Voyage.act(d,r.npc(),r.action());}
            assertEquals(2,Voyage.completed(d));assertEquals(0,Voyage.active(d));assertEquals(Voyage.FINISH,Voyage.act(d,"captain",48));
            assertTrue(Voyage.finished(d));assertEquals("",Voyage.act(d,"captain",48));assertTrue(Voyage.options(d,"tik").isEmpty());
            assertEquals(Voyage.OUTRO,Voyage.act(d,"mara",49));assertEquals("",Voyage.act(d,"mara",49));
        }
    }
    @Test void cancellationFreesCapacityAndNeverRewardsOrReusesProgress(){
        var d=start();var a=Voyage.ROUTES.get(0);var b=Voyage.ROUTES.get(1);var c=Voyage.ROUTES.get(2);
        Voyage.act(d,a.npc(),a.action());Voyage.act(d,b.npc(),b.action());assertEquals(2,Voyage.active(d));
        assertEquals("",Voyage.act(d,c.npc(),c.action()));Voyage.record(d,a.metric(),"craft","first");
        int h=AdventureEngine.heart(d,a.npc());assertFalse(Voyage.act(d,a.npc(),a.action()+10).isEmpty());assertEquals(h,AdventureEngine.heart(d,a.npc()));
        assertEquals(1,Voyage.active(d));assertFalse(Voyage.act(d,c.npc(),c.action()).isEmpty());
        Voyage.act(d,c.npc(),c.action()+10);Voyage.act(d,a.npc(),a.action());assertEquals("",Voyage.act(d,a.npc(),a.action()));
        assertFalse(d.progress.containsKey("voyage.progress.craft"));assertFalse(d.facts.contains("voyage.observed.craft:craft"));
    }
    @Test void wrongNpcInvalidEventsAndPrematureFinishAreRejected(){
        var d=start();assertEquals("",Voyage.act(d,"tik",41));assertEquals("",Voyage.act(d,"captain",48));
        Voyage.act(d,"mara",41);assertFalse(Voyage.record(d,null,"x","x"));assertFalse(Voyage.record(d,Voyage.Metric.PROFESSION,null,"x"));
        assertFalse(Voyage.record(d,Voyage.Metric.PROFESSION,"x",""));assertFalse(Voyage.record(d,Voyage.Metric.PROFESSION,"x","a".repeat(161)));
        assertFalse(Voyage.record(d,Voyage.Metric.DUNGEON,"x","x"));
    }
    @Test void briefingIsOptionalAndAllTenRecordingsMatchTransitions(){
        var d=start();assertEquals(Voyage.START,AdventureVoice.action(d,"captain",40).text());
        assertEquals(Voyage.BRAM,Voyage.act(d,"quartermaster",46));assertEquals(Voyage.BRAM,AdventureVoice.action(d,"quartermaster",46).text());
        assertEquals("",Voyage.act(d,"quartermaster",46));assertEquals(Voyage.LYSANDER,Voyage.act(d,"lysander",47));
        assertEquals(Voyage.LYSANDER,AdventureVoice.action(d,"lysander",47).text());
        for(var r:Voyage.ROUTES){var p=start();Voyage.act(p,r.npc(),r.action());assertEquals(r.request(),AdventureVoice.action(p,r.npc(),r.action()).text());
            Voyage.record(p,r.metric(),"result","fresh");Voyage.act(p,r.npc(),r.action());assertNull(AdventureVoice.action(p,r.npc(),r.action()));}
    }
}
