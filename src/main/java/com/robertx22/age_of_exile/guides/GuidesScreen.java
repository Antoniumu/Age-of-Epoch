package com.robertx22.age_of_exile.guides;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.database.data.game_balance_config.PlayerPointsType;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.gui.screens.character_screen.MainHubScreen;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.TalentsScreen;
import com.robertx22.mine_and_slash.gui.screens.spell.SpellSchoolScreen;
import com.robertx22.mine_and_slash.gui.screens.stat_gui.StatScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class GuidesScreen extends BaseScreen implements INamedScreen {

    private String selectedId = "";

    public GuidesScreen() {
        super(380, 236);
    }

    @Override
    public Identifier iconLocation() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/icons/wiki.png");
    }

    @Override
    public Words screenName() {
        return Words.Guides;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        List<GuideDefinition> guides = GuideAdvisor.getAllGuides();
        String activeId = GuideAdvisor.getActiveGuideId(mc.player);
        if (selectedId.isBlank()) {
            selectedId = !activeId.isBlank() ? activeId : guides.get(0).id;
        }

        int listX = this.guiLeft + 8;
        int listY = this.guiTop + 28;
        for (int i = 0; i < guides.size(); i++) {
            GuideDefinition g = guides.get(i);
            String prefix = g.id.equals(activeId) ? "§a★ " : (g.id.equals(selectedId) ? "§e▶ " : "§7");
            publicAddButton(Button.builder(Component.literal(prefix + g.name), b -> {
                this.selectedId = g.id;
                this.init();
            }).bounds(listX, listY + i * 24, 134, 20).build());
        }

        int actY = this.guiTop + 114;
        publicAddButton(Button.builder(Component.literal("§bСделать активным"), b -> {
            Packets.sendToServer(new GuidesPacket("select", selectedId));
        }).bounds(listX, actY, 134, 20).build());

        publicAddButton(Button.builder(Component.literal("§aАвто-вложить очки"), b -> {
            Packets.sendToServer(new GuidesPacket("apply", selectedId));
        }).bounds(listX, actY + 24, 134, 20).build());

        publicAddButton(Button.builder(Component.literal("§7Отключить гайд"), b -> {
            Packets.sendToServer(new GuidesPacket("select", ""));
        }).bounds(listX, actY + 48, 134, 20).build());

        publicAddButton(Button.builder(Component.literal("§e← Меню персонажа"), b ->
            Minecraft.getInstance().gui.setScreen(new MainHubScreen())
        ).bounds(listX, this.guiTop + this.sizeY - 28, 134, 20).build());

        int rightX = this.guiLeft + 150;
        int barY = this.guiTop + this.sizeY - 28;
        publicAddButton(Button.builder(Component.literal("§eТаланты"), b ->
            Minecraft.getInstance().gui.setScreen(new TalentsScreen())
        ).bounds(rightX, barY, 72, 20).build());

        publicAddButton(Button.builder(Component.literal("§bЗаклинания"), b ->
            Minecraft.getInstance().gui.setScreen(new SpellSchoolScreen())
        ).bounds(rightX + 76, barY, 76, 20).build());

        publicAddButton(Button.builder(Component.literal("§aСтаты"), b ->
            Minecraft.getInstance().gui.setScreen(new StatScreen())
        ).bounds(rightX + 156, barY, 66, 20).build());
    }

    @Override
    public void render(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTicks) {
        gui.fill(this.guiLeft, this.guiTop, this.guiLeft + this.sizeX, this.guiTop + this.sizeY, 0xE614141C);
        gui.fill(this.guiLeft, this.guiTop, this.guiLeft + this.sizeX, this.guiTop + 2, 0xFF55CCFF);
        gui.fill(this.guiLeft + 145, this.guiTop + 24, this.guiLeft + 146, this.guiTop + this.sizeY - 8, 0x6055CCFF);

        GuideDefinition sel = GuideAdvisor.getById(selectedId);
        int lvl = Load.Unit(mc.player).getLevel();
        gui.text(mc.font, "§bСистема Билд-Гайдов Age of Epoch", this.guiLeft + 10, this.guiTop + 9, 0xFFFFFFFF, true);
        gui.text(mc.font, "§6" + sel.name, this.guiLeft + 152, this.guiTop + 28, 0xFFFFFFFF, true);
        gui.text(mc.font, "§7Архетип: §f" + sel.tagline, this.guiLeft + 152, this.guiTop + 44, 0xFFCCCCCC, false);
        gui.text(mc.font, "§7" + (sel.tips.isEmpty() ? "" : sel.tips.get(0)), this.guiLeft + 152, this.guiTop + 62, 0xFFAAAAAA, false);

        gui.text(mc.font, "§eБюджет очков на ур. " + lvl + ":", this.guiLeft + 152, this.guiTop + 92, 0xFFFFD24A, false);
        gui.text(mc.font, "§7• Свободно статов: §a" + PlayerPointsType.STATS.getFreePoints(mc.player)
            + " §8(макс: " + GuideMath.statPointsAtLevel(GuideMath.guideMaxLevel()) + ")", this.guiLeft + 152, this.guiTop + 108, 0xFFCCCCCC, false);
        gui.text(mc.font, "§7• Свободно талантов: §e" + PlayerPointsType.TALENTS.getFreePoints(mc.player)
            + " §8| Возвышение: §d" + PlayerPointsType.ASCENDANCY.getFreePoints(mc.player), this.guiLeft + 152, this.guiTop + 122, 0xFFCCCCCC, false);
        gui.text(mc.font, "§7• Свободно заклинаний: §b" + PlayerPointsType.SPELLS.getFreePoints(mc.player)
            + " §8| Пассивки: §3" + PlayerPointsType.PASSIVES.getFreePoints(mc.player), this.guiLeft + 152, this.guiTop + 136, 0xFFCCCCCC, false);

        super.extractRenderState(gui, mouseX, mouseY, partialTicks);
    }
}
