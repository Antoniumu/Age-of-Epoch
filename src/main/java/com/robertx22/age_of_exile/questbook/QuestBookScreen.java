package com.robertx22.age_of_exile.questbook;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.database.data.unique_items.UniqueGear;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.guides.GuideData;
import com.robertx22.age_of_exile.guides.GuideDefinition;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.gui.bases.INamedScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.GemItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.RuneItem;
import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.age_of_exile.questbook.QuestBookCatalog.Chapter;
import com.robertx22.age_of_exile.questbook.QuestBookCatalog.Objective;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.GuiUtils;
import com.robertx22.world_of_exile.tower.TowerBossDef;
import com.robertx22.world_of_exile.tower.TowerRoster;
import com.robertx22.world_of_exile.tower.TowerTheme;
import com.robertx22.world_of_exile.tower.TowerThemes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** In-game, scrollable campaign guide, collection ledgers, and optional Platinum challenge. */
public class QuestBookScreen extends BaseScreen implements INamedScreen {

    private static final Identifier PANEL = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/guides_panel.png");
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/icons/questbook.png");
    private static final int PANEL_TEXTURE_WIDTH = 360;
    private static final int PANEL_TEXTURE_HEIGHT = 300;
    private static final int SIZE_X = 360;
    private static final int SIZE_Y = 300;

    private static final int NAV_X = 10;
    private static final int NAV_Y = 34;
    private static final int NAV_WIDTH = 126;
    private static final int NAV_ROW_HEIGHT = 17;
    private static final int BODY_X = 143;
    private static final int BODY_WIDTH = 204;
    private static final int BODY_TOP = 37;
    private static final int BODY_BOTTOM = 275;
    private static final int BODY_LINE = 9;
    private static final int OBJECTIVE_HEIGHT = 21;
    private static final float BODY_SCALE = 0.86F;

    private int selectedChapter = 0;
    private int scrollPixels = 0;
    private int refreshTicks = 0;
    private boolean refreshRequested = false;
    private List<PageRow> pageRows = List.of();

    public QuestBookScreen() {
        super(SIZE_X, SIZE_Y);
    }

    @Override
    public Identifier iconLocation() {
        return ICON;
    }

