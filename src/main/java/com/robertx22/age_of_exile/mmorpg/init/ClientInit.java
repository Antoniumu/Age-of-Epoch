package com.robertx22.age_of_exile.mmorpg.init;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.a_libraries.dmg_number_particle.DamageParticleRenderer;
import com.robertx22.age_of_exile.a_libraries.mob_plates.MobPlateSettings;
import com.robertx22.age_of_exile.mixin_methods.RenderMobInfo;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.event_registers.Client;
import com.robertx22.age_of_exile.mmorpg.registers.client.ClientSetup;
import com.robertx22.age_of_exile.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.event.RenderGameOverlayEvent;
import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLClientSetupEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class ClientInit implements net.fabricmc.api.ClientModInitializer {

    private static boolean clientSetupDone = false;

    @Override
    public void onInitializeClient() {
        com.robertx22.age_of_exile.mmorpg.FabricBootstrap.fireClientSetup();
        onInitializeClient(new FMLClientSetupEvent());

        ClientTickEvents.START_CLIENT_TICK.register(client ->
            MinecraftForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START)));
        ClientTickEvents.END_CLIENT_TICK.register(client ->
            MinecraftForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END)));

        HudElementRegistry.attachElementAfter(
            VanillaHudElements.BOSS_BAR,
            Identifier.fromNamespaceAndPath(SlashRef.MODID, "rpg_hud"),
            (graphics, deltaTracker) -> {
                if (net.minecraft.client.Minecraft.getInstance().gui.screen() != null && !(net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen)) return;
                GuiGraphicsExtractor prevG = AbstractGui.currentGraphics();
                PoseStack prevPose = AbstractGui.currentPose();
                PoseStack pose = new PoseStack();
                AbstractGui.setCurrentGraphics(graphics);
                AbstractGui.setCurrentPose(pose);
                try {
                    MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent(RenderGameOverlayEvent.ElementType.ALL, pose, 0.0F));
                } finally {
                    AbstractGui.setCurrentGraphics(prevG);
                    AbstractGui.setCurrentPose(prevPose);
                }
            }
        );

        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            RenderMobInfo.submitWorldMobLabels(context);
            DamageParticleRenderer.submitWorldParticles(context);
        });
    }

    public static void onInitializeClient(final FMLClientSetupEvent event) {
        if (clientSetupDone) {
            return;
        }
        clientSetupDone = true;

        com.robertx22.age_of_exile.airship.AirshipTikRenderer.register();
        com.robertx22.age_of_exile.airship.AirshipClient.registerDevTelemetry();
        MobPlateSettings.load();
        ClientSetup.setup();
        KeybindsRegister.register();
        Client.register();
    }
}
