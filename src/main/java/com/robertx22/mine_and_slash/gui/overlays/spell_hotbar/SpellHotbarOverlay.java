package com.robertx22.mine_and_slash.gui.overlays.spell_hotbar;

import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.config.forge.overlay.OverlayConfig;
import com.robertx22.mine_and_slash.config.forge.overlay.OverlayType;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.client.SpellKeybind;
import com.robertx22.mine_and_slash.saveclasses.PointData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;


public class SpellHotbarOverlay {


    public static Identifier getHotbarTex(OverlayType type) {
        return hotbarTex(type == OverlayType.SPELL_HOTBAR_HORIZONTAL, ClientConfigs.getConfig().HOTBAR_SWAPPING.get(), SpellKeybind.IS_ON_SECONd_HOTBAR ? 2 : 1);
    }

    static Identifier hotbarTex(Boolean horizontal, Boolean swap, Integer swapnum) {
        String swaptex = swap ? "_swap" + swapnum : "";
        String horizontaltex = horizontal ? "_horizontal" : "";
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/spells/hotbar/" + "hotbar" + swaptex + horizontaltex + ".png");
    }

    Minecraft mc = Minecraft.getInstance();


    public void onHudRender(GuiGraphicsExtractor gui, OverlayConfig config, OverlayType type) {

        try {

            if (false) {
                return;
            }
            if (mc.player.isSpectator()) {
                return;
            }
            if (ChatUtils.isChatOpen() && type == OverlayType.SPELL_HOTBAR_VERTICAL) {
                return;
            }
            if (Load.player(mc.player) == null) {
                return;
            }

            RenderSystem.enableBlend(); // enables transparency

            int x = config.getPos().x;
            int y = config.getPos().y;

            int spells = 8;
            List<SpellOnHotbarRender> list = new ArrayList<>();

            for (int i = 0; i < spells; i++) {
                int place = i;
                int xp = x + 3;
                int yp = y + 3;
                list.add(new SpellOnHotbarRender(type == OverlayType.SPELL_HOTBAR_HORIZONTAL, place, gui, xp, yp)) ;
            }
            if (ClientConfigs.getConfig().HIDE_SPELL_HOTBAR_WHEN_NO_SPELL.get()){
                if (list.stream().anyMatch(spell -> spell.spell != null)){
                    renderHotbarBackground(type, type.getSize(), gui, x, y);
                    list.forEach(SpellOnHotbarRender::render);
                }
            } else {
                renderHotbarBackground(type, type.getSize(), gui, x, y);
                list.forEach(SpellOnHotbarRender::render);
            }

            RenderSystem.disableBlend(); // enables transparency

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void renderHotbarBackground(OverlayType type, PointData size, GuiGraphicsExtractor gui, int x, int y) {
        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        var hotbar = getHotbarTex(type);
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, hotbar, x, y, 0, 0, size.x, size.y);

    }

}