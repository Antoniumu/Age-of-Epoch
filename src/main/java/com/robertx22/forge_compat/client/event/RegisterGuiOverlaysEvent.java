package com.robertx22.forge_compat.client.event;

import com.robertx22.forge_compat.client.gui.overlay.ForgeGui;
import com.robertx22.forge_compat.client.gui.overlay.IGuiOverlay;
import com.robertx22.forge_compat.eventbus.api.Event;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class RegisterGuiOverlaysEvent extends Event {
    private static final ForgeGui FORGE_GUI = new ForgeGui();

    public void registerAbove(Identifier other, String id, IGuiOverlay overlay) {
        registerAboveAll(id, overlay);
    }

    public void registerBelow(Identifier other, String id, IGuiOverlay overlay) {
        registerAboveAll(id, overlay);
    }

    public void registerAboveAll(String id, IGuiOverlay overlay) {
        Identifier hudId = id.contains(":")
            ? Identifier.parse(id.replace(".", "_"))
            : Identifier.fromNamespaceAndPath("mmorpg", id.replace(".", "_"));
        HudElementRegistry.addLast(hudId, (extractor, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            overlay.render(
                FORGE_GUI,
                extractor,
                deltaTracker.getGameTimeDeltaPartialTick(true),
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight()
            );
        });
    }
}
