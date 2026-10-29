package com.robertx22.age_of_exile.airship.adventure;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrewWorkshopTest {
    private AdventureData ready(int figure){
        var d=new AdventureData();
        d.completed.add(AdventureCatalog.personal("oren").id());
        d.facts.add("memory:oren:confidence");
        assertFalse(CrewMoments.choose(d,"oren",figure).isBlank());
        d.completed.add(AdventureCatalog.personal("tik").id());
        return d;
    }
    @Test void consentAndCharacterOwnedProgressForBothPaths(){
        for(int figure=0;figure<2;figure++)for(int action:new int[]{93,94}){
            var d=ready(figure);var other=new AdventureData();
            assertTrue(CrewWorkshop.eligible(d));assertFalse(CrewWorkshop.eligible(other));
            assertEquals("",CrewWorkshop.act(d,"tik",action));
            assertEquals("",CrewWorkshop.act(d,"tik",92));
            assertEquals(CrewWorkshop.OREN_START,CrewWorkshop.act(d,"oren",92));
            assertEquals("",CrewWorkshop.act(d,"oren",92));
            assertEquals(CrewWorkshop.voiceText(action),CrewWorkshop.act(d,"tik",action));
            assertEquals("",CrewWorkshop.act(d,"tik",action==93?94:93));
            assertEquals("",CrewWorkshop.act(d,"oren",97)); // Consent is required.
            assertFalse(CrewWorkshop.act(d,"tik",96).isBlank());
            assertEquals(CrewWorkshop.OREN_REPLY,CrewWorkshop.act(d,"oren",97));
            assertFalse(CrewWorkshop.act(d,"tik",98).isBlank());
            int oren=d.trust.get("oren"),tik=d.trust.get("tik");
            assertEquals("",CrewWorkshop.act(d,"tik",98));
            assertEquals(oren,d.trust.get("oren"));assertEquals(tik,d.trust.get("tik"));
            assertFalse(CrewWorkshop.eligible(other));assertTrue(other.facts.isEmpty());
            var restored=new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(d),AdventureData.class);
            assertEquals(d.facts,restored.facts);assertEquals(d.trust,restored.trust);
            assertEquals("",CrewWorkshop.act(restored,"tik",98));
            assertTrue(CrewWorkshop.objective(restored).contains("ещё впереди"));
        }
    }
    @Test void clipsArePlayedOnlyForExpectedSpeakerAndExactConfirmedLine(){
        for(int action:new int[]{92,93,94,95,97,98}){
            var d=new AdventureData();d.lastInvestigationLine=CrewWorkshop.voiceText(action);
            String npc=action==92||action==97?"oren":"tik";
            assertEquals(d.lastInvestigationLine,AdventureVoice.action(d,npc,action).text());
            assertNull(AdventureVoice.action(d,"captain",action));
            d.lastInvestigationLine="different";assertNull(AdventureVoice.action(d,npc,action));
        }
    }
    @Test void playerCanRespectPrivacyWithoutTellingOrenOrReplayingReward(){
        for(int topic:new int[]{93,94}){
            var d=ready(0);CrewWorkshop.act(d,"oren",92);CrewWorkshop.act(d,"tik",topic);
            assertEquals("",CrewWorkshop.act(d,"oren",95));
            assertFalse(CrewWorkshop.act(d,"tik",95).isBlank());
            assertTrue(d.facts.contains("workshop:private"));assertTrue(d.facts.contains("workshop:finished"));
            assertFalse(d.facts.contains("workshop:consent"));assertFalse(d.facts.contains("workshop:oren_informed"));
            int trust=d.trust.get("tik");assertEquals("",CrewWorkshop.act(d,"oren",97));
            assertEquals("",CrewWorkshop.act(d,"tik",95));assertEquals("",CrewWorkshop.act(d,"tik",96));
            assertEquals(trust,d.trust.get("tik"));assertTrue(CrewWorkshop.options(d,"tik").isEmpty());
            assertTrue(CrewWorkshop.objective(d).contains("оставил разговор"));
            var restored=new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(d),AdventureData.class);
            assertEquals("",CrewWorkshop.act(restored,"oren",97));assertEquals("",CrewWorkshop.act(restored,"tik",95));
        }
    }
}
