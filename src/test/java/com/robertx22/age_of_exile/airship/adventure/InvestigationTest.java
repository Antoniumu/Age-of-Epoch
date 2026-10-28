package com.robertx22.age_of_exile.airship.adventure;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InvestigationTest {
    private AdventureData start(String choice){
        var d=new AdventureData();AdventureEngine.witnessCall(d);assertTrue(AdventureEngine.chooseCall(d,choice));
        assertEquals(Investigation.ANALYSIS,Investigation.act(d,"mara",30,0));return d;
    }
    private AdventureData proof(String choice){
        var d=start(choice);
        assertEquals("",Investigation.fish(d,"minecraft:diamond",99,"bad"));
        assertEquals("",Investigation.fish(d,"minecraft:cod",1,"cast1"));
        assertEquals("",Investigation.fish(d,"minecraft:cod",1,"cast1"));
        assertEquals(1,d.progress.get("investigation.water"));
        assertFalse(Investigation.fish(d,"minecraft:salmon",2,"cast2").isEmpty());
        assertEquals("",Investigation.act(d,"tik",31,3));assertFalse(d.facts.contains("evidence.device"));
        assertFalse(Investigation.act(d,"tik",31,4).isEmpty());
        assertEquals("",Investigation.act(d,"oren",33,0));
        assertFalse(Investigation.act(d,"lysander",32,0).isEmpty());assertEquals(3,Investigation.evidenceCount(d));
        assertFalse(Investigation.act(d,"oren",33,0).isEmpty());
        assertEquals(Investigation.CONFESSION,Investigation.act(d,"captain",34,0));return d;
    }
    @Test void noSpoilersBeforeCallOrEvidenceAndNoCrossNpcAction(){
        var d=new AdventureData();
        assertTrue(Investigation.options(d,"captain").isEmpty());assertTrue(Investigation.journal(d).isEmpty());assertEquals("",Investigation.act(d,"captain",34,99));
        assertEquals("",Investigation.fish(d,"minecraft:cod",3,"cast"));
        d=start("record");assertEquals("",Investigation.act(d,"captain",30,99));assertEquals("",Investigation.act(d,"captain",35,99));
        assertEquals("",Investigation.act(d,"mara",30,99));
    }
    @Test void everyResponseHasDifferentEvidenceAndAllCanContinue(){
        for(String choice:java.util.List.of("answer","silence","record")){
            var d=proof(choice);assertTrue(d.facts.contains("evidence.trace."+choice));
            assertEquals(3,Investigation.options(d,"captain").size());assertTrue(Investigation.journal(d).stream().anyMatch(s->s.contains(Investigation.CONFESSION)));
        }
    }
    @Test void publicTruthHasDistinctReactionsAndCannotBeFarmed(){
        var d=proof("answer");d.trust.put("quartermaster",20);
        assertFalse(Investigation.act(d,"captain",35,0).isEmpty());assertTrue(d.facts.contains("truth.shared"));
        assertEquals(17,d.trust.get("quartermaster"));int size=d.facts.size();
        assertEquals("",Investigation.act(d,"captain",35,0));assertEquals(size,d.facts.size());
        assertNotEquals(Investigation.reaction(d,"captain"),Investigation.reaction(d,"mara"));
    }
    @Test void cautionIsAnUnfinishedPromiseNotAnAlternateFreeCompletion(){
        var d=proof("silence");assertFalse(Investigation.act(d,"captain",36,0).isEmpty());
        assertTrue(d.facts.contains("promise.return_to_crew"));assertFalse(d.facts.contains("truth.shared"));
        assertEquals("",Investigation.act(d,"lysander",38,0));
        assertFalse(Investigation.act(d,"mara",38,0).isEmpty());assertTrue(d.facts.contains("truth.shared"));
        assertFalse(d.facts.contains("promise.return_to_crew"));assertTrue(Investigation.decided(d));
        assertTrue(Investigation.options(d,"captain").isEmpty());assertEquals("",Investigation.act(d,"captain",35,0));
    }
    @Test void mediatorNeedsFollowupAndKeepsOriginalChoiceHistory(){
        var d=proof("record");Investigation.act(d,"captain",37,0);
        assertFalse(d.facts.contains("truth.shared"));assertTrue(d.facts.contains("promise.mediated_talk"));
        assertFalse(Investigation.act(d,"lysander",39,0).isEmpty());assertTrue(d.facts.contains("truth.mediator_completed"));
        assertEquals("",Investigation.act(d,"lysander",39,0));assertTrue(Investigation.decided(d));assertEquals("record",d.callDecision);
    }
    @Test void recordedConfidenceAndInvestigationLinesRequireAuthoritativeFacts(){
        var d=new AdventureData();assertNull(AdventureVoice.action(d,"captain",34));assertNull(AdventureVoice.action(d,"mara",30));
        for(String npc:com.robertx22.age_of_exile.airship.AirshipStory.IDS){
            assertNull(AdventureVoice.action(d,npc,22));d.facts.add("memory:"+npc+":confidence");
            assertEquals(AdventureEngine.confidence(npc),AdventureVoice.action(d,npc,22).text());
        }
        d=proof("record");assertEquals(Investigation.CONFESSION,AdventureVoice.action(d,"captain",34).text());
        assertEquals(Investigation.ANALYSIS,AdventureVoice.action(d,"mara",30).text());
        assertNull(AdventureVoice.action(d,"oren",34));
    }
    @Test void invalidFishCannotCrashOrOverflowAndEvidenceIsOneTime(){
        var d=start("record");assertEquals("",Investigation.fish(d,null,1,"x"));assertEquals("",Investigation.fish(d,"minecraft:cod",-1,"x"));
        assertFalse(Investigation.fish(d,"minecraft:cod",Integer.MAX_VALUE,"one").isEmpty());
        assertEquals(3,d.progress.get("investigation.water"));assertEquals("",Investigation.fish(d,"minecraft:cod",1,"two"));
    }
}
