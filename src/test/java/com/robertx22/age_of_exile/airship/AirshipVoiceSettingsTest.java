package com.robertx22.age_of_exile.airship;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class AirshipVoiceSettingsTest {
    @TempDir java.nio.file.Path dir;
    @Test void preferenceDefaultsOnAndPersistsBothWays() throws Exception {
        var file=dir.resolve("config/voice.properties");
        assertTrue(AirshipVoiceSettings.read(file));
        AirshipVoiceSettings.write(file,false);assertFalse(AirshipVoiceSettings.read(file));
        AirshipVoiceSettings.write(file,true);assertTrue(AirshipVoiceSettings.read(file));
        Files.writeString(file,"enabled= FALSE ");assertFalse(AirshipVoiceSettings.read(file));
        Files.writeString(file,"enabled=\\uXYZ1");assertTrue(AirshipVoiceSettings.read(file));
    }
}
