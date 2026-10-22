package com.robertx22.mine_and_slash.prophecy.gui;

import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

import static com.robertx22.library_of_exile.tooltip.ExileTooltipUtils.splitLongText;

public class MainProphecyButton extends ImageButton {

    public static int FAVOR_BUTTON_SIZE_X = 34;
    public static int FAVOR_BUTTON_SIZE_Y = 34;

    Minecraft mc = Minecraft.getInstance();

    public MainProphecyButton(int xPos, int yPos) {
        super(xPos, yPos, FAVOR_BUTTON_SIZE_X, FAVOR_BUTTON_SIZE_Y, 0, 0, FAVOR_BUTTON_SIZE_Y, Identifier.parse(String.valueOf("empty")), (button) -> {
        });

    }

    public void renderLegacyUnused(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        setModTooltip();
        super.extractContents(gui, mouseX, mouseY, delta);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        if (this.isHovered()) setModTooltip();
        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, SlashRef.guiId("prophecy/button"), getX(), getY(), FAVOR_BUTTON_SIZE_X, FAVOR_BUTTON_SIZE_X, FAVOR_BUTTON_SIZE_X, FAVOR_BUTTON_SIZE_X, FAVOR_BUTTON_SIZE_X, FAVOR_BUTTON_SIZE_X);

    }

    public void setModTooltip() {

        var data = Load.player(mc.player).prophecy;


        List<MutableComponent> list = new ArrayList<>();

        list.add(Words.PROPHECIES.locName());

        list.addAll(splitLongText(Chats.PROPHECIES_GUIDE.locName()));

        list.add(Component.literal(""));

        //list.add(Words.CURRENT_PROPHECY_CURRENCY.locName(Coin.PROPHECY.getTotalFromInventory(mc.player)));
        //list.add(Words.AVG_LVL.locName(data.getAverageLevel()));
        //list.add(Words.AVG_TIER.locName(data.getAverageTier()));


        this.setTooltip(Tooltip.create(TextUTIL.mergeList(list)));

    }


}