package com.robertx22.age_of_exile.mmorpg;

import com.robertx22.age_of_exile.a_libraries.mob_plates.MobPlateRenderer;
import com.robertx22.age_of_exile.a_libraries.mob_plates.MobPlateSettings;
import com.robertx22.age_of_exile.a_libraries.mob_plates.MobPlateStyleHud;
import com.robertx22.ancient_obelisks.main.ObeliskClient;
import com.robertx22.dungeon_realm.main.DungeonClient;
import com.robertx22.forge_compat.client.GuiGraphics;
import com.robertx22.forge_compat.client.event.EntityRenderersEvent;
import com.robertx22.forge_compat.client.event.RegisterClientTooltipComponentFactoriesEvent;
import com.robertx22.forge_compat.client.event.RegisterKeyMappingsEvent;
import com.robertx22.forge_compat.client.event.RenderGuiOverlayEvent;
import com.robertx22.forge_compat.client.gui.overlay.VanillaGuiOverlay;
import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.forge_compat.eventbus.api.IEventBus;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLClientSetupEvent;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;
import com.robertx22.library_of_exile.main.ClientInit;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.the_harvest.main.HarvestClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class AgeOfEpochClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AgeOfEpochMod.bootstrapCommon();
        MobPlateSettings.load();

        // 1. Initialize client sub-modules across all 6 mods
        ClientInit.onInitializeClient();
        DungeonClient.init();
        ObeliskClient.init();
        HarvestClient.init();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.post(new FMLClientSetupEvent());

        // 2. Register all key mappings, entity renderers, model layers & SocketTooltip factories
        modBus.post(new RegisterKeyMappingsEvent());
        modBus.post(new EntityRenderersEvent.RegisterLayerDefinitions());
        modBus.post(new EntityRenderersEvent.RegisterRenderers());
        modBus.post(new RegisterClientTooltipComponentFactoriesEvent());

        // 3. Client Tick, HUD Overlays & World Mob Plates
        ClientTickEvents.START_CLIENT_TICK.register(client ->
            MinecraftForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START)));
        ClientTickEvents.END_CLIENT_TICK.register(client ->
            MinecraftForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END)));

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "rpg_hud"),
            (guiGraphics, deltaTracker) -> {
                Minecraft mc = Minecraft.getInstance();
                GuiGraphics compatGfx = new GuiGraphics(guiGraphics);
                float pt = deltaTracker.getGameTimeDeltaPartialTick(true);
                MinecraftForge.EVENT_BUS.post(new RenderGuiOverlayEvent.Post(mc.getWindow(), compatGfx, pt, VanillaGuiOverlay.HOTBAR));
                MobPlateStyleHud.render(guiGraphics);
            }
        );

        LevelRenderEvents.COLLECT_SUBMITS.register(MobPlateRenderer::submitWorldMobLabels);
    }
}
