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
    public static AirshipVoiceScript.Cue cue(String npc,boolean completed){
        var q=AdventureCatalog.personal(npc);String key=npc+(completed?"_thanks":"_request");
        if(q==null||!CLIPS.has(key))return null;
        var clip=CLIPS.getAsJsonObject(key);String text=clip.get("text").getAsString();
        // Fail closed when authored text changes: outdated audio must not contradict the displayed line.
        if(!text.equals(completed?q.thanks():q.request()))return null;
        return new AirshipVoiceScript.Cue(clip.get("sound").getAsString(),text,clip.get("durationMillis").getAsInt());
    }
}
