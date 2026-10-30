package com.robertx22.age_of_exile.airship;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/** Client-only preference for spoken crew dialogue; never affects ambient sounds or story state. */
public final class AirshipVoiceSettings {
    private static Boolean enabled;
    private AirshipVoiceSettings() {}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("age-of-epoch-voice.properties");}
    static boolean read(Path file){
        if(!Files.isRegularFile(file))return true;
        var properties=new Properties();
        try(var in=Files.newInputStream(file)){properties.load(in);}
        catch(IOException | IllegalArgumentException e){return true;}
        return !"false".equalsIgnoreCase(properties.getProperty("enabled", "true").trim());
    }
    static void write(Path file,boolean value) throws IOException{
        Files.createDirectories(file.getParent());
        var properties=new Properties();properties.setProperty("enabled",Boolean.toString(value));
        try(var out=Files.newOutputStream(file)){properties.store(out,"Age of Epoch spoken dialogue");}
    }
    public static boolean enabled(){if(enabled==null)enabled=read(path());return enabled;}
    public static boolean toggle(){
        enabled=!enabled();
        try{write(path(),enabled);}catch(IOException e){System.err.println("Could not save Age of Epoch voice setting: "+e);}
        return enabled;
    }
    public static String hint(){return "V • Озвучка: "+(enabled()?"вкл":"выкл");}
}
