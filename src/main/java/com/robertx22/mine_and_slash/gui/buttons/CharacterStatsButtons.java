package com.robertx22.mine_and_slash.gui.buttons;

import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.library_of_exile.wrappers.ExileText;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.stats.IUsableStat;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.gui.screens.character_screen.MainHubScreen;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.NumberUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CharacterStatsButtons extends ImageButton {

    public static int BUTTON_SIZE_X = 16;
    public static int BUTTON_SIZE_Y = 16;

    MainHubScreen.StatType type;

    public CharacterStatsButtons(MainHubScreen.StatType type, int xPos, int yPos) {
        super(xPos, yPos, BUTTON_SIZE_X, BUTTON_SIZE_Y, 0, 0, BUTTON_SIZE_Y, type.getIcon(), (button) -> {

        });
        this.type = type;
    }

    @Override
    protected ClientTooltipPositioner createTooltipPositioner() {
        return DefaultTooltipPositioner.INSTANCE;
    }

    public void renderLegacyUnused(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        setModTooltip();
        super.extractContents(gui, mouseX, mouseY, delta);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        if (this.isHovered()) setModTooltip();
        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, type.getIcon(), getX(), getY(), BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X);

    }

    public void setModTooltip() {

        List<Component> tooltip = new ArrayList<>();

        Minecraft mc = Minecraft.getInstance();

        for (List<Stat> list : MainHubScreen.STAT_MAP.get(type)) {
            if (!tooltip.isEmpty()) {
                tooltip.add(ExileText.emptyLine().get());
            }

            for (Stat stat : list) {
                tooltip.add(stat.locName().append(": " + getStatString(stat, Load.Unit(mc.player))));
            }
        }


        this.setTooltip(Tooltip.create(TextUTIL.mergeList(tooltip)));


    }


    public static String getStatString(Stat stat, EntityData unitdata) {
        StatData data = unitdata.getUnit().getCalculatedStat(stat);

        String str = "";
        String v1 = NumberUtils.formatForTooltip(data.getValue());

        if (stat instanceof IUsableStat) {
            IUsableStat usable = (IUsableStat) stat;

            String value = NumberUtils.format(usable.getUsableValue(unitdata.getUnit(), (int) data.getValue(), unitdata.getLevel()) * 100);

            str += value + "%";

            if (stat.IsPercent()) {
                str += " (" + v1 + "%)";
            } else {
                str += " (" + v1 + ")";
            }
        } else {
            str += v1;

            if (stat.IsPercent()) {
                str += '%';
            }
        }
        return str;

    }


    public static String getHubStatString(Stat stat, EntityData unitdata) {
        StatData data = unitdata.getUnit().getCalculatedStat(stat);

        String str = "";
        String v1 = NumberUtils.formatForTooltip(data.getValue());

        if (stat instanceof IUsableStat) {
            IUsableStat usable = (IUsableStat) stat;

            String value = NumberUtils.format(usable.getUsableValue(unitdata.getUnit(), (int) data.getValue(), unitdata.getLevel()) * 100);

            str += value;
        } else {
            str += v1;
        }
        return str;

    }

}
