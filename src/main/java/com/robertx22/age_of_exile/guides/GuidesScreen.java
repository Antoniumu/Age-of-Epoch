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
 * Слева — список сборок и советы советника, справа — что именно качать:
 * характеристики, таланты, заклинания. Кнопка «Собрать» работает только
 * на Полигоне и собирает билд целиком.
 *
 * Вёрстка: панель 300×220 под отдельную текстуру guides_panel.png
 * (та же рамка 8 px, что у full_stats_panel.png, но без тайлинга — иначе на
 * стыках полос видны швы). Любой текст режется по ширине и по нижней границе
 * области — за панель ничего не выезжает, длинные строки получают «…».
 */
public class GuidesScreen extends BaseScreen implements INamedScreen {

    private static final Identifier PANEL = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/guides_panel.png");
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/icons/guides.png");

    static final int SIZE_X = 300;
    static final int SIZE_Y = 220;

    private static final int LINE = 9;

    // --- геометрия колонок (относительно guiLeft / guiTop)
    private static final int LEFT_X = 16;
    private static final int LEFT_W = 126;
    private static final int RIGHT_X = 150;
    private static final int RIGHT_W = 134;
    private static final int TOP_Y = 24;
    private static final int BAR_Y = SIZE_Y - 30;

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

    private boolean inSimulation() {
        return mc.player != null && WorldUtils.isSimulationWorld(mc.player.level());
    }

    // ------------------------------------------------------------------ кнопки
    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.children.clear();

        int listX = guiLeft + LEFT_X;
        int listY = guiTop + TOP_Y;
        String activeId = GuideAdvisor.activeGuideId(mc.player);

        for (int i = 0; i < GuideData.all().size(); i++) {
            final int index = i;
            GuideDefinition g = GuideData.all().get(i);
            boolean active = g.id.equals(activeId);
            String prefix = active ? "§a● " : (index == selected ? "§e> " : "§7○ ");
            addButton(new GuideButton(listX, listY + i * 22, LEFT_W, 20, prefix + g.name,
                b -> {
                    selected = index;
                    this.init();
                },
                tooltipFor(g)));
        }

        int actionY = listY + GuideData.all().size() * 22 + 6;

        addButton(new GuideButton(listX, actionY, LEFT_W, 20, "§bСделать активным", b -> {
            GuideDefinition g = guide();
            if (g != null) {
                Packets.sendToServer(GuidesPacket.select(g.id));
            }
        }, List.of(Component.literal("Включить подсказки гайда").withStyle(ChatFormatting.AQUA),
            Component.literal("Кнопки характеристик, талантов и заклинаний")
                .withStyle(ChatFormatting.GRAY),
            Component.literal("начнут подсвечивать следующий шаг.")
                .withStyle(ChatFormatting.GRAY))));

        addButton(new GuideButton(listX, actionY + 22, LEFT_W, 20, "§7Отключить гайд", b ->
            Packets.sendToServer(GuidesPacket.clear()),
            List.of(Component.literal("Снять подсветку").withStyle(ChatFormatting.GRAY))));

        // нижняя полоса: собрать / перейти к талантам, заклинаниям, статам
        int barY = guiTop + BAR_Y;
        int x = guiLeft + LEFT_X;

