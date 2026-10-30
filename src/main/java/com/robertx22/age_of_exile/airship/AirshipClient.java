package com.robertx22.age_of_exile.airship;
import net.minecraft.client.Minecraft;

/** Client proxy. Never called by world generation or server packet handlers. */
public final class AirshipClient {
    private AirshipClient() {}
    /** Client-side telemetry for the opt-in integrated-server movement probe. */
    public static void registerDevTelemetry(){
        if(!net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment())return;
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.level==null||client.player==null||client.level.getGameTime()%20!=0)return;
            var id=DevCrewWalk.observedActor();if(id==null)return;
            for(var entity:client.level.entitiesForRendering())if(entity.getUUID().equals(id)){
                System.out.println("AIRSHIPWALK CLIENT uuid="+id+" time="+client.level.getGameTime()+" pos="+entity.position()+" player="+client.player.position());break;
            }
        });
    }
    public static void openHub(String nonce,String message) { Minecraft.getInstance().gui.setScreen(new AirshipHubScreen(nonce,message)); }
    public static void open(int entityId,String npc,String texture) {
        if(AirshipStory.index(npc)>=0) {
            Minecraft.getInstance().gui.setScreen(new AirshipDialogueScreen(entityId,npc,texture));
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment())System.out.println("AIRSHIPROUTE CLIENT_OPEN npc="+npc);
        }
    }
}
