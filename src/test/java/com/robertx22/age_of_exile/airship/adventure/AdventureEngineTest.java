package com.robertx22.age_of_exile.airship.adventure;
import com.robertx22.age_of_exile.airship.AirshipData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AdventureEngineTest {
    @Test void progressStartsOnlyOnAcceptanceAndRejectsDuplicateFish(){
        var d=new AdventureData();assertFalse(AdventureEngine.record(d,AdventureCatalog.Metric.FISH,"minecraft:cod",1,"cast1"));
        assertTrue(AdventureEngine.accept(d,"captain.quiet_water"));assertFalse(AdventureEngine.accept(d,"captain.quiet_water"));
        assertFalse(AdventureEngine.record(d,AdventureCatalog.Metric.FISH,"minecraft:diamond",1,"fake"));
        assertTrue(AdventureEngine.record(d,AdventureCatalog.Metric.FISH,"minecraft:cod",1,"cast1"));
        assertFalse(AdventureEngine.record(d,AdventureCatalog.Metric.FISH,"minecraft:cod",1,"cast1"));
        assertEquals(1,d.progress.get("captain.quiet_water"));assertFalse(AdventureEngine.finish(d,"captain.quiet_water",0));
        AdventureEngine.record(d,AdventureCatalog.Metric.FISH,"minecraft:salmon",2,"cast2");
        assertTrue(AdventureEngine.finish(d,"captain.quiet_water",0));assertFalse(AdventureEngine.finish(d,"captain.quiet_water",0));
        int heart=AdventureEngine.heart(d,"captain");assertFalse(AdventureEngine.record(d,AdventureCatalog.Metric.FISH,"minecraft:cod",999,"cast3"));assertEquals(heart,AdventureEngine.heart(d,"captain"));
    }
    @Test void biomesAndPlantingPositionsAreUnique(){
        var d=new AdventureData();AdventureEngine.accept(d,"mara.unfinished_map");
        assertTrue(AdventureEngine.record(d,AdventureCatalog.Metric.BIOME,"minecraft:plains",1,"tick1"));
        assertFalse(AdventureEngine.record(d,AdventureCatalog.Metric.BIOME,"minecraft:plains",1,"tick2"));
        AdventureEngine.accept(d,"oren.new_roots");
        assertTrue(AdventureEngine.record(d,AdventureCatalog.Metric.SAPLING,"sapling",1,"overworld:1"));
        assertFalse(AdventureEngine.record(d,AdventureCatalog.Metric.SAPLING,"sapling",1,"overworld:1"));
    }
    @Test void migrationIsOnceAndNeverRewritesOldCampaign(){
        var d=new AirshipData();d.stage=11;d.captainTrust=100;d.favorAccepted=7;d.storyDecision=2;
        AdventureEngine.migrate(d);int seed=d.adventure.trust.get("captain");AdventureEngine.migrate(d);
        assertEquals(seed,d.adventure.trust.get("captain"));assertEquals(100,d.captainTrust);assertEquals(11,d.stage);assertEquals(2,d.storyDecision);assertEquals(7,d.favorAccepted);
        assertTrue(d.adventure.facts.contains("legacy.introduction_complete"));assertTrue(d.relationshipValue("captain")<=25);
    }
    @Test void noDecisionBeforeEventAndNoReplayingChoice(){
        var d=new AdventureData();assertFalse(AdventureEngine.chooseCall(d,"answer"));assertTrue(AdventureEngine.witnessCall(d));assertFalse(AdventureEngine.witnessCall(d));
        assertFalse(AdventureEngine.chooseCall(d,"unknown"));assertTrue(AdventureEngine.chooseCall(d,"record"));assertFalse(AdventureEngine.chooseCall(d,"answer"));assertEquals("record",d.callDecision);
    }
    @Test void heartsRequireNarrativeGatesAndMemoriesAreOnceOnly(){
        var d=new AdventureData();assertTrue(AdventureEngine.bond(d,"oren","test",100,100,100));assertFalse(AdventureEngine.bond(d,"oren","test",100,100,100));
        assertEquals(25,AdventureEngine.heart(d,"oren"));d.completed.add("oren.new_roots");assertEquals(50,AdventureEngine.heart(d,"oren"));
        AdventureEngine.bond(d,"oren","confidence",0,0,0);assertEquals(75,AdventureEngine.heart(d,"oren"));
        assertNotEquals(AdventureEngine.greeting(new AdventureData(),"oren"),AdventureEngine.greeting(d,"oren"));
        assertFalse(AdventureEngine.bond(d,"fake","confidence",100,100,100));
    }
    @Test void deliveryCannotBeCompletedByInventoryEventOrWithoutAcceptance(){
        var d=new AdventureData();assertFalse(AdventureEngine.finish(d,"bram.shared_table",64));AdventureEngine.accept(d,"bram.shared_table");
        assertFalse(AdventureEngine.record(d,AdventureCatalog.Metric.DELIVERY,"minecraft:bread",64,"fake"));
        assertFalse(AdventureEngine.finish(d,"bram.shared_table",7));assertTrue(AdventureEngine.finish(d,"bram.shared_table",8));assertFalse(AdventureEngine.finish(d,"bram.shared_table",8));
    }
    @Test void invalidNegativeAndOverflowEventsDoNotOverflowProgress(){
        var d=new AdventureData();assertFalse(AdventureEngine.accept(d,"unknown"));AdventureEngine.accept(d,"captain.quiet_water");
        assertFalse(AdventureEngine.record(d,AdventureCatalog.Metric.FISH,"minecraft:cod",-1,"x"));assertFalse(AdventureEngine.record(d,AdventureCatalog.Metric.FISH,"minecraft:cod",1,""));
        assertTrue(AdventureEngine.record(d,AdventureCatalog.Metric.FISH,"minecraft:cod",Integer.MAX_VALUE,"x"));assertEquals(3,d.progress.get("captain.quiet_water"));
    }
}
