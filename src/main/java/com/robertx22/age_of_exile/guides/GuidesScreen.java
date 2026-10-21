package com.robertx22.age_of_exile.guides;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.capability.player.EntitySpellCap;
import com.robertx22.age_of_exile.database.data.perks.Perk;
import com.robertx22.age_of_exile.database.data.talent_tree.TalentTree;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.gui.bases.INamedScreen;
import com.robertx22.age_of_exile.gui.screens.character_screen.CharacterScreen;
import com.robertx22.age_of_exile.gui.screens.skill_tree.TalentsScreen;
import com.robertx22.age_of_exile.gui.screens.spell.SpellScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.PointData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.forge_compat.common.CompatChatFormatting;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Вкладка «Гайды».
 *
 * Слева — список готовых сборок, справа — что именно качать:
 * характеристики, таланты, заклинания и советы по отклонениям.
 * Кнопка «Собрать» работает только на Полигоне и собирает билд целиком.
 */
public class GuidesScreen extends BaseScreen implements INamedScreen {

    private static final Identifier PANEL = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/full_stats_panel.png");
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/icons/guides.png");

    static final int SIZE_X = 256;
    static final int SIZE_Y = 180;

    public int selected = 0;

    public GuidesScreen() {
        super(SIZE_X, SIZE_Y);
        String active = GuideAdvisor.activeGuideId(Minecraft.getInstance().player);
        for (int i = 0; i < GuideData.all().size(); i++) {
            if (GuideData.all().get(i).id.equals(active)) {
                selected = i;
            }
        }
    }

    @Override
    public Identifier iconLocation() {
        return ICON;
    }

    @Override
    public Words screenName() {
        return Words.Guides;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private GuideDefinition guide() {
        List<GuideDefinition> all = GuideData.all();
        if (all.isEmpty()) {
            return null;
        }
        return all.get(Math.max(0, Math.min(all.size() - 1, selected)));
    }

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.children.clear();

        int listX = guiLeft + 8;
        int listY = guiTop + 24;
        String activeId = GuideAdvisor.activeGuideId(mc.player);

        for (int i = 0; i < GuideData.all().size(); i++) {
            final int index = i;
            GuideDefinition g = GuideData.all().get(i);
            boolean active = g.id.equals(activeId);
            String prefix = active ? "§a● " : (index == selected ? "§e> " : "§7○ ");
            addButton(new GuideButton(listX, listY + i * 22, 106, 20, prefix + g.name,
                b -> {
                    selected = index;
                    this.init();
                },
                tooltipFor(g)));
        }

        int rowA = guiTop + SIZE_Y - 44;
        int rowB = guiTop + SIZE_Y - 22;

        addButton(new GuideButton(listX, rowA, 106, 20, "§bСделать активным", b -> {
            GuideDefinition g = guide();
            if (g != null) {
                Packets.sendToServer(GuidesPacket.select(g.id));
            }
        }, List.of(Component.literal("Включить подсказки гайда").withStyle(ChatFormatting.AQUA),
            Component.literal("Кнопки характеристик, талантов и заклинаний")
                .withStyle(ChatFormatting.GRAY),
            Component.literal("начнут подсвечивать следующий шаг.")
                .withStyle(ChatFormatting.GRAY))));

        addButton(new GuideButton(listX, rowB, 106, 20, "§7Отключить гайд", b ->
            Packets.sendToServer(GuidesPacket.clear()),
            List.of(Component.literal("Снять подсветку").withStyle(ChatFormatting.GRAY))));

        int rightX = guiLeft + 118;

        addButton(new GuideButton(rightX, rowA, 62, 20, "§6Собрать", b -> {
            GuideDefinition g = guide();
            if (g != null) {
                Packets.sendToServer(GuidesPacket.applyInSimulation(g.id, true));
                this.onClose();
            }
        }, List.of(Component.literal("Собрать сборку целиком").withStyle(ChatFormatting.GOLD),
            Component.literal("Только на Полигоне: сбрасывает билд,")
                .withStyle(ChatFormatting.GRAY),
            Component.literal("вкладывает статы, таланты, заклинания")
                .withStyle(ChatFormatting.GRAY),
            Component.literal("и выдаёт экипировку под гайд.")
                .withStyle(ChatFormatting.GRAY))));

        addButton(new GuideButton(rightX + 66, rowA, 62, 20, "§eТаланты", b ->
            mc.gui.setScreen(new TalentsScreen()),
            List.of(Component.literal("Открыть Дерево талантов").withStyle(ChatFormatting.YELLOW))));

        addButton(new GuideButton(rightX, rowB, 62, 20, "§bЗаклинан.", b ->
            mc.gui.setScreen(new SpellScreen()),
            List.of(Component.literal("Открыть экран заклинаний").withStyle(ChatFormatting.AQUA))));

        addButton(new GuideButton(rightX + 66, rowB, 62, 20, "§aСтаты", b ->
            mc.gui.setScreen(new CharacterScreen()),
            List.of(Component.literal("Открыть экран персонажа").withStyle(ChatFormatting.GREEN))));
    }

