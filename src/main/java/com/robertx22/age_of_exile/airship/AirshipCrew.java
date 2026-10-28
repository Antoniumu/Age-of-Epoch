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
            if(json.has(id)) {var value=json.get(id);return Identifier.parse(value.isJsonObject()?value.getAsJsonObject().get("texture").getAsString():value.getAsString()).toString();}
        } catch(Exception e) {
            System.err.println("Invalid airship-crew.json; using original bundled skin: "+e.getMessage());
        }
        return fallback;
    }
    public static String bundledModel(String id){
        return java.util.Set.of("mara","veya","selin").contains(id)?"slim":"wide";
    }
    public static String model(String id) {
        Path file=FabricLoader.getInstance().getConfigDir().resolve("mmorpg/airship-crew.json");
        if(Files.isRegularFile(file))try{var json=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(json.has(id)){
                var value=json.get(id);
                if(value.isJsonObject()&&value.getAsJsonObject().has("model")&&value.getAsJsonObject().get("model").getAsString().equals("slim"))return "slim";
                return "wide"; // Legacy external texture overrides retain their original wide default.
            }
        }catch(Exception ignored){/* Invalid override must not make the shared crew unusable. */}
        return bundledModel(id);
    }
}
