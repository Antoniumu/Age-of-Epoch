package com.robertx22.age_of_exile.airship.adventure;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReconTest {
    private AdventureData start(){var d=new AdventureData();d.facts.add("council.closed");d.facts.add("council.choice.map");assertEquals(Recon.CAPTAIN_START,Recon.act(d,"captain",80));return d;}
    @Test void gatesAndExplicitAcceptance(){
        var d=new AdventureData();assertEquals("",Recon.act(d,"captain",80));assertFalse(Recon.observe(d,"minecraft:plains",true,true,40000));
        d.facts.add("council.closed");assertEquals("",Recon.act(d,"veya",81));Recon.act(d,"captain",80);
        assertFalse(Recon.observe(d,"minecraft:plains",true,true,40000));assertEquals("",Recon.act(d,"veya",82));
        assertEquals("",Recon.act(d,"captain",81));assertEquals(Recon.VEYA_START,Recon.act(d,"veya",81));assertEquals("",Recon.act(d,"veya",81));
    }
    @Test void distinctFreshOverworldBiomesAtRealDistanceOnly(){
        var d=start();Recon.act(d,"veya",81);
        assertFalse(Recon.observe(d,"minecraft:plains",true,true,16383));assertFalse(Recon.observe(d,"minecraft:plains",false,true,20000));
        assertFalse(Recon.observe(d,"minecraft:plains",true,false,20000));assertFalse(Recon.observe(d,"minecraft:plains",true,true,Double.NaN));
        assertFalse(Recon.observe(d,null,true,true,20000));assertFalse(Recon.observe(d,"not an id",true,true,20000));
        assertTrue(Recon.observe(d,"minecraft:plains",true,true,16384));assertFalse(Recon.observe(d,"minecraft:plains",true,true,90000));
        assertEquals(1,Recon.count(d));assertEquals("",Recon.act(d,"veya",82));
        assertTrue(Recon.observe(d,"minecraft:forest",true,true,20000));assertFalse(Recon.observe(d,"minecraft:desert",true,true,20000));
        assertEquals(2,Recon.count(d));assertEquals(Recon.VEYA_FINISH,Recon.act(d,"veya",82));
        assertFalse(Recon.observe(d,"minecraft:desert",true,true,90000));assertEquals("",Recon.act(d,"veya",82));
        assertEquals(Recon.CAPTAIN_FINISH,Recon.act(d,"captain",83));assertEquals("",Recon.act(d,"captain",83));assertTrue(Recon.finished(d));
    }
    @Test void optionalBriefingsAndAllTenRecordedCues(){
        var d=start();assertEquals(Recon.CAPTAIN_START,AdventureVoice.action(d,"captain",80).text());
        for(var b:Recon.BRIEFS){assertEquals(b.text(),Recon.act(d,b.npc(),b.action()));assertEquals(b.text(),AdventureVoice.action(d,b.npc(),b.action()).text());assertEquals("",Recon.act(d,b.npc(),b.action()));}
        Recon.act(d,"veya",81);assertEquals(Recon.VEYA_START,AdventureVoice.action(d,"veya",81).text());
        Recon.observe(d,"minecraft:plains",true,true,20000);Recon.observe(d,"minecraft:forest",true,true,20000);Recon.act(d,"veya",82);
        assertEquals(Recon.VEYA_FINISH,AdventureVoice.action(d,"veya",82).text());Recon.act(d,"captain",83);
        assertEquals(Recon.CAPTAIN_FINISH,AdventureVoice.action(d,"captain",83).text());
    }
    @Test void journalOnlyShowsObservedNamesAndPlanConsequences(){
        for(String plan:new String[]{"map","signal","aid"}){
            var d=new AdventureData();d.facts.add("council.closed");d.facts.add("council.choice."+plan);Recon.act(d,"captain",80);Recon.act(d,"veya",81);
            assertTrue(Recon.context(d).contains(plan.equals("map")?"Марой":plan.equals("signal")?"Тик":"Селин"));
            assertTrue(Recon.journal(d).stream().noneMatch(x->x.contains("minecraft:forest")));
            Recon.observe(d,"minecraft:forest",true,true,20000);assertTrue(Recon.journal(d).stream().anyMatch(x->x.contains("minecraft:forest")));
            assertEquals("",Recon.act(d,"captain",83));
        }
    }
}