        addButton(new GuideButton(x, barY, 72, 22,
            inSimulation() ? "§6Собрать" : "§8Собрать", b -> {
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
        x += 76;

        addButton(new GuideButton(x, barY, 58, 22, "§eТаланты", b ->
            mc.gui.setScreen(new TalentsScreen()),
            List.of(Component.literal("Открыть Дерево талантов").withStyle(ChatFormatting.YELLOW))));
        x += 62;

        addButton(new GuideButton(x, barY, 60, 22, "§bСпеллы", b ->
            mc.gui.setScreen(new SpellScreen()),
            List.of(Component.literal("Открыть экран заклинаний").withStyle(ChatFormatting.AQUA))));
        x += 64;

        addButton(new GuideButton(x, barY, 56, 22, "§aСтаты", b ->
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

    // ------------------------------------------------------------------ отрисовка
    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        AbstractGui.bindTexture(PANEL);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawPanel(matrix, guiLeft, guiTop, SIZE_X, SIZE_Y);

        GuideDefinition guide = guide();

        AbstractGui.drawShadow(mc.font, matrix, "§6ГАЙДЫ §7— готовые сборки",
            guiLeft + LEFT_X, guiTop + 10, 0xFFFFFF);

        if (guide == null) {
            AbstractGui.drawShadow(mc.font, matrix, "§cГайды не загружены",
                guiLeft + RIGHT_X, guiTop + TOP_Y, 0xFFFFFF);
            super.render(matrix, mouseX, mouseY, partialTicks);
            return;
        }

        // разделитель колонок
        int dividerX = guiLeft + RIGHT_X - 6;
        AbstractGui.fill(matrix, dividerX, guiTop + TOP_Y - 2, dividerX + 1,
            guiTop + BAR_Y - 6, 0x40FFFFFF);

        renderDetails(matrix, guide);
        renderAdvice(matrix, guide);

        super.render(matrix, mouseX, mouseY, partialTicks);
        buttons.forEach(b -> b.renderToolTip(matrix, mouseX, mouseY));
    }

    /** Правая колонка: что качать. Строго до нижней полосы с кнопками. */
    private void renderDetails(PoseStack matrix, GuideDefinition guide) {
        EntityData unitData = Load.Unit(mc.player);
        int level = unitData.getLevel();
        GuideAdvisor.Progress progress = GuideAdvisor.progress(mc.player);

        int x = guiLeft + RIGHT_X;
        int y = guiTop + TOP_Y;
        int contentBottom = guiTop + BAR_Y - 8;

        AbstractGui.drawShadow(mc.font, matrix, clip("§l" + guide.name, RIGHT_W), x, y,
            CompatChatFormatting.getColor(guide.color));
        y += LINE + 2;

        // слоган — одна строка, длинный обрезается с «…»
        y = wrap(matrix, guide.tagline, x, y, RIGHT_W, y + LINE, 0xA0A0A0);

        // прогресс: две короткие строки вместо одной «простыни»
        line(matrix, "§7Таланты: §f" + progress.planTalentsTaken + "§7/§f" + progress.planTalentsTotal,
            x, y, RIGHT_W, 0xFFFFFF);
        y += LINE;
        line(matrix, "§7Статы: §f" + progress.statSpent + "§7/§f" + progress.statBudget
            + " §7| Спеллы: §f" + progress.spellSpent + "§7/§f" + progress.spellBudget,
            x, y, RIGHT_W, 0xFFFFFF);
        y += LINE;

        // веха по уровню — одна строка
        String milestone = GuideAdvisor.milestoneText(mc.player, guide);
        if (!milestone.isEmpty()) {
            y = wrap(matrix, "§6Сейчас: §7" + milestone, x, y, RIGHT_W, y + LINE, 0xC0C0C0);
        }
        y += 2;

        if (y + LINE > contentBottom) {
            return;
        }

        line(matrix, "§eХарактеристики §7сейчас/цель", x, y, RIGHT_W, 0xFFFFFF);
        y += LINE;
        for (String statLine : GuideAdvisor.statPlanLines(mc.player, guide)) {
            if (y + LINE > contentBottom) {
                return;
            }
            line(matrix, "§f" + statLine, x + 4, y, RIGHT_W - 4, 0xFFFFFF);
            y += LINE;
        }

        if (y + LINE > contentBottom) {
            return;
        }
        line(matrix, "§eТаланты", x, y, RIGHT_W, 0xFFFFFF);
        y += LINE;
        if (y + LINE <= contentBottom) {
            line(matrix, talentLine(guide, level, progress), x + 4, y, RIGHT_W - 4, 0xFFFFFF);
            y += LINE;
        }

        if (y + LINE > contentBottom) {
            return;
        }
        line(matrix, "§eЗаклинания", x, y, RIGHT_W, 0xFFFFFF);
        y += LINE;
        wrap(matrix, spellLine(guide), x + 4, y, RIGHT_W - 4, contentBottom, 0xFFFFFF);
    }

    private String talentLine(GuideDefinition guide, int level, GuideAdvisor.Progress progress) {
        if (!progress.startCompatible) {
            return "§cДругой стартовый путь — план недостижим";
        }
        PointData nextTalent = GuideAdvisor.recommendedTalent(mc.player);
        if (nextTalent == null) {
            return level < 50 ? "§7Ждите уровень или очки" : "§7План изучен";
        }
        int index = guide.indexOfPoint(nextTalent);
        TalentTree tree = GuideAdvisor.talentTree();
        Perk perk = tree != null ? tree.calcData.getPerk(nextTalent) : null;
        String name = perk != null ? perk.locName().getString() : nextTalent.toString();
        return (index >= 0 ? "§a№" + (index + 1) + " §f" : "§6путь §f") + name;
    }

    private String spellLine(GuideDefinition guide) {
        StringBuilder sb = new StringBuilder();
        EntitySpellCap.ISpellsCap spells = Load.spells(mc.player);
        for (GuideDefinition.SpellStep step : guide.spells) {
            int lvl = spells.getLevelOf(step.spell);
            if (sb.length() > 0) {
                sb.append("§7, ");
            }
            sb.append(lvl >= step.targetLevel ? "§a" : "§7")
                .append(GuideAdvisor.spellName(step.spell)).append(" ").append(lvl)
                .append("/").append(step.targetLevel);
        }
        return sb.toString();
    }

    /** Левая колонка под кнопками: советы советника (откат талантов и т.п.). */
    private void renderAdvice(PoseStack matrix, GuideDefinition guide) {
        GuideAdvisor.Progress progress = GuideAdvisor.progress(mc.player);

        int x = guiLeft + LEFT_X;
        int y = guiTop + TOP_Y + GuideData.all().size() * 22 + 6 + 22 + 22 + 12;
        int contentBottom = guiTop + BAR_Y - 6;

        if (y + LINE > contentBottom) {
            return;
        }

        if (!inSimulation()) {
            line(matrix, "§8Сборка — на Полигоне", x, y, LEFT_W, 0x808080);
            y += LINE + 2;
        }

        if (progress == null || progress.advice.isEmpty() || y + LINE > contentBottom) {
            return;
        }

        line(matrix, "§6Советник", x, y, LEFT_W, 0xFFFFFF);
        y += LINE;

        int lines = 1;
        for (String advice : progress.advice) {
            if (y + LINE > contentBottom) {
                break;
            }
            y = wrap(matrix, advice, x, y, LEFT_W, contentBottom, 0xC0C0C0) + 1;
            if (++lines > 4) {
                break;
            }
        }
    }

    // ------------------------------------------------------------------ панель
    /** Панель целиком одним blit'ом: текстура ровно 300×220, швов нет. */
    static void drawPanel(PoseStack matrix, int x, int y, int width, int height) {
        AbstractGui.blit(matrix, x, y, 0, 0, width, height);
    }

    // ------------------------------------------------------------------ текст
    /** Одна строка, обрезанная по ширине с «…». */
    private void line(PoseStack matrix, String text, int x, int y, int maxWidth, int color) {
        AbstractGui.drawShadow(mc.font, matrix, clip(text, maxWidth), x, y, color);
    }

    /**
     * Перенос по словам с жёсткими границами: не шире maxWidth и не ниже maxY.
     * Если текст не влез — последняя строка обрезается и получает «…».
     * Возвращает Y следующей строки.
     */
    private int wrap(PoseStack matrix, String text, int x, int y, int maxWidth, int maxY, int color) {
        if (text == null || text.isEmpty() || y + LINE > maxY) {
            return y;
        }

        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String probe = current.length() == 0 ? word : current + " " + word;
                if (mc.font.width(probe) > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(probe);
                }
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            if (y + LINE > maxY) {
                return y;
            }
            boolean last = i == lines.size() - 1;
            // есть ли ещё место хотя бы на одну строку после этой
            boolean roomBelow = y + LINE * 2 <= maxY;
            String row = lines.get(i);
            if (!last && !roomBelow) {
                AbstractGui.drawShadow(mc.font, matrix, clip(row, maxWidth - 6) + "…", x, y, color);
                return y + LINE;
            }
            AbstractGui.drawShadow(mc.font, matrix, clip(row, maxWidth), x, y, color);
            y += LINE;
        }
        return y;
    }

    /** Обрезает строку по фактической ширине шрифта, сохраняя §-коды целиком. */
    static String clip(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.font.width(text) <= maxWidth) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            String chunk;
            if (text.charAt(i) == '§' && i + 1 < text.length()) {
                chunk = text.substring(i, i + 2);
                i += 2;
            } else {
                chunk = String.valueOf(text.charAt(i));
                i++;
            }
            if (mc.font.width(sb.toString() + chunk + "…") > maxWidth) {
                break;
            }
            sb.append(chunk);
        }
        return sb + "…";
    }

    // ------------------------------------------------------------------ кнопка
    /** Кнопка с фоном, текстом в одну строку (с обрезкой) и тултипом. */
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
            int color = isHovered() ? 0x90503008 : 0x60201008;
            AbstractGui.fill(matrix, this.x, this.y, this.x + this.width, this.y + this.height, color);
            if (isHovered()) {
                AbstractGui.fill(matrix, this.x, this.y, this.x + this.width, this.y + 1, 0x60FFD24A);
                AbstractGui.fill(matrix, this.x, this.y + this.height - 1,
                    this.x + this.width, this.y + this.height, 0x60FFD24A);
            }
            AbstractGui.drawShadow(Minecraft.getInstance().font, matrix,
                clip(label, this.width - 8),
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
