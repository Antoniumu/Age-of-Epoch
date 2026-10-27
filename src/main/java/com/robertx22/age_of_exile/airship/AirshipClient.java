package com.robertx22.age_of_exile.airship;
import net.minecraft.client.Minecraft;

/** Client proxy. Never called by world generation or server packet handlers. */
public final class AirshipClient {
    private AirshipClient() {}
    public static void open(int entityId,String npc) {
        if(AirshipStory.index(npc)>=0) Minecraft.getInstance().gui.setScreen(new AirshipDialogueScreen(entityId,npc));
    }
}
