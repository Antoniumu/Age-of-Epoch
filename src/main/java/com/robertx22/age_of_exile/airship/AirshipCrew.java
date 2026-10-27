package com.robertx22.age_of_exile.airship;

import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import java.nio.file.Files;
import java.nio.file.Path;

/** Local resource textures, not account names: works offline and cannot change with someone's account. */
public final class AirshipCrew {
    private AirshipCrew() {}
    public static String texture(String id) {
        String fallback="mmorpg:entity/airship/"+(AirshipStory.index(id)>=0?id:"captain");
        Path file=FabricLoader.getInstance().getConfigDir().resolve("mmorpg/airship-crew.json");
        if(!Files.isRegularFile(file)) return fallback;
        try {
            var json=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(json.has(id)) return Identifier.parse(json.get(id).getAsString()).toString();
        } catch(Exception e) {
            System.err.println("Invalid airship-crew.json; using vanilla skin: "+e.getMessage());
        }
        return fallback;
    }
    public static String model(String id) { return "wide"; }
}
