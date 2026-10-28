package com.robertx22.age_of_exile.airship.adventure;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.robertx22.age_of_exile.airship.AirshipVoiceScript;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Optional recorded cues. Unrecorded lines remain text, never another character's voice. */
public final class AdventureVoice {
    private AdventureVoice() {}
    private static final JsonObject CLIPS=load();
    private static JsonObject load(){
        try(var stream=AdventureVoice.class.getResourceAsStream("/assets/mmorpg/airship/adventure-voice.json")){
            if(stream==null)throw new IllegalStateException("Missing adventure voice manifest");
            return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(Exception e){throw new IllegalStateException("Invalid adventure voice manifest",e);}
    }
    private static AirshipVoiceScript.Cue exact(String key,String expected){
        if(!CLIPS.has(key))return null;
        var clip=CLIPS.getAsJsonObject(key);String text=clip.get("text").getAsString();
        if(!text.equals(expected))return null;
        return new AirshipVoiceScript.Cue(clip.get("sound").getAsString(),text,clip.get("durationMillis").getAsInt());
    }
    public static AirshipVoiceScript.Cue cue(String npc,boolean completed){
        var q=AdventureCatalog.personal(npc);if(q==null)return null;
        return exact(npc+(completed?"_thanks":"_request"),completed?q.thanks():q.request());
    }
    public static AirshipVoiceScript.Cue action(AdventureData d,String npc,int action){
        String key=Voyage.voiceKey(d,npc,action);
        if(!key.isEmpty())return exact(key,Voyage.voiceText(key));
        var q=AdventureCatalog.personal(npc);
        if(action==20&&q!=null&&d.accepted.contains(q.id()))return cue(npc,false);
        if(action==21&&q!=null&&d.completed.contains(q.id()))return cue(npc,true);
        if(action==22&&d.facts.contains("memory:"+npc+":confidence"))return exact(npc+"_confidence",AdventureEngine.confidence(npc));
        if(action==30&&npc.equals("mara")&&Investigation.started(d))return exact("mara_analysis",Investigation.ANALYSIS);
        if(action==34&&npc.equals("captain")&&d.facts.contains("testimony.captain"))return exact("captain_confession",Investigation.CONFESSION);
        return null;
    }
}