    @Override
    public Words screenName() {
        return Words.QuestBook;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Chapter selected() {
        List<Chapter> chapters = QuestBookCatalog.chapters();
        return chapters.get(Math.max(0, Math.min(chapters.size() - 1, selectedChapter)));
    }

    private QuestBookData book() {
        return mc.player == null ? new QuestBookData() : Load.playerRPGData(mc.player).questBook;
    }

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.children.clear();

        if (!refreshRequested && mc.player != null) {
            Packets.sendToServer(QuestBookPacket.refresh());
            refreshRequested = true;
        }

        List<Chapter> chapters = QuestBookCatalog.chapters();
        selectedChapter = Math.max(0, Math.min(chapters.size() - 1, selectedChapter));

        for (int i = 0; i < chapters.size(); i++) {
            final int index = i;
            Chapter chapter = chapters.get(i);
            boolean complete = mc.player != null && (chapter.platinum()
                ? book().platinumAwarded : QuestBookService.chapterComplete(mc.player, chapter));
            String prefix = complete ? "§a✓ " : (i == selectedChapter ? "§e» " : "§7· ");
            String label = prefix + chapter.shortTitle();
            addButton(new QuestBookButton(guiLeft + NAV_X, guiTop + NAV_Y + i * NAV_ROW_HEIGHT,
                NAV_WIDTH, 16, label, button -> {
                    selectedChapter = index;
                    scrollPixels = 0;
                    init();
                }, List.of(Component.literal(chapter.title()).withStyle(chapter.platinum()
                    ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.YELLOW))));
        }

        boolean platinumEnabled = book().platinumMode;
        String platinumLabel = platinumEnabled ? "§6✦ ПЛАТИНА: ВКЛ" : "§7✧ ПЛАТИНА: ВЫКЛ";
        addButton(new QuestBookButton(guiLeft + NAV_X, guiTop + SIZE_Y - 31, NAV_WIDTH, 19,
            platinumLabel, button -> Packets.sendToServer(QuestBookPacket.togglePlatinum()),
            List.of(Component.literal("Добровольный режим 100%"),
                Component.literal("Прогресс коллекций сохраняется автоматически."),
                Component.literal("Звание выдаётся только при включённом режиме."))));

        pageRows = createPageRows(selected());
        int totalHeight = pageRows.stream().mapToInt(PageRow::height).sum();
        int maxScroll = Math.max(0, totalHeight - (BODY_BOTTOM - BODY_TOP));
        scrollPixels = Math.max(0, Math.min(scrollPixels, maxScroll));

        int y = guiTop + BODY_TOP - scrollPixels;
        for (PageRow row : pageRows) {
            if (row.objective != null && y >= guiTop + BODY_TOP
                && y + row.height <= guiTop + BODY_BOTTOM) {
                Objective objective = row.objective;
                QuestBookService.Progress progress = mc.player == null
                    ? new QuestBookService.Progress(0, Math.max(1, QuestBookService.target(null, objective)), false)
                    : QuestBookService.progress(mc.player, objective);
                String mark = progress.complete() ? "§a✓ " : "§e○ ";
                String label = mark + objective.title() + " §8" + progress.current() + "/" + progress.target();
                boolean canClaim = objective.metric() == QuestBookCatalog.Metric.MANUAL && !progress.complete();
                addButton(new QuestBookButton(guiLeft + BODY_X, y, BODY_WIDTH, OBJECTIVE_HEIGHT,
                    label, button -> Packets.sendToServer(QuestBookPacket.claim(objective.id())),
                    List.of(Component.literal(objective.title()).withStyle(net.minecraft.ChatFormatting.YELLOW),
                        Component.literal(objective.instruction()).withStyle(net.minecraft.ChatFormatting.GRAY)),
                    canClaim));
            }
            y += row.height;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            init();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (mouseX >= guiLeft + BODY_X && mouseX <= guiLeft + BODY_X + BODY_WIDTH
            && mouseY >= guiTop + BODY_TOP && mouseY <= guiTop + BODY_BOTTOM) {
            int totalHeight = pageRows.stream().mapToInt(PageRow::height).sum();
            int maxScroll = Math.max(0, totalHeight - (BODY_BOTTOM - BODY_TOP));
            int next = Math.max(0, Math.min(maxScroll, scrollPixels - (int) Math.signum(scroll) * 20));
            if (next != scrollPixels) {
                scrollPixels = next;
                init();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scroll);
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        AbstractGui.bindTexture(PANEL);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        AbstractGui.blit(matrix, guiLeft, guiTop, SIZE_X, SIZE_Y, 0F, 0F,
            SIZE_X, SIZE_Y, PANEL_TEXTURE_WIDTH, PANEL_TEXTURE_HEIGHT);

        AbstractGui.drawShadow(mc.font, matrix, "§6КНИГА ЭПОХИ", guiLeft + 13, guiTop + 9, 0xFFFFFF);
        drawSmall(matrix, "§e" + selected().title(), guiLeft + BODY_X, guiTop + 9);
        AbstractGui.fill(matrix, guiLeft + 138, guiTop + 26, guiLeft + SIZE_X - 12, guiTop + 27, 0x705F4828);
        AbstractGui.fill(matrix, guiLeft + 139, guiTop + 30, guiLeft + 140, guiTop + SIZE_Y - 19, 0x705F4828);
        AbstractGui.drawShadow(mc.font, matrix, "§8КОЛЕСО — ПРОКРУТКА • K / H — ЗАКРЫТЬ",
            guiLeft + 14, guiTop + SIZE_Y - 10, 0xFFFFFF);

        int y = guiTop + BODY_TOP - scrollPixels;
        for (PageRow row : pageRows) {
            if (row.objective == null && y + row.height >= guiTop + BODY_TOP && y < guiTop + BODY_BOTTOM) {
                int lineY = y;
                for (String line : row.lines) {
                    if (lineY >= guiTop + BODY_TOP && lineY < guiTop + BODY_BOTTOM) {
                        drawSmall(matrix, line, guiLeft + BODY_X + 3, lineY);
                    }
                    lineY += BODY_LINE;
                }
            }
            y += row.height;
        }

        super.render(matrix, mouseX, mouseY, partialTicks);
        for (Button button : this.buttons) {
            button.renderToolTip(matrix, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private List<PageRow> createPageRows(Chapter chapter) {
        List<PageRow> rows = new ArrayList<>();
        addText(rows, "§7" + chapter.introduction());
        addText(rows, "§eПорядок прохождения");
        for (String step : chapter.route()) {
            addText(rows, "§7• " + step);
        }

        addText(rows, "§eКонтрольные точки");
        for (Objective objective : chapter.objectives()) {
            rows.add(PageRow.objective(objective));
            addText(rows, "§8" + objective.instruction());
        }

        if (chapter.id().equals("gems")) {
            addGemLedger(rows);
        } else if (chapter.id().equals("runes")) {
            addRuneLedger(rows);
        } else if (chapter.id().equals("towers")) {
            addTowerLedger(rows);
        } else if (chapter.id().equals("platinum")) {
            QuestBookData data = book();
            if (data.platinumAwarded) {
                addText(rows, "§6✦ ЗВАНИЕ «ПЛАТИНА AGE OF EPOCH» ПОЛУЧЕНО ✦");
            } else if (data.platinumMode) {
                addText(rows, "§eПлатиновый режим включён — осталось выполнить пункты ниже.");
            } else {
                addText(rows, "§7Платина добровольна: включи режим кнопкой внизу слева.");
            }
            addCompletionLedgers(rows);
        }
        return rows;
    }

    private void addGemLedger(List<PageRow> rows) {
        QuestBookData data = book();
        addText(rows, "§eКАТАЛОГ САМОЦВЕТОВ — " + ledgerProgress("platinum_gems") + "/" + QuestBookService.gemVariantCount());
        addText(rows, "§8Ранги слева направо: 0–6 (Треснувший → Божественный). • найден  ○ нет");
        for (GemItem.GemType type : GemItem.GemType.values()) {
            StringBuilder line = new StringBuilder("§f").append(russianGemName(type.id)).append(" §8");
            for (GemItem.GemRank rank : GemItem.GemRank.values()) {
                boolean found = data.gemVariants.contains(QuestBookService.gemKey(type, rank));
                line.append(found ? "§a●" : "§8○").append(" ");
            }
            rows.add(textRow(line.toString(), BODY_WIDTH - 4));
        }
    }

    private void addRuneLedger(List<PageRow> rows) {
        QuestBookData data = book();
        addText(rows, "§eКАТАЛОГ 15 РУН — " + ledgerProgress("platinum_runes") + "/" + RuneItem.RuneType.values().length);
        addText(rows, "§8Типы и уровни редкости (в скобках):");
        for (RuneItem.RuneType type : RuneItem.RuneType.values()) {
            boolean found = data.runes.contains(type.id);
            rows.add(textRow((found ? "§a✓ " : "§8○ ") + type.locName + " §7— уровень " + type.tier,
                BODY_WIDTH - 4));
        }
        addText(rows, "§8Слова: CEN DOS YUN (лук); ITA DOS TOQ (шлем); TOQ DOS YUN WIR (ожерелье); ANO ORU DOS и YUN FEY ORU (кольца); YUN MOS ORU VEN (посох); ANO TOQ MOS HAR (меч).");
    }

    private void addTowerLedger(List<PageRow> rows) {
        QuestBookData data = book();
        addText(rows, "§eРОСТЕР 16 ТЕМ / 80 БОССОВ — " + ledgerProgress("platinum_tower_bosses") + "/" + TowerRoster.size());
        for (TowerTheme theme : TowerThemes.all()) {
            List<TowerBossDef> bosses = TowerRoster.all(theme.id());
            long defeated = bosses.stream().filter(boss -> data.towerBosses
                .contains(QuestBookService.towerBossKey(theme.id(), boss.id()))).count();
            rows.add(textRow("§6" + theme.name() + " §8(" + defeated + "/" + bosses.size() + ")",
                BODY_WIDTH - 4));
            for (TowerBossDef boss : bosses) {
                boolean defeatedBoss = data.towerBosses.contains(QuestBookService.towerBossKey(theme.id(), boss.id()));
                rows.add(textRow((defeatedBoss ? "§a✓ " : "§8○ ") + boss.name() + " §8[" + boss.id() + "]",
                    BODY_WIDTH - 4));
            }
        }
    }

    private void addCompletionLedgers(List<PageRow> rows) {
        QuestBookData data = book();

        addText(rows, "§eУНИКАЛЬНЫЕ ПРЕДМЕТЫ — " + ledgerProgress("platinum_uniques") + "/" + QuestBookService.target(mc.player, objectiveById("platinum_uniques")));
        try {
            ExileDB.UniqueGears().getList().stream()
                .filter(gear -> gear.GUID() != null && !gear.GUID().isBlank())
                .sorted(Comparator.comparing(UniqueGear::GUID))
                .forEach(gear -> {
                    boolean found = data.uniqueGears.contains(gear.GUID());
                    String name;
                    try {
                        name = gear.translate();
                    } catch (Exception ignored) {
                        name = gear.GUID();
                    }
                    if (name == null || name.isBlank() || name.equals(gear.locNameLangFileGUID())) {
                        name = gear.GUID();
                    }
                    rows.add(textRow((found ? "§a✓ " : "§8○ ") + name + " §8[" + gear.GUID() + "]",
                        BODY_WIDTH - 4));
                });
        } catch (Exception ignored) {
            addText(rows, "§cБаза уникальных предметов ещё не загружена.");
        }

        addText(rows, "§eРУНИЧЕСКИЕ СЛОВА — " + ledgerProgress("platinum_runewords") + "/" + QuestBookService.target(mc.player, objectiveById("platinum_runewords")));
        try {
            ExileDB.RuneWords().getList().stream()
                .sorted(Comparator.comparing(word -> word.GUID() == null ? "" : word.GUID()))
                .forEach(word -> rows.add(textRow((data.runewords.contains(word.GUID()) ? "§a✓ " : "§8○ ")
                    + word.GUID(), BODY_WIDTH - 4)));
        } catch (Exception ignored) {
            addText(rows, "§cБаза рунических слов ещё не загружена.");
        }

        addText(rows, "§eПЛАНИРОВКИ INFINITE DUNGEONS — " + ledgerProgress("platinum_dungeons")
            + "/" + QuestBookService.target(mc.player, objectiveById("platinum_dungeons")));
        try {
            DungeonsDB.DungeonLayouts().getList().stream()
                .sorted(Comparator.comparing(layout -> layout.GUID() == null ? "" : layout.GUID()))
                .forEach(layout -> {
                    String id = layout.GUID();
                    boolean found = id != null && data.infiniteDungeonLayouts.contains(id);
                    String name;
                    try {
                        name = layout.getTranslatable().getString();
                    } catch (Exception ignored) {
                        name = id;
                    }
                    rows.add(textRow((found ? "§a✓ " : "§8○ ") + name + " §8[" + id + "]",
                        BODY_WIDTH - 4));
                });
        } catch (Exception ignored) {
            addText(rows, "§cБаза Infinite Dungeons ещё не загружена.");
        }

        addText(rows, "§eМИССИИ БОГОВ — " + ledgerProgress("platinum_gods")
            + "/" + QuestBookService.target(mc.player, objectiveById("platinum_gods")));
        try {
            MissionsDB.Gods().getList().stream()
                .sorted(Comparator.comparing(god -> god.GUID() == null ? "" : god.GUID()))
                .forEach(god -> {
                    String id = god.GUID();
                    boolean found = id != null && data.missionGods.contains(id);
                    String name;
                    try {
                        name = god.getTranslatable().getString();
                    } catch (Exception ignored) {
                        name = id;
                    }
                    rows.add(textRow((found ? "§a✓ " : "§8○ ") + name + " §8[" + id + "]",
                        BODY_WIDTH - 4));
                });
        } catch (Exception ignored) {
            addText(rows, "§cБаза богов ещё не загружена.");
        }

        addText(rows, "§eСБОРКИ НА ПОЛИГОНЕ — " + ledgerProgress("platinum_guides")
            + "/" + QuestBookService.target(mc.player, objectiveById("platinum_guides")));
        for (GuideDefinition guide : GuideData.all()) {
            boolean found = data.simulationGuides.contains(guide.id);
            rows.add(textRow((found ? "§a✓ " : "§8○ ") + guide.name + " §8[" + guide.id + "]",
                BODY_WIDTH - 4));
        }

        addText(rows, "§8Важно: для зачёта предмета оставь его в инвентаре хотя бы до ближайшего сканирования (раз в секунду).");
    }

    private Objective objectiveById(String id) {
        Objective objective = QuestBookCatalog.findObjective(id);
        if (objective != null) {
            return objective;
        }
        return new Objective("missing", "Неизвестно", "", QuestBookCatalog.Metric.MANUAL, 1);
    }

    private int ledgerProgress(String id) {
        Objective objective = objectiveById(id);
        if (mc.player == null) {
            return 0;
        }
        return QuestBookService.progress(mc.player, objective).current();
    }

    private void addText(List<PageRow> rows, String text) {
        rows.add(textRow(text, BODY_WIDTH - 5));
    }

    private PageRow textRow(String text, int maxWidth) {
        List<String> lines = wrap(text, maxWidth);
        return new PageRow(lines, null, Math.max(BODY_LINE, lines.size() * BODY_LINE) + 1);
    }

    private void drawSmall(PoseStack matrix, String text, int x, int y) {
        matrix.pushPose();
        matrix.scale(BODY_SCALE, BODY_SCALE, 1F);
        AbstractGui.drawShadow(mc.font, matrix, text, x / BODY_SCALE, y / BODY_SCALE, 0xFFFFFF);
        matrix.popPose();
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        String[] paragraphs = text.split("\\n", -1);
        for (String paragraph : paragraphs) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (word.isEmpty()) {
                    continue;
                }
                String next = current.isEmpty() ? word : current + " " + word;
                if (mc.font.width(next) > maxWidth / BODY_SCALE && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(next);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    private static String russianGemName(String id) {
        return switch (id) {
            case "tourmaline" -> "Турмалин";
            case "azurite" -> "Азурит";
            case "garnet" -> "Гранат";
            case "opal" -> "Опал";
            case "topaz" -> "Топаз";
            case "amethyst" -> "Аметист";
            case "ruby" -> "Рубин";
            case "emerald" -> "Изумруд";
            case "sapphire" -> "Сапфир";
            default -> id;
        };
    }

    private record PageRow(List<String> lines, Objective objective, int height) {
        private static PageRow objective(Objective objective) {
            return new PageRow(List.of(), objective, OBJECTIVE_HEIGHT);
        }
    }

    private static final class QuestBookButton extends Button {
        private final String label;
        private final List<Component> tooltip;

        private QuestBookButton(int x, int y, int width, int height, String label,
                                net.minecraft.client.gui.components.Button.OnPress onPress,
                                List<Component> tooltip) {
            this(x, y, width, height, label, onPress, tooltip, true);
        }

        private QuestBookButton(int x, int y, int width, int height, String label,
                                net.minecraft.client.gui.components.Button.OnPress onPress,
                                List<Component> tooltip, boolean active) {
            super(x, y, width, height, Component.literal(label), onPress);
            this.label = label;
            this.tooltip = tooltip == null ? List.of() : tooltip;
            this.active = active;
        }

        @Override
        public void renderButton(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
            boolean hovered = isHovered() && active;
            int background = hovered ? 0xA04C350B : (active ? 0x70301B08 : 0x50301808);
            AbstractGui.fill(matrix, x, y, x + width, y + height, background);
            AbstractGui.fill(matrix, x, y, x + width, y + 1,
                hovered ? 0xA0FFD35A : 0x706D5635);
            if (hovered) {
                AbstractGui.fill(matrix, x, y + height - 1, x + width, y + height, 0xA0FFD35A);
            }

            float scale = 1F;
            while (Minecraft.getInstance().font.width(label) * scale > width - 7 && scale > 0.55F) {
                scale -= 0.05F;
            }
            matrix.pushPose();
            matrix.scale(scale, scale, 1F);
            AbstractGui.drawShadow(Minecraft.getInstance().font, matrix, label,
                (x + 4) / scale, (y + (height - 8) / 2F) / scale, 0xFFFFFF);
            matrix.popPose();
        }

        @Override
        public void renderToolTip(PoseStack matrix, int mouseX, int mouseY) {
            if (isHovered() && !tooltip.isEmpty()) {
                GuiUtils.renderTooltip(matrix, tooltip, mouseX, mouseY);
            }
        }
    }
}
