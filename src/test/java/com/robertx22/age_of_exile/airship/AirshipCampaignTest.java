package com.robertx22.age_of_exile.airship;
import com.robertx22.age_of_exile.questbook.QuestBookData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirshipCampaignTest {
    @Test void allThreeChaptersRequireFreshMeasuredProgress() {
        var d=new AirshipData();var b=new QuestBookData();b.chestsLooted=100;b.mobKills=100;b.standardDungeonsCompleted=20;
        for(int stage=0;stage<11;stage++) {
            assertEquals(stage,d.stage);
            if(AirshipStory.target(stage)>0) {
                assertFalse(AirshipCampaign.ready(d,b));
                assertFalse(AirshipCampaign.advance(d,b,AirshipStory.actor(stage),stage,0));
                switch(AirshipStory.goal(stage)) {case CHESTS->b.chestsLooted+=3;case KILLS->b.mobKills+=10;case DUNGEON->b.standardDungeonsCompleted++;default->fail();}
            }
            assertTrue(AirshipCampaign.advance(d,b,AirshipStory.actor(stage),stage,1));
            assertFalse(AirshipCampaign.advance(d,b,AirshipStory.actor(stage),stage,1),"stale/replayed packet");
        }
        assertEquals(1,d.storyDecision);assertFalse(AirshipCampaign.ready(d,b));
        assertTrue(d.captainTrust>0 && d.maraTrust>0 && d.bramTrust>0);
    }
    @Test void countersCannotGoNegativeAndCannotAdvanceWrongActor() {
        var d=new AirshipData();var b=new QuestBookData();d.stage=5;d.baselineChests=100;b.chestsLooted=1;
        assertEquals(0,AirshipCampaign.measured(d,b));b.chestsLooted=103;
        assertFalse(AirshipCampaign.advance(d,b,"captain",5,0));assertFalse(AirshipCampaign.advance(d,b,"mara",5,2));
        assertFalse(AirshipCampaign.advance(d,b,"mara",5,-1));assertEquals(0,d.maraTrust);
    }
    @Test void personalFavorsAreIndependentFreshAndOnceOnly() {
        var d=new AirshipData();var b=new QuestBookData();d.stage=1;b.mobKills=100;b.chestsLooted=100;
        for(String id:AirshipStory.IDS) { assertTrue(AirshipCampaign.takeFavor(d,b,id));assertFalse(AirshipCampaign.takeFavor(d,b,id)); }
        assertFalse(AirshipCampaign.finishFavor(d,b,"captain",0));assertFalse(AirshipCampaign.finishFavor(d,b,"mara",0));
        b.mobKills+=5;b.chestsLooted+=2;
        assertFalse(AirshipCampaign.finishFavor(d,b,"quartermaster",15));
        for(String id:AirshipStory.IDS) { assertTrue(AirshipCampaign.finishFavor(d,b,id,16));assertEquals(15,d.trust(id));assertFalse(AirshipCampaign.finishFavor(d,b,id,16)); }
        assertEquals(7,d.favorCompleted);assertEquals(0,d.stage-1);
    }
    @Test void choiceChangesCrewReputationWithoutSkippingAStage() {
        var honest=new AirshipData();var cautious=new AirshipData();var b=new QuestBookData();honest.stage=cautious.stage=7;
        assertTrue(AirshipCampaign.advance(honest,b,"captain",7,1));assertTrue(AirshipCampaign.advance(cautious,b,"captain",7,0));
        assertEquals(8,honest.stage);assertEquals(8,cautious.stage);assertEquals(5,honest.maraTrust);assertEquals(5,cautious.bramTrust);
        assertEquals(1,honest.storyDecision);assertEquals(2,cautious.storyDecision);
    }
    @Test void reputationIsBoundedAndUnknownActorsCannotEarnIt() {
        var d=new AirshipData();d.addTrust("captain",1000);d.addTrust("mara",-100);d.addTrust("fake",1000);
        assertEquals(100,d.captainTrust);assertEquals(0,d.maraTrust);assertEquals(0,d.trust("fake"));
        assertFalse(AirshipCampaign.takeFavor(d,new QuestBookData(),"fake"));
    }
    @Test void savedFieldsRoundTripAndMissingFieldsKeepDefaults() {
        var gson=new com.google.gson.Gson();var d=new AirshipData();d.stage=9;d.maraTrust=26;d.favorAccepted=7;d.favorCompleted=2;d.storyDecision=1;d.baselineDungeons=12;
        var restored=gson.fromJson(gson.toJson(d),AirshipData.class);
        assertEquals(9,restored.stage);assertEquals(26,restored.maraTrust);assertEquals(7,restored.favorAccepted);assertEquals(2,restored.favorCompleted);assertEquals(12,restored.baselineDungeons);assertEquals(1,restored.storyDecision);
        var old=gson.fromJson("{\"stage\":4,\"summoned\":true}",AirshipData.class);assertEquals(0,old.maraTrust);assertEquals(4,old.stage);
    }
}
