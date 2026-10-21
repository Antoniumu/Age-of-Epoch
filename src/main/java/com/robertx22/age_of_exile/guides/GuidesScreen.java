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
 * Слева — список сборок и кнопки, справа — прогресс, характеристики и талант.
 * Внизу отдельной полосой во всю ширину — заклинания и советы советника:
 * так текст укладывается в 2–3 строки вместо шести и не требует обрезки.
 *
 * Вёрстка подобрана так, чтобы текст никогда не обрезался:
 *  - всё переносится по словам (никаких «…»);
 *  - справочные блоки рисуются кеглем 0.85 — это +18% символов в строке;
 *  - если текста всё-таки больше, чем места (маленький GUI scale или очень
 *    длинный совет), полоса прокручивается колесом, а не режется.
 */
public class GuidesScreen extends BaseScreen implements INamedScreen {

    private static final Identifier PANEL = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/guides_panel.png");
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/icons/guides.png");

    static final int SIZE_X = 360;
    static final int SIZE_Y = 300;

    /** высота строки полным кеглем */
    private static final int LINE = 9;
    /** кегль справочного текста */
    private static final float SMALL = 0.85F;
    /** высота строки мелким кеглем */
    private static final int LINE_S = 8;
    /** кегль советов в левой колонке */
    private static final float TINY = 0.8F;
    private static final int LINE_T = 7;

    // --- геометрия (относительно guiLeft / guiTop)
    private static final int TOP_Y = 26;
    private static final int LEFT_X = 16;
    private static final int LEFT_W = 132;
    private static final int RIGHT_X = 160;
    private static final int RIGHT_W = 186;
    private static final int DIVIDER_X = 152;
    /** низ колонок: ниже начинаются полосы во всю ширину */
    private static final int COLUMNS_BOTTOM = 150;
    private static final int STRIP_X = 16;
    private static final int STRIP_W = SIZE_X - STRIP_X * 2;
    /** полоса «Заклинания» */
    private static final int SPELLS_Y = 156;
    private static final int SPELLS_BOTTOM = 192;
    /** полоса «Советник» / «Советы» */
    private static final int NOTES_Y = 196;
    private static final int NOTES_BOTTOM = SIZE_Y - 34;
    private static final int BAR_Y = SIZE_Y - 30;

    public int selected = 0;

    /** прокрутка нижней полосы, если текст не влез (строки) */
    private int stripScroll = 0;
    private int stripMaxScroll = 0;

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
                    stripScroll = 0;
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

        int barY = guiTop + BAR_Y;
        int x = guiLeft + LEFT_X;

        addButton(new GuideButton(x, barY, 92, 22,
            inSimulation() ? "§6Собрать сборку" : "§8Собрать сборку", b -> {
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
        x += 96;

        addButton(new GuideButton(x, barY, 74, 22, "§eТаланты", b ->
            mc.gui.setScreen(new TalentsScreen()),
            List.of(Component.literal("Открыть Дерево талантов").withStyle(ChatFormatting.YELLOW))));
        x += 78;

        addButton(new GuideButton(x, barY, 74, 22, "§bСпеллы", b ->
            mc.gui.setScreen(new SpellScreen()),
            List.of(Component.literal("Открыть экран заклинаний").withStyle(ChatFormatting.AQUA))));
        x += 78;

        addButton(new GuideButton(x, barY, 70, 22, "§aСтаты", b ->
            mc.gui.setScreen(new CharacterScreen()),
            List.of(Component.literal("Открыть экран персонажа").withStyle(ChatFormatting.GREEN))));
    }

    /** Тултип кнопки сборки: полное название, слоган, статы и все советы гайда. */
    private static List<Component> tooltipFor(GuideDefinition g) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(g.name).withStyle(g.color));
        lines.add(Component.literal(g.tagline).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("§8Статы на 50 ур.: "
            + GuideAdvisor.splitLine(GuideMath.idealSplit(g, GuideMath.statBudget(50)))));
        for (String tip : g.tips) {
            lines.add(Component.literal("§7• " + tip));
        }
        lines.add(Component.literal("§eЛКМ — посмотреть, «Сделать активным» — включить"));
        return lines;
    }

