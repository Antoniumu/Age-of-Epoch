package com.robertx22.age_of_exile.airship;

import com.robertx22.age_of_exile.questbook.QuestBookData;

/** Pure, server-tested campaign rules. Existing measured RPG counters drive the new narrative. */
public final class AirshipCampaign {
    private AirshipCampaign() {}
    public static int measured(AirshipData d,QuestBookData book) { return switch(AirshipStory.goal(d.stage)) {
        case CHESTS->Math.max(0,book.chestsLooted-d.baselineChests);
        case KILLS->Math.max(0,book.mobKills-d.baselineKills);
        case DUNGEON->Math.max(0,book.standardDungeonsCompleted-d.baselineDungeons);
        default->0;}; }
    public static boolean ready(AirshipData d,QuestBookData b) { return AirshipStory.goal(d.stage)!=AirshipStory.Goal.COMPLETE && measured(d,b)>=AirshipStory.target(d.stage); }
    public static void baseline(AirshipData d,QuestBookData b) { d.baselineChests=b.chestsLooted;d.baselineKills=b.mobKills;d.baselineDungeons=b.standardDungeonsCompleted; }
    public static boolean advance(AirshipData d,QuestBookData b,String npc,int expectedStage,int choice) {
        if(d.stage!=expectedStage || choice<0 || choice>1 || !ready(d,b)) return false;
        int next=AirshipStory.advance(npc,d.stage);
        if(next==d.stage) return false;
        d.addTrust(npc,AirshipStory.target(d.stage)>0?8:3);
        if(d.stage==7) { d.storyDecision=choice==1?1:2;d.addTrust(choice==1?"mara":"quartermaster",5); }
        if(next==11) for(String id:AirshipStory.IDS) d.addTrust(id,5);
        d.stage=next;baseline(d,b);return true;
    }
    public static int bit(String id) { int i=AirshipStory.index(id);return i<0?0:1<<i; }
    public static boolean favorReady(AirshipData d,QuestBookData b,String npc,int iron) {
        if((d.favorAccepted&bit(npc))==0 || (d.favorCompleted&bit(npc))!=0) return false;
        return switch(npc) {case "captain"->b.mobKills-d.favorKills>=5;case "mara"->b.chestsLooted-d.favorChests>=2;case "quartermaster"->iron>=16;default->false;};
    }
    public static boolean takeFavor(AirshipData d,QuestBookData b,String npc) {
        int bit=bit(npc);if(!AirshipStory.hasFavor(npc) || bit==0 || (d.favorAccepted&bit)!=0 || d.stage<1) return false;
        d.favorAccepted|=bit;
        if(npc.equals("captain")) d.favorKills=b.mobKills;
        if(npc.equals("mara")) d.favorChests=b.chestsLooted;
        return true;
    }
    public static boolean finishFavor(AirshipData d,QuestBookData b,String npc,int iron) {
        if(!favorReady(d,b,npc,iron)) return false;
        d.favorCompleted|=bit(npc);d.addTrust(npc,15);return true;
    }
}
