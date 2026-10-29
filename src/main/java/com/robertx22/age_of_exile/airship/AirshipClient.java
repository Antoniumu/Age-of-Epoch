package com.robertx22.age_of_exile.airship;
import net.minecraft.client.Minecraft;

/** Client proxy. Never called by world generation or server packet handlers. */
public final class AirshipClient {
    private AirshipClient() {}
    public static void openHub(String nonce,String message) { Minecraft.getInstance().gui.setScreen(new AirshipHubScreen(nonce,message)); }
    public static void open(int entityId,String npc,String texture) {
        if(AirshipStory.index(npc)>=0) {
            Minecraft.getInstance().gui.setScreen(new AirshipDialogueScreen(entityId,npc,texture));
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment())System.out.println("AIRSHIPROUTE CLIENT_OPEN npc="+npc);
        }
    }
}