    /** Тултип кнопки сборки: название, слоган и ключевые советы гайда. */
    private static List<Component> tooltipFor(GuideDefinition g) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(g.name).withStyle(g.color));
        lines.add(Component.literal(g.tagline).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("§8Статы на 50 ур.: "
            + GuideAdvisor.splitLine(GuideMath.idealSplit(g, GuideMath.statBudget(50)))));
        for (int i = 0; i < g.tips.size() && i < 2; i++) {
            lines.add(Component.literal("§7• " + g.tips.get(i)));
        }
        lines.add(Component.literal("§eЛКМ — посмотреть, «Сделать активным» — включить").withStyle(ChatFormatting.YELLOW));
        return lines;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        AbstractGui.bindTexture(PANEL);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, guiLeft, guiTop, 0, 0, SIZE_X, SIZE_Y);

        Minecraft mc = Minecraft.getInstance();
        GuideDefinition guide = guide();

        AbstractGui.drawShadow(mc.font, matrix, "§6ГАЙДЫ — готовые сборки", guiLeft + 8, guiTop + 8, 0xFFFFFF);

        if (guide == null) {
            AbstractGui.drawShadow(mc.font, matrix, "§cГайды не загружены", guiLeft + 118, guiTop + 30, 0xFFFFFF);
            super.render(matrix, mouseX, mouseY, partialTicks);
            return;
        }

        EntityData unitData = Load.Unit(mc.player);
        int level = unitData.getLevel();
        GuideAdvisor.Progress progress = GuideAdvisor.progress(mc.player);

        int x = guiLeft + 118;
        int y = guiTop + 22;
        int bottom = guiTop + 132;

        AbstractGui.drawShadow(mc.font, matrix, "§l" + guide.name, x, y,
            CompatChatFormatting.getColor(guide.color));
        y += 10;
        y = drawWrapped(matrix, guide.tagline, x, y, 130, 0xA0A0A0) + 1;

        y = drawWrapped(matrix, "§7Узлов плана: §f" + progress.planTalentsTaken + "/" + progress.planTalentsTotal
            + " §7| статы §f" + progress.statSpent + "/" + progress.statBudget
            + " §7| заклинания §f" + progress.spellSpent + "/" + progress.spellBudget,
            x, y, 130, 0xC0C0C0) + 2;

        // веха по уровню — «что делать на этом отрезке»
        String milestone = GuideAdvisor.milestoneText(mc.player, guide);
        if (!milestone.isEmpty()) {
            y = drawWrapped(matrix, "§6Сейчас: §7" + milestone, x, y, 130, 0xC0C0C0) + 2;
        }

        AbstractGui.drawShadow(mc.font, matrix, "§eХарактеристики §7(сейчас/цель)", x, y, 0xFFFFFF);
        y += 9;
        for (String line : GuideAdvisor.statPlanLines(mc.player, guide)) {
            AbstractGui.drawShadow(mc.font, matrix, "§f" + line, x + 4, y, 0xFFFFFF);
            y += 9;
            if (y > bottom - 32) {
                break;
            }
        }

        AbstractGui.drawShadow(mc.font, matrix, "§eТаланты", x, y, 0xFFFFFF);
        y += 9;
        PointData nextTalent = GuideAdvisor.recommendedTalent(mc.player);
        String talentLine;
        if (!progress.startCompatible) {
            talentLine = "§cДругой стартовый путь — план недостижим";
        } else if (nextTalent == null) {
            talentLine = level < 50 ? "§7Ждите уровень или очки" : "§7План изучен";
        } else {
            int index = guide.indexOfPoint(nextTalent);
            TalentTree tree = GuideAdvisor.talentTree();
            Perk perk = tree != null ? tree.calcData.getPerk(nextTalent) : null;
            String name = perk != null ? perk.locName().getString() : nextTalent.toString();
            talentLine = (index >= 0 ? "§a№" + (index + 1) + " §f" : "§6путь §f") + name;
        }
        AbstractGui.drawShadow(mc.font, matrix, talentLine, x + 4, y, 0xFFFFFF);
        y += 9;

        AbstractGui.drawShadow(mc.font, matrix, "§eЗаклинания", x, y, 0xFFFFFF);
        y += 9;
        StringBuilder spellLine = new StringBuilder();
        EntitySpellCap.ISpellsCap spells = Load.spells(mc.player);
        for (GuideDefinition.SpellStep step : guide.spells) {
            int lvl = spells.getLevelOf(step.spell);
            if (spellLine.length() > 0) {
                spellLine.append("§7, ");
            }
            spellLine.append(lvl >= step.targetLevel ? "§a" : "§7")
                .append(GuideAdvisor.spellName(step.spell)).append(" ").append(lvl)
                .append("/").append(step.targetLevel);
        }
        y = drawWrapped(matrix, spellLine.toString(), x + 4, y, 128, 0xFFFFFF) + 2;

        if (!progress.advice.isEmpty() && y < bottom - 10) {
            AbstractGui.drawShadow(mc.font, matrix, "§6Советник", x, y, 0xFFFFFF);
            y += 9;
            int shown = 0;
            for (String advice : progress.advice) {
                if (y > bottom) {
                    break;
                }
                y = drawWrapped(matrix, advice, x + 4, y, 128, 0xFFFFFF) + 1;
                if (++shown >= 2) {
                    break;
                }
            }
        }

        if (mc.player != null && !WorldUtils.isSimulationWorld(mc.player.level())) {
            AbstractGui.drawShadow(mc.font, matrix, "§8«Собрать» — только на Полигоне",
                x, guiTop + SIZE_Y - 56, 0x808080);
        }

        super.render(matrix, mouseX, mouseY, partialTicks);
        buttons.forEach(b -> b.renderToolTip(matrix, mouseX, mouseY));
    }

    /** Рисует текст с переносом строк, возвращает Y следующей строки. */
    private int drawWrapped(PoseStack matrix, String text, int x, int y, int maxWidth, int color) {
        Minecraft mc = Minecraft.getInstance();
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String probe = line.length() == 0 ? word : line + " " + word;
                if (mc.font.width(probe) > maxWidth && line.length() > 0) {
                    AbstractGui.drawShadow(mc.font, matrix, line.toString(), x, y, color);
                    y += 9;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(probe);
                }
            }
            if (line.length() > 0) {
                AbstractGui.drawShadow(mc.font, matrix, line.toString(), x, y, color);
                y += 9;
            }
        }
        return y;
    }

    /** Кнопка с фоном, текстом и тултипом. */
    private static class GuideButton extends Button {
        private final String label;
        private final List<Component> tooltip;

        public GuideButton(int x, int y, int w, int h, String label,
                           net.minecraft.client.gui.components.Button.OnPress onPress,
                           List<Component> tooltip) {
            super(x, y, w, h, Component.literal(label), onPress);
            this.label = label;
            this.tooltip = tooltip == null ? new ArrayList<>() : tooltip;
        }

        @Override
        public void renderButton(PoseStack matrix, int mouseX, int mouseY, float delta) {
            int color = isHovered() ? 0x804A2A08 : 0x60201008;
            AbstractGui.fill(matrix, this.x, this.y, this.x + this.width, this.y + this.height, color);
            AbstractGui.drawShadow(Minecraft.getInstance().font, matrix, label,
                this.x + 4, this.y + (this.height - 8) / 2, 0xFFFFFF);
        }

        @Override
        public void renderToolTip(PoseStack matrix, int mouseX, int mouseY) {
            if (isHovered() && !tooltip.isEmpty()) {
                GuiUtils.renderTooltip(matrix, tooltip, mouseX, mouseY);
            }
        }
    }
}
