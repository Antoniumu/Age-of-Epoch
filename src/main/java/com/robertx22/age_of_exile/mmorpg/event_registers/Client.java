package com.robertx22.age_of_exile.mmorpg.event_registers;

import com.robertx22.age_of_exile.a_libraries.dmg_number_particle.DamageParticleRenderer;
import com.robertx22.age_of_exile.a_libraries.mob_plates.MobPlateStyleHud;
import com.robertx22.age_of_exile.a_libraries.neat.AnotherTry;
import com.robertx22.age_of_exile.event_hooks.ontick.OnClientTick;
import com.robertx22.age_of_exile.event_hooks.player.OnKeyPress;
import com.robertx22.age_of_exile.gui.overlays.bar_overlays.types.RPGGuiOverlay;
import com.robertx22.age_of_exile.gui.overlays.death_stats.DeathStatsOverlay;
import com.robertx22.age_of_exile.gui.overlays.mob_bar.MobBarScreen;
import com.robertx22.age_of_exile.gui.overlays.party_hud.PartyHudOverlay;
import com.robertx22.age_of_exile.gui.overlays.spell_cast_bar.SpellCastBarOverlay;
import com.robertx22.age_of_exile.gui.overlays.spell_hotbar.SpellHotbarOverlay;
import com.robertx22.age_of_exile.gui.overlays.spell_audit.SpellAuditOverlay;
import com.robertx22.forge_compat.client.event.RenderGameOverlayEvent;
import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.library_of_exile.main.ForgeEvents;
import net.minecraft.client.Minecraft;

public class Client {

    public static void register() {

        SpellHotbarOverlay spellHotbarOverlay = new SpellHotbarOverlay();
        SpellAuditOverlay spellAuditOverlay = new SpellAuditOverlay();
        DeathStatsOverlay death = new DeathStatsOverlay();
        SpellCastBarOverlay castbar = new SpellCastBarOverlay();
        RPGGuiOverlay rpggui = new RPGGuiOverlay();
        MobBarScreen mobBar = new MobBarScreen();
        PartyHudOverlay partyHud = new PartyHudOverlay();

        AnotherTry.register();

        ForgeEvents.registerForgeEvent(TickEvent.ClientTickEvent.class, event -> {
            if (event.phase == TickEvent.Phase.END) {
                OnClientTick.onEndTick(Minecraft.getInstance());
                OnKeyPress.onEndTick(Minecraft.getInstance());
            }
        });

        ForgeEvents.registerForgeEvent(RenderGameOverlayEvent.class, event -> {

            if (event.isCancelable() || event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
                return;
            }

            spellHotbarOverlay.onHudRender(event.getMatrixStack(), event.getPartialTicks());
            castbar.onHudRender(event.getMatrixStack(), event.getPartialTicks());
            death.onHudRender(event.getMatrixStack(), event.getPartialTicks());
            rpggui.onHudRender(event.getMatrixStack(), event.getPartialTicks());
            mobBar.onHudRender(event.getMatrixStack(), event.getPartialTicks());
            partyHud.onHudRender(event.getMatrixStack(), event.getPartialTicks());
            spellAuditOverlay.onHudRender(event.getMatrixStack(), event.getPartialTicks());
            DamageParticleRenderer.renderHudParticles(event.getMatrixStack());
            MobPlateStyleHud.render(event.getMatrixStack());

        });
    }
}
