package com.robertx22.age_of_exile.airship;
import com.robertx22.age_of_exile.questbook.QuestBookData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AirshipQuestTrackerTest {
    @Test void unacceptedFavorsNeverAppearAndAcceptanceStartsAtZero(){
        var d=new AirshipData();d.stage=1;var b=new QuestBookData();b.mobKills=90;
        assertTrue(AirshipQuestTracker.active(d,b,0).isEmpty());assertTrue(AirshipCampaign.takeFavor(d,b,"captain"));
        assertTrue(AirshipQuestTracker.active(d,b,0).getFirst().contains("0/5 противников"));
        b.mobKills+=3;assertEquals(3,AirshipQuestTracker.sideProgress(d,b,"captain",0));
        b.mobKills+=30;assertEquals(5,AirshipQuestTracker.sideProgress(d,b,"captain",0));
        assertTrue(AirshipCampaign.finishFavor(d,b,"captain",0));assertTrue(AirshipQuestTracker.active(d,b,0).isEmpty());
    }
    @Test void countsNeverGoNegativeAndIronIsAnInventoryObjective(){
        var d=new AirshipData();var b=new QuestBookData();d.favorKills=100;d.favorChests=100;
        assertEquals(0,AirshipQuestTracker.sideProgress(d,b,"captain",0));assertEquals(0,AirshipQuestTracker.sideProgress(d,b,"mara",0));
        assertEquals(12,AirshipQuestTracker.sideProgress(d,b,"quartermaster",12));assertEquals(16,AirshipQuestTracker.sideProgress(d,b,"quartermaster",64));
    }
    @Test void mainObjectiveHasItsOwnLabelAndFreshNumericProgress(){
        var d=new AirshipData();var b=new QuestBookData();d.stage=5;d.baselineChests=20;b.chestsLooted=22;
        assertTrue(AirshipQuestTracker.main(d,b).startsWith("ОСНОВНОЙ СЮЖЕТ [2/3]"));
    }
    @Test void revisionsAndOldAnchorsNeverOverwritePersonalStory(){
        var h=new HubState();h.exists=true;h.revision=4;h.x=300;h.y=140;h.z=200;h.history.add(new HubState.Anchor(1,100,2));
        var d=new AirshipData();d.x=1;d.y=100;d.z=2;d.stage=9;d.favorAccepted=7;
        assertTrue(h.knownOldAnchor(d));assertTrue(h.mirror(d));assertEquals(4,d.hubRevision);assertEquals(9,d.stage);assertEquals(7,d.favorAccepted);assertFalse(h.mirror(d));
    }
    @Test void inFlightTargetAndSourceAreBothProtected(){
        var h=new HubState();h.exists=true;h.x=1;h.y=140;h.z=2;h.jumpPending=true;h.targetX=300;h.targetY=160;h.targetZ=400;
        assertTrue(h.contains(1,140,2));assertTrue(h.contains(300,160,400));assertFalse(h.contains(1000,160,400));h.jumpPending=false;assertFalse(h.contains(300,160,400));
    }
    @Test void everyDeliveryUsesItsOwnItemCountNotTheBreadCount(){
        var d=new AirshipData();var b=new QuestBookData();
        for(String id:java.util.List.of("bram.shared_table","veya.light_at_gangway","selin.her_own_place","lysander.ordinary_names","tik.spare_key"))d.adventure.accepted.add(id);
        var lines=AirshipQuestTracker.active(d,b,0,java.util.Map.of("bram.shared_table",8,"veya.light_at_gangway",2,"tik.spare_key",1));
        assertTrue(lines.stream().anyMatch(x->x.contains("Свет у трапа")&&x.contains("2/4")));
        assertTrue(lines.stream().anyMatch(x->x.contains("Место для лекаря")&&x.contains("0/2")));
        assertTrue(lines.stream().anyMatch(x->x.contains("Не запасная дружба")&&x.contains("1/1")));
        assertTrue(AirshipQuestTracker.active(d,b,0,8).stream().anyMatch(x->x.contains("Свет у трапа")&&x.contains("0/4")));
    }
}
