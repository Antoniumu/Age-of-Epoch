package com.robertx22.age_of_exile.airship;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Exact recorded text and measured file durations; no unused generated dialogue. */
public final class AirshipCrewVoice {
    private AirshipCrewVoice() {}
    private static final JsonObject CLIPS=load();
    private static JsonObject load() {
        try(var stream=AirshipCrewVoice.class.getResourceAsStream("/assets/mmorpg/airship/crew-voice.json")) {
            if(stream==null) throw new IllegalStateException("Missing crew voice manifest");
            return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(Exception e) { throw new IllegalStateException("Invalid crew voice manifest",e); }
    }
    private static AirshipVoiceScript.Cue clip(String key) {
        var c=CLIPS.getAsJsonObject(key);
        return new AirshipVoiceScript.Cue(c.get("sound").getAsString(),c.get("text").getAsString(),c.get("durationMillis").getAsInt());
    }
    public static String dialogue(String npc) { return CLIPS.has(npc+"_intro")?clip(npc+"_intro").text():""; }
    public static List<AirshipVoiceScript.Cue> cues(String npc,int stage,String transcript) {
        if(npc.equals("captain")) return AirshipVoiceScript.cues(stage);
        String key=switch(npc) {
            case "mara"->stage==1?"mara_intro":stage==5?"mara_echoes":"";
            case "quartermaster"->stage==2?"bram_intro":stage==6?"bram_path":stage==9?"bram_anchor":"";
            default->npc+"_intro";
        };
        if(!CLIPS.has(key)) return List.of();
        var cue=clip(key);
        // A not-ready quest has a different transcript: never play a premature success line.
        return cue.text().equals(transcript)?List.of(cue):List.of();
    }
}
