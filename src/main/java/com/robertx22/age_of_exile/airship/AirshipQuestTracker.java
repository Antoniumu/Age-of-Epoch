package com.robertx22.age_of_exile.airship;
import com.robertx22.age_of_exile.questbook.QuestBookData;
import java.util.ArrayList;
import java.util.List;
/** Same typed, numerical objectives in dialogue, H and the shared daily/weekly HUD. */
public final class AirshipQuestTracker {
    private AirshipQuestTracker() {}
    public static String main(AirshipData d,QuestBookData b){
        int target=AirshipStory.target(d.stage);
        String count=target>0?" ["+Math.min(target,AirshipCampaign.measured(d,b))+"/"+target+"]":"";
        return "ОСНОВНОЙ СЮЖЕТ"+count+" • "+AirshipStory.objective(d.stage);
    }
    public static int sideProgress(AirshipData d,QuestBookData b,String npc,int iron){return Math.clamp(switch(npc){
        case "captain"->b.mobKills-d.favorKills;case "mara"->b.chestsLooted-d.favorChests;case "quartermaster"->iron;default->0;},0,sideTarget(npc));}
    public static int sideTarget(String npc){return npc.equals("captain")?5:npc.equals("mara")?2:npc.equals("quartermaster")?16:0;}
    public static String side(AirshipData d,QuestBookData b,String npc,int iron){return "ПОБОЧНОЕ • "+AirshipStory.name(npc).split(" • ")[0]+" • "+sideProgress(d,b,npc,iron)+"/"+sideTarget(npc)+" "+switch(npc){case "captain"->"противников";case "mara"->"новых сундуков";default->"железных слитков";};}
    public static List<String> active(AirshipData d,QuestBookData b,int iron){return active(d,b,iron,0);}
    public static List<String> active(AirshipData d,QuestBookData b,int iron,int bread){
        var result=new ArrayList<String>();for(String id:new String[]{"captain","mara","quartermaster"})if((d.favorAccepted&AirshipCampaign.bit(id))!=0 && (d.favorCompleted&AirshipCampaign.bit(id))==0)result.add(side(d,b,id,iron));for(var q:com.robertx22.age_of_exile.airship.adventure.AdventureCatalog.QUESTS)if(d.adventure.accepted.contains(q.id())&&!d.adventure.completed.contains(q.id()))result.add("ПОБОЧНОЕ • "+q.title()+" • "+com.robertx22.age_of_exile.airship.adventure.AdventureEngine.progress(d.adventure,q,bread)+"/"+q.target()+" "+q.objective());return result;
    }
}