    // ------------------------------------------------------------------ отрисовка
    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        AbstractGui.bindTexture(PANEL);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawPanel(matrix, guiLeft, guiTop, SIZE_X, SIZE_Y);

        GuideDefinition guide = guide();

        full(matrix, "§6ГАЙДЫ §7— готовые сборки", guiLeft + LEFT_X, guiTop + 10);

        if (guide == null) {
            AbstractGui.drawShadow(mc.font, matrix, "§cГайды не загружены",
                guiLeft + RIGHT_X, guiTop + TOP_Y, 0xFFFFFF);
            super.render(matrix, mouseX, mouseY, partialTicks);
            return;
        }

        // разделители колонок и полосы
        fillVertical(matrix, guiLeft + DIVIDER_X, guiTop + TOP_Y - 2, guiTop + COLUMNS_BOTTOM);
        fillHorizontal(matrix, guiTop + COLUMNS_BOTTOM + 2, guiLeft + LEFT_X, guiLeft + SIZE_X - LEFT_X);

        renderDetails(matrix, guide);
        renderLeftNote(matrix, guide);
        renderSpells(matrix, guide);
        renderNotes(matrix, guide);

        super.render(matrix, mouseX, mouseY, partialTicks);
        buttons.forEach(b -> b.renderToolTip(matrix, mouseX, mouseY));
    }

    private void fillVertical(PoseStack matrix, int x, int y0, int y1) {
        AbstractGui.fill(matrix, x, y0, x + 1, y1, 0x40FFFFFF);
    }

    private void fillHorizontal(PoseStack matrix, int y, int x0, int x1) {
        AbstractGui.fill(matrix, x0, y, x1, y + 1, 0x40FFFFFF);
    }

    /** Правая колонка: прогресс, характеристики, следующий талант. */
    private void renderDetails(PoseStack matrix, GuideDefinition guide) {
        EntityData unitData = Load.Unit(mc.player);
        int level = unitData.getLevel();
        GuideAdvisor.Progress progress = GuideAdvisor.progress(mc.player);

        int x = guiLeft + RIGHT_X;
        int y = guiTop + TOP_Y;
        int bottom = guiTop + COLUMNS_BOTTOM;

        AbstractGui.drawShadow(mc.font, matrix, "§l" + guide.name, x, y,
            CompatChatFormatting.getColor(guide.color));
        y += LINE + 3;

        for (String row : wrap(guide.tagline, RIGHT_W, SMALL)) {
            if (y + LINE_S > bottom) {
                return;
            }
            small(matrix, row, x, y);
            y += LINE_S;
        }
        y += 2;

        // прогресс одной строкой
        small(matrix, "§7Таланты §f" + progress.planTalentsTaken + "§7/§f" + progress.planTalentsTotal
            + " §7• Статы §f" + progress.statSpent + "§7/§f" + progress.statBudget
            + " §7• Спеллы §f" + progress.spellSpent + "§7/§f" + progress.spellBudget, x, y);
        y += LINE_S + 2;

        String milestone = GuideAdvisor.milestoneText(mc.player, guide);
        if (!milestone.isEmpty()) {
            for (String row : wrap("§6Сейчас: §7" + milestone, RIGHT_W, SMALL)) {
                if (y + LINE_S > bottom) {
                    return;
                }
                small(matrix, row, x, y);
                y += LINE_S;
            }
            y += 3;
        }

        small(matrix, "§eХарактеристики §7сейчас/цель", x, y);
        y += LINE_S;
        for (String statLine : GuideAdvisor.statPlanLines(mc.player, guide)) {
            if (y + LINE_S > bottom) {
                return;
            }
            small(matrix, "§f" + statLine, x + 4, y);
            y += LINE_S;
        }

        y += 2;
        if (y + LINE_S > bottom) {
            return;
        }
        small(matrix, "§eТаланты", x, y);
        y += LINE_S;
        for (String row : wrap(talentLine(guide, level, progress), RIGHT_W - 4, SMALL)) {
            if (y + LINE_S > bottom) {
                return;
            }
            small(matrix, row, x + 4, y);
            y += LINE_S;
        }
    }

    /** Левая колонка под кнопками: где собирается билд и сколько узлов вне плана. */
    private void renderLeftNote(PoseStack matrix, GuideDefinition guide) {
        int x = guiLeft + LEFT_X;
        int y = guiTop + TOP_Y + GuideData.all().size() * 22 + 6 + 22 + 22 + 10;
        int bottom = guiTop + COLUMNS_BOTTOM;

        if (!inSimulation() && y + LINE_T <= bottom) {
            tiny(matrix, "§8Сборка — на Полигоне", x, y);
            y += LINE_T + 3;
        }

        GuideAdvisor.Progress progress = GuideAdvisor.progress(mc.player);
        if (progress == null || y + LINE_T > bottom) {
            return;
        }
        String state = progress.startCompatible
            ? "§7Узлов плана: §f" + progress.planTalentsTaken + "§7/§f" + progress.planTalentsTotal
            : "§cСтартовый путь другой";
        tiny(matrix, state, x, y);
    }

    /** Полоса «Заклинания» во всю ширину — короткие строки вместо узкой колонки. */
    private void renderSpells(PoseStack matrix, GuideDefinition guide) {
        int x = guiLeft + STRIP_X;
        int y = guiTop + SPELLS_Y;
        int bottom = guiTop + SPELLS_BOTTOM;

        if (y + LINE_S > bottom) {
            return;
        }
        small(matrix, "§eЗаклинания §7(уровень / цель)", x, y);
        y += LINE_S;
        for (String row : wrap(spellLine(guide), STRIP_W, SMALL)) {
            if (y + LINE_S > bottom) {
                return;
            }
            small(matrix, "§f" + row, x + 4, y);
            y += LINE_S;
        }
    }

    /**
     * Полоса «Советник» (или «Советы», если советнику нечего сказать).
     * Текст переносится целиком; если строк больше, чем места — прокрутка колесом.
     */
    private void renderNotes(PoseStack matrix, GuideDefinition guide) {
        int x = guiLeft + STRIP_X;
        int top = guiTop + NOTES_Y;
        int bottom = guiTop + NOTES_BOTTOM;

        List<Row> rows = new ArrayList<>();
        GuideAdvisor.Progress progress = GuideAdvisor.progress(mc.player);
        boolean hasAdvice = progress != null && !progress.advice.isEmpty();

        if (hasAdvice) {
            rows.add(new Row("§6Советник", 0));
            for (String advice : progress.advice) {
                for (String row : wrap(advice, STRIP_W - 4, SMALL)) {
                    rows.add(new Row(row, 4));
                }
            }
        } else {
            rows.add(new Row("§6Советы по сборке", 0));
            for (String tip : guide.tips) {
                for (String row : wrap("§7• " + tip, STRIP_W - 4, SMALL)) {
                    rows.add(new Row(row, 4));
                }
            }
        }

        int available = (bottom - top) / LINE_S;
        stripMaxScroll = Math.max(0, rows.size() - available);
        stripScroll = Math.max(0, Math.min(stripScroll, stripMaxScroll));

        int y = top;
        for (int i = stripScroll; i < rows.size(); i++) {
            if (y + LINE_S > bottom) {
                break;
            }
            Row row = rows.get(i);
            if (!row.text.isEmpty()) {
                small(matrix, row.text, x + row.indent, y);
            }
            y += LINE_S;
        }

        if (stripMaxScroll > 0) {
            int trackHeight = bottom - top;
            int thumb = Math.max(10, trackHeight * available / rows.size());
            int thumbY = top + (trackHeight - thumb) * stripScroll / stripMaxScroll;
            int sx = guiLeft + SIZE_X - 12;
            AbstractGui.fill(matrix, sx, top, sx + 2, top + trackHeight, 0x40000000);
            AbstractGui.fill(matrix, sx, thumbY, sx + 2, thumbY + thumb, 0x80FFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (stripMaxScroll > 0) {
            stripScroll = Math.max(0, Math.min(stripMaxScroll, stripScroll - (int) Math.signum(scroll) * 2));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scroll);
    }

    private record Row(String text, int indent) {
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

    // ------------------------------------------------------------------ панель
    /** Размер текстуры панели (assets/mmorpg/textures/gui/guides_panel.png). */
    private static final int PANEL_TEX_W = 360;
    private static final int PANEL_TEX_H = 300;

    /**
     * Панель целиком одним blit'ом.
     *
     * Размер атласа передаём явно: короткий blit(x, y, u, v, w, h) считает текстуру
     * 256×256, а наша — 360×280. Иначе правые пиксели сэмплируются за краем
     * текстуры с заворотом на её начало и справа появляется вторая рамка.
     */
    static void drawPanel(PoseStack matrix, int x, int y, int width, int height) {
        AbstractGui.blit(matrix, x, y, width, height, 0F, 0F, width, height, PANEL_TEX_W, PANEL_TEX_H);
    }

    // ------------------------------------------------------------------ текст
    /** Строка полным кеглем. */
    private void full(PoseStack matrix, String text, int x, int y) {
        AbstractGui.drawShadow(mc.font, matrix, text, x, y, 0xFFFFFF);
    }

    /** Строка мелким кеглем (справочные блоки). */
    private void small(PoseStack matrix, String text, int x, int y) {
        scaled(matrix, text, x, y, SMALL);
    }

    /** Строка самым мелким кеглем (советы в левой колонке). */
    private void tiny(PoseStack matrix, String text, int x, int y) {
        scaled(matrix, text, x, y, TINY);
    }

    /**
     * Текст в масштабе. Цвет берётся из §-кодов внутри строки, поэтому одним
     * вызовом можно рисовать разноцветный текст.
     */
    private void scaled(PoseStack matrix, String text, int x, int y, float scale) {
        if (text == null || text.isEmpty()) {
            return;
        }
        matrix.pushPose();
        matrix.scale(scale, scale, 1F);
        AbstractGui.drawShadow(mc.font, matrix, text, x / scale, y / scale, 0xFFFFFF);
        matrix.popPose();
    }

    /**
     * Перенос по словам БЕЗ обрезки: возвращает все строки целиком.
     * Ширина учитывается с поправкой на масштаб (текст рисуется в scale раз меньше).
     */
    private List<String> wrap(String text, int maxWidth, float scale) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        float limit = maxWidth / scale;
        for (String paragraph : text.split("\n")) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String probe = current.length() == 0 ? word : current + " " + word;
                if (mc.font.width(probe) > limit && current.length() > 0) {
                    out.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(probe);
                }
            }
            if (current.length() > 0) {
                out.add(current.toString());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ кнопка
    /** Кнопка с фоном, текстом в одну строку и тултипом. */
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
            Minecraft mc = Minecraft.getInstance();
            String text = label;
            // если название не влезает — уменьшаем кегль, но не режем текст
            float scale = 1F;
            while (mc.font.width(text) * scale > this.width - 8 && scale > 0.6F) {
                scale -= 0.05F;
            }
            PoseStack pose = matrix;
            pose.pushPose();
            pose.scale(scale, scale, 1F);
            AbstractGui.drawShadow(mc.font, pose, text,
                (this.x + 4) / scale, (this.y + (this.height - 8) / 2F) / scale, 0xFFFFFF);
            pose.popPose();
        }

        @Override
        public void renderToolTip(PoseStack matrix, int mouseX, int mouseY) {
            if (isHovered() && !tooltip.isEmpty()) {
                GuiUtils.renderTooltip(matrix, tooltip, mouseX, mouseY);
            }
        }
    }
}
