package com.robertx22.age_of_exile.wiki;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.gui.bases.INamedScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.wiki.WikiMarkdown.Block;
import com.robertx22.age_of_exile.wiki.WikiMarkdown.Kind;
import com.robertx22.age_of_exile.wiki.WikiMarkdown.Page;
import com.robertx22.age_of_exile.wiki.WikiMarkdown.Span;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран внутриигровой вики Age of Epoch.
 *
 * <p>Рендерит каталог markdown-страниц ({@link WikiCatalog} +
 * {@link WikiMarkdown}): слева — оглавление с поиском и фильтрами, по центру —
 * статья с кликабельными [[ссылками]], справа — карточка с вращающейся
 * 3D-моделью предмета (для блоков это настоящая объёмная модель игры,
 * плоские предметы «крутятся» турнтейбл-анимацией; модель можно вращать
 * мышиным перетаскиванием).
 *
 * <p>Экран полностью рисуется примитивами совместимого слоя (как
 * QuestBookScreen), поэтому не требует текстур фиксированного размера и
 * честно растягивается под окно. Все операции с данными защищены
 * try/catch на уровне каталога — вики не может уронить игру.
 */
public class ModWikiScreen extends BaseScreen implements INamedScreen {

    private static final Identifier ICON = Identifier.fromNamespaceAndPath(
        SlashRef.MODID, "textures/gui/main_hub/icons/wiki.png");

    // ---- состояние, переживающее переоткрытие экрана (хаб создаёт новый экземпляр) ----
    private static String lastPageId = WikiCatalog.HOME_ID;
    private static final List<String> BACK = new ArrayList<String>();
    private static final List<String> FORWARD = new ArrayList<String>();

    // ---- геометрия (пересчитывается в init под размер окна) ----
    private int sidebarX, sidebarY, sidebarW, sidebarH;
    private int articleX, articleY, articleW, articleH;
    private int infoX, infoY, infoW, infoH;
    private int searchX, searchY, searchW;
    private int previewCX, previewCY, previewR;
    private boolean hasInfobox;

    // ---- состояние просмотра ----
    private String pageId = lastPageId;
    private String pendingAnchor = null;
    private Page page;
    private ItemStack previewStack = ItemStack.EMPTY;
    private String query = "";
    private boolean searchFocused = false;
    private int filterGroup = 0; // 0 все, 1 гайды, 2 разделы, 3 предметы
    private List<Page> indexList = new ArrayList<Page>();
    private int sidebarScroll = 0;
    private int articleScroll = 0;
    private List<Line> lines = new ArrayList<Line>();
    private int articleContentH = 0;
    private double spinDragOffset = 0;
    private boolean draggingPreview = false;
    private String hoveredLink = null;

    private static final String[] FILTER_LABELS = {"Все", "Гайды", "Разделы", "Предметы"};

    public ModWikiScreen() {
        super(360, 300);
        this.pageId = lastPageId;
    }

    private static String pendingSearch = null;

    /** Открыть вики сразу на нужной странице (для будущих вызовов из других экранов). */
    public static void openAt(String id) {
        if (id != null && id.length() > 0) {
            lastPageId = id;
        }
        Minecraft.getInstance().gui.setScreen(new ModWikiScreen());
    }

    /** Открыть вики с уже заполненным поиском (предметы без своей статьи). */
    public static void openWithSearch(String queryText) {
        pendingSearch = queryText;
        lastPageId = WikiCatalog.HOME_ID;
        Minecraft.getInstance().gui.setScreen(new ModWikiScreen());
    }

    @Override
    public Identifier iconLocation() {
        return ICON;
    }

    @Override
    public Words screenName() {
        return Words.Wiki;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- инициализация и вёрстка

    @Override
    protected void init() {
        WikiCatalog.ensureLoaded();
        super.init();
        this.buttons.clear();
        this.children.clear();

        this.sizeX = Math.max(340, Math.min(640, this.width - 24));
        this.sizeY = Math.max(240, Math.min(420, this.height - 24));
        this.guiLeft = (this.width - this.sizeX) / 2;
        this.guiTop = (this.height - this.sizeY) / 2;

        int pad = 8;
        int headerH = 26;
        int footerH = 13;
        sidebarW = sizeX >= 480 ? 138 : 106;
        hasInfobox = sizeX >= 520;
        infoW = hasInfobox ? 124 : 0;

        sidebarX = guiLeft + pad;
        sidebarY = guiTop + headerH + 14;
        sidebarH = sizeY - headerH - 14 - footerH - 4;

        infoX = guiLeft + sizeX - pad - infoW;
        infoY = guiTop + headerH;
        infoH = sizeY - headerH - footerH - 4;

        articleX = sidebarX + sidebarW + 6;
        articleY = guiTop + headerH;
        articleW = (hasInfobox ? infoX - 6 : guiLeft + sizeX - pad) - articleX;
        articleH = sizeY - headerH - footerH - 4;

        searchW = Math.max(90, sizeX - 8 - 170);
        searchX = guiLeft + sizeX - pad - searchW;
        searchY = guiTop + 6;

        previewCX = infoX + infoW / 2;
        previewCY = infoY + 62;
        previewR = 44;

        loadPage(pageId, false);
        if (pendingSearch != null) {
            query = pendingSearch;
            pendingSearch = null;
            filterGroup = 0;
            sidebarScroll = 0;
        }
        refreshIndex();
        clampScrolls();
    }

    private void loadPage(String id, boolean resetScroll) {
        this.pageId = id == null ? WikiCatalog.HOME_ID : id;
        this.page = WikiCatalog.page(this.pageId);
        this.pageId = this.page.id;
        lastPageId = this.pageId;
        this.previewStack = stackFor(this.page);
        layoutArticle();
        if (resetScroll) {
            articleScroll = 0;
        }
        if (pendingAnchor != null) {
            String slug = WikiMarkdown.slugify(pendingAnchor);
            for (Line line : lines) {
                if (slug.equals(line.anchor)) {
                    articleScroll = Math.max(0, line.y - 4);
                    break;
                }
            }
            pendingAnchor = null;
        }
        clampScrolls();
    }

    private ItemStack stackFor(Page p) {
        if (p == null || p.itemId == null) {
            return ItemStack.EMPTY;
        }
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(p.itemId));
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    private void refreshIndex() {
        List<Page> found = WikiCatalog.search(query);
        indexList = new ArrayList<Page>();
        for (Page p : found) {
            if (filterGroup == 1 && !"guides".equals(p.category)) {
                continue;
            }
            if (filterGroup == 2 && !"categories".equals(p.category)) {
                continue;
            }
            if (filterGroup == 3 && ("guides".equals(p.category) || "categories".equals(p.category)
                || "home".equals(p.category))) {
                continue;
            }
            indexList.add(p);
        }
    }

    private void clampScrolls() {
        int maxArticle = Math.max(0, articleContentH - articleH);
        articleScroll = Math.max(0, Math.min(articleScroll, maxArticle));
        int rowsVisible = Math.max(1, sidebarH / 14);
        int maxSidebar = Math.max(0, (indexList.size() - rowsVisible) * 14);
        sidebarScroll = Math.max(0, Math.min(sidebarScroll, maxSidebar));
    }

    // ---- вёрстка статьи: блоки -> строки с хитбоксами ссылок ----

    private static final class Frag {
        String rendered;      // текст с §-кодами для отрисовки/замера
        String link;          // цель ссылки или null
        int width;
        int x;                // смещение внутри статьи
    }

    private static final class Line {
        final List<Frag> frags = new ArrayList<Frag>();
        int y;                // смещение от начала статьи (без скролла)
        int height = 10;
        String anchor;        // если строка — начало заголовка
    }

    private String decorate(Span span, Kind kind) {
        StringBuilder sb = new StringBuilder();
        if (span.link != null) {
            sb.append(span.link.equals(hoveredLink) ? "§b§n" : "§9");
        } else if (span.code) {
            sb.append("§a");
        } else if (kind == Kind.QUOTE) {
            sb.append("§7");
        } else if (kind == Kind.TABLE_ROW) {
            sb.append("§f");
        }
        if (span.bold) {
            sb.append("§l");
        }
        if (span.italic) {
            sb.append("§o");
        }
        sb.append(span.text);
        return sb.toString();
    }

    private void layoutArticle() {
        lines = new ArrayList<Line>();
        if (page == null || mc.font == null) {
            articleContentH = 0;
            return;
        }
        int y = 2;
        int numbered = 0;
        for (Block block : page.blocks) {
            if (block.kind == Kind.NUMBERED_ITEM) {
                numbered++;
            } else {
                numbered = 0;
            }
            if (block.kind == Kind.RULE) {
                Line rule = new Line();
                rule.y = y + 4;
                rule.height = 9;
                Frag f = new Frag();
                f.rendered = "§8────────────────────────";
                f.width = mc.font.width(f.rendered);
                rule.frags.add(f);
                lines.add(rule);
                y += 11;
                continue;
            }
            String prefix;
            int gapAfter = 4;
            switch (block.kind) {
                case HEADING:
                    prefix = block.level <= 1 ? "§6§l" : (block.level == 2 ? "§e§l" : "§f§l");
                    y += block.level <= 1 ? 2 : 6;
                    gapAfter = 3;
                    break;
                case LIST_ITEM:
                    prefix = block.level > 0 ? "    §8– §r" : "§6• §r";
                    gapAfter = 1;
                    break;
                case NUMBERED_ITEM:
                    prefix = "§6" + numbered + ". §r";
                    gapAfter = 1;
                    break;
                case QUOTE:
                    prefix = "§8┃ §7";
                    break;
                default:
                    prefix = "";
            }
            List<Frag> words = new ArrayList<Frag>();
            if (prefix.length() > 0) {
                Frag pf = new Frag();
                pf.rendered = prefix;
                pf.width = mc.font.width(prefix.replace("§r", ""));
                if (block.kind == Kind.HEADING) {
                    pf.width = 0; // у заголовка префикс — только цвет, он сольётся с первым словом ниже
                }
                words.add(pf);
            }
            boolean firstWord = true;
            for (Span span : block.spans) {
                String text = span.text.replace("\n", " ");
                String decoratedPrefix = "";
                if (block.kind == Kind.HEADING && firstWord && span.link == null && !span.code) {
                    decoratedPrefix = prefix;
                }
                for (String word : text.split(" ", -1)) {
                    if (word.length() == 0) {
                        continue;
                    }
                    Span piece = new Span(word, span.bold, span.italic, span.code, span.link);
                    Frag frag = new Frag();
                    frag.rendered = decoratedPrefix + decorate(piece, block.kind);
                    frag.link = span.link;
                    frag.width = mc.font.width(frag.rendered) + 4;
                    words.add(frag);
                    decoratedPrefix = "";
                    firstWord = false;
                }
            }
            // жадная упаковка по ширине статьи
            Line line = new Line();
            line.y = y;
            line.anchor = block.kind == Kind.HEADING ? block.anchor : null;
            int cursor = 0;
            for (Frag frag : words) {
                if (cursor > 0 && cursor + frag.width > articleW - 12) {
                    lines.add(line);
                    y += line.height;
                    line = new Line();
                    line.y = y;
                    line.anchor = null;
                    cursor = (block.kind == Kind.LIST_ITEM || block.kind == Kind.NUMBERED_ITEM) ? 10 : 0;
                }
                frag.x = cursor;
                line.frags.add(frag);
                cursor += frag.width;
            }
            lines.add(line);
            y += line.height + gapAfter;
        }
        articleContentH = y + 4;
    }

    // ---------------------------------------------------------------- отрисовка

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrix);
        updateHoveredLink(mouseX, mouseY);

        // панель и рамки
        fill(matrix, guiLeft, guiTop, guiLeft + sizeX, guiTop + sizeY, 0xF00E1219);
        rect(matrix, guiLeft, guiTop, guiLeft + sizeX, guiTop + sizeY, 0xFF8A6D3B);
        fill(matrix, guiLeft + 1, guiTop + 1, guiLeft + sizeX - 1, guiTop + 25, 0xFF161D29);
        fill(matrix, guiLeft + 1, guiTop + 25, guiLeft + sizeX - 1, guiTop + 26, 0xFF8A6D3B);

        drawHeader(matrix, mouseX, mouseY);
        drawSidebar(matrix, mouseX, mouseY);
        drawArticle(matrix, mouseX, mouseY);
        if (hasInfobox) {
            drawInfobox(matrix, mouseX, mouseY, partialTicks);
        }

        AbstractGui.drawShadow(mc.font, matrix,
            "§8КОЛЕСО — ПРОКРУТКА • ССЫЛКИ КЛИКАБЕЛЬНЫ • МОДЕЛЬ МОЖНО КРУТИТЬ МЫШЬЮ",
            guiLeft + 9, guiTop + sizeY - 10, 0xFFFFFF);
        String counter = "§8страниц: " + WikiCatalog.all().size() + " • " + pageId;
        AbstractGui.drawShadow(mc.font, matrix, counter,
            guiLeft + sizeX - 9 - mc.font.width(counter.replace("§8", "")), guiTop + sizeY - 10, 0xFFFFFF);

        super.render(matrix, mouseX, mouseY, partialTicks);

        // подсказка предмета на превью
        if (hasInfobox && !previewStack.isEmpty() && inside(mouseX, mouseY,
            previewCX - previewR, previewCY - previewR, previewR * 2, previewR * 2)) {
            List<Component> tip = new ArrayList<Component>();
            tip.add(previewStack.getHoverName());
            tip.add(Component.literal("§8" + page.itemId));
            tip.add(Component.literal("§7Тяните мышью, чтобы вращать"));
            GuiUtils.renderTooltip(matrix, tip, mouseX, mouseY);
        }
        if (hoveredLink != null) {
            Page target = targetOf(hoveredLink);
            if (target != null) {
                List<Component> tip = new ArrayList<Component>();
                tip.add(Component.literal("§e" + target.title));
                tip.add(Component.literal("§8" + hoveredLink));
                GuiUtils.renderTooltip(matrix, tip, mouseX, mouseY);
            }
        }
    }

    private void drawHeader(PoseStack matrix, int mouseX, int mouseY) {
        AbstractGui.drawShadow(mc.font, matrix, "§6§lВИКИ ЭПОХИ", guiLeft + 9, guiTop + 9, 0xFFFFFF);
        drawNavButton(matrix, mouseX, mouseY, guiLeft + 78, guiTop + 5, 16, "‹", !BACK.isEmpty());
        drawNavButton(matrix, mouseX, mouseY, guiLeft + 96, guiTop + 5, 16, "›", !FORWARD.isEmpty());
        drawNavButton(matrix, mouseX, mouseY, guiLeft + 114, guiTop + 5, 42, "Главная", true);

        // строка поиска
        fill(matrix, searchX, searchY, searchX + searchW, searchY + 14, searchFocused ? 0xFF1E2A3D : 0xFF121A26);
        rect(matrix, searchX, searchY, searchX + searchW, searchY + 14, searchFocused ? 0xFFD8A83C : 0xFF3A465C);
        String shown = query.length() == 0 ? (searchFocused ? "" : "§8Поиск по вики…") : query;
        String withCaret = shown + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "§e|" : "");
        AbstractGui.drawShadow(mc.font, matrix, withCaret, searchX + 4, searchY + 3, 0xFFFFFF);

        // фильтры под заголовком сайдбара
        int chipX = sidebarX;
        for (int i = 0; i < FILTER_LABELS.length; i++) {
            int w = mc.font.width(FILTER_LABELS[i]) + 6;
            boolean active = filterGroup == i;
            fill(matrix, chipX, sidebarY - 13, chipX + w, sidebarY - 2, active ? 0xFF6B4E1F : 0xFF18202E);
            AbstractGui.drawShadow(mc.font, matrix, (active ? "§e" : "§7") + FILTER_LABELS[i],
                chipX + 3, sidebarY - 11, 0xFFFFFF);
            chipX += w + 3;
        }
    }

    private void drawNavButton(PoseStack matrix, int mouseX, int mouseY, int x, int y, int w, String label, boolean active) {
        boolean hovered = active && inside(mouseX, mouseY, x, y, w, 14);
        fill(matrix, x, y, x + w, y + 14, hovered ? 0xFF3A2B12 : 0xFF18202E);
        rect(matrix, x, y, x + w, y + 14, active ? 0xFF8A6D3B : 0xFF2A3444);
        String colored = (active ? (hovered ? "§e" : "§f") : "§8") + label;
        AbstractGui.drawShadow(mc.font, matrix, colored,
            x + (w - mc.font.width(label)) / 2, y + 3, 0xFFFFFF);
    }

    private void drawSidebar(PoseStack matrix, int mouseX, int mouseY) {
        fill(matrix, sidebarX - 2, sidebarY - 1, sidebarX + sidebarW, sidebarY + sidebarH, 0x80101824);
        int y = sidebarY - sidebarScroll;
        for (Page p : indexList) {
            if (y + 14 >= sidebarY && y < sidebarY + sidebarH) {
                boolean current = p.id.equals(pageId);
                boolean hovered = inside(mouseX, mouseY, sidebarX, y, sidebarW - 2, 13);
                if (current || hovered) {
                    fill(matrix, sidebarX, y, sidebarX + sidebarW - 2, y + 13,
                        current ? 0xFF3A2B12 : 0xFF1C2636);
                }
                fill(matrix, sidebarX + 2, y + 4, sidebarX + 6, y + 8, groupColor(p));
                String label = trimToWidth(p.title, sidebarW - 16);
                AbstractGui.drawShadow(mc.font, matrix, (current ? "§e" : "§f") + label,
                    sidebarX + 9, y + 3, 0xFFFFFF);
            }
            y += 14;
        }
        drawScrollbar(matrix, sidebarX + sidebarW - 1, sidebarY, sidebarH,
            indexList.size() * 14, sidebarScroll);
    }

    private int groupColor(Page p) {
        if ("guides".equals(p.category)) {
            return 0xFFD8A83C;
        }
        if ("categories".equals(p.category)) {
            return 0xFF9D6BFF;
        }
        if ("home".equals(p.category)) {
            return 0xFF5AD86B;
        }
        return p.itemId != null ? 0xFF4EA3FF : 0xFF8A8A8A;
    }

    private void drawArticle(PoseStack matrix, int mouseX, int mouseY) {
        int top = articleY + 3;
        int bottom = articleY + articleH;
        for (Line line : lines) {
            int y = top + line.y - articleScroll;
            if (y + line.height < top || y > bottom) {
                continue;
            }
            for (Frag frag : line.frags) {
                // подчёркивание наведённой ссылки пересобираем на лету
                String text = frag.rendered;
                if (frag.link != null && frag.link.equals(hoveredLink) && !text.contains("§n")) {
                    text = "§b§n" + text.replace("§9", "");
                }
                AbstractGui.drawShadow(mc.font, matrix, text, articleX + frag.x, y, 0xE8E4D8);
            }
        }
        drawScrollbar(matrix, articleX + articleW - 1, articleY + 3, articleH - 3,
            articleContentH, articleScroll);
    }

    private void drawScrollbar(PoseStack matrix, int x, int y, int h, int contentH, int scroll) {
        if (contentH <= h || h <= 0) {
            return;
        }
        fill(matrix, x, y, x + 2, y + h, 0xFF222C3E);
        int thumbH = Math.max(12, h * h / Math.max(1, contentH));
        int thumbY = y + (h - thumbH) * scroll / Math.max(1, contentH - h);
        fill(matrix, x, thumbY, x + 2, thumbY + thumbH, 0xFFD8A83C);
    }

    private void drawInfobox(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        fill(matrix, infoX, infoY + 2, infoX + infoW, infoY + infoH, 0x80101824);
        rect(matrix, infoX, infoY + 2, infoX + infoW, infoY + 118, 0xFF2A3444);

        // пьедестал и тень
        long now = System.currentTimeMillis();
        double bob = Math.sin(now / 380.0) * 2.2;
        int shadowW = 30 + (int) bob;
        fill(matrix, previewCX - shadowW, previewCY + 34, previewCX + shadowW, previewCY + 38, 0x66000000);
        fill(matrix, previewCX - 34, previewCY + 38, previewCX + 34, previewCY + 41, 0xFF8A6D3B);

        // вращающаяся модель
        if (!previewStack.isEmpty()) {
            double angle = spinDragOffset + now * 0.0016;
            double squash = 0.30 + 0.70 * Math.abs(Math.cos(angle));
            float scale = 3.1F;
            GuiGraphicsExtractor graphics = AbstractGui.currentGraphics();
            if (graphics != null) {
                graphics.pose().pushMatrix();
                try {
                    graphics.pose().translate((float) previewCX, (float) (previewCY + bob));
                    graphics.pose().scale((float) (scale * squash), scale);
                    graphics.pose().translate(-8F, -8F);
                    graphics.item(previewStack, 0, 0);
                } finally {
                    graphics.pose().popMatrix();
                }
            }
        } else {
            String mark = "?";
            AbstractGui.drawShadow(mc.font, matrix, "§8" + mark, previewCX - 2, previewCY - 4, 0xFFFFFF);
        }

        // факты карточки
        int y = infoY + 124;
        String title = trimToWidth(page.title, infoW - 8);
        AbstractGui.drawShadow(mc.font, matrix, "§e§l" + title, infoX + 4, y, 0xFFFFFF);
        y += 13;
        if (page.categoryLabel.length() > 0) {
            AbstractGui.drawShadow(mc.font, matrix, "§7Раздел: §f" +
                trimToWidth(page.categoryLabel, infoW - 34), infoX + 4, y, 0xFFFFFF);
            y += 11;
        }
        if (page.itemId != null) {
            AbstractGui.drawShadow(mc.font, matrix, "§7ID:", infoX + 4, y, 0xFFFFFF);
            y += 10;
            AbstractGui.drawShadow(mc.font, matrix, "§8" +
                trimToWidth(page.itemId, infoW - 8), infoX + 4, y, 0xFFFFFF);
            y += 14;
        }
        if (!page.links.isEmpty()) {
            AbstractGui.drawShadow(mc.font, matrix, "§6Связано:", infoX + 4, y, 0xFFFFFF);
            y += 11;
            int shown = 0;
            for (String link : page.links) {
                if (shown >= 7 || y > infoY + infoH - 12) {
                    break;
                }
                Page target = WikiCatalog.page(link);
                boolean hovered = inside(mouseX, mouseY, infoX + 4, y - 1, infoW - 8, 10);
                String label = trimToWidth(target.title, infoW - 16);
                AbstractGui.drawShadow(mc.font, matrix, (hovered ? "§b§n› " : "§9› ") + label,
                    infoX + 4, y, 0xFFFFFF);
                y += 10;
                shown++;
            }
        }
    }

    // ---------------------------------------------------------------- взаимодействие

    private void updateHoveredLink(int mouseX, int mouseY) {
        hoveredLink = linkAt(mouseX, mouseY);
    }

    private String linkAt(double mouseX, double mouseY) {
        int top = articleY + 3;
        if (!inside(mouseX, mouseY, articleX, top, articleW, articleH)) {
            return null;
        }
        for (Line line : lines) {
            int y = top + line.y - articleScroll;
            if (mouseY < y || mouseY >= y + line.height) {
                continue;
            }
            for (Frag frag : line.frags) {
                if (frag.link != null && mouseX >= articleX + frag.x
                    && mouseX < articleX + frag.x + frag.width) {
                    return frag.link;
                }
            }
        }
        return null;
    }

    private Page targetOf(String link) {
        String id = link;
        int hash = id.indexOf('#');
        if (hash >= 0) {
            id = id.substring(0, hash);
        }
        if (id.length() == 0) {
            return page;
        }
        return WikiCatalog.exists(id) ? WikiCatalog.page(id) : null;
    }

    private void navigate(String link, boolean pushHistory) {
        if (link == null) {
            return;
        }
        String id = link;
        String anchor = null;
        int hash = id.indexOf('#');
        if (hash >= 0) {
            anchor = id.substring(hash + 1);
            id = id.substring(0, hash);
        }
        if (id.length() == 0) {
            // ссылка внутри той же страницы
            pendingAnchor = anchor;
            loadPage(pageId, false);
            return;
        }
        if (!WikiCatalog.exists(id)) {
            return;
        }
        if (pushHistory) {
            BACK.add(pageId);
            if (BACK.size() > 50) {
                BACK.remove(0);
            }
            FORWARD.clear();
        }
        pendingAnchor = anchor;
        loadPage(id, anchor == null);
    }

    private void goBack() {
        if (BACK.isEmpty()) {
            return;
        }
        FORWARD.add(pageId);
        String id = BACK.remove(BACK.size() - 1);
        pendingAnchor = null;
        loadPage(id, true);
    }

    private void goForward() {
        if (FORWARD.isEmpty()) {
            return;
        }
        BACK.add(pageId);
        String id = FORWARD.remove(FORWARD.size() - 1);
        pendingAnchor = null;
        loadPage(id, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // поиск: фокус
            if (inside(mouseX, mouseY, searchX, searchY, searchW, 14)) {
                searchFocused = true;
                return true;
            }
            searchFocused = false;
            // навигация
            if (inside(mouseX, mouseY, guiLeft + 78, guiTop + 5, 16, 14)) {
                goBack();
                return true;
            }
            if (inside(mouseX, mouseY, guiLeft + 96, guiTop + 5, 16, 14)) {
                goForward();
                return true;
            }
            if (inside(mouseX, mouseY, guiLeft + 114, guiTop + 5, 42, 14)) {
                navigate(WikiCatalog.HOME_ID, true);
                return true;
            }
            // чипсы-фильтры
            int chipX = sidebarX;
            for (int i = 0; i < FILTER_LABELS.length; i++) {
                int w = mc.font.width(FILTER_LABELS[i]) + 6;
                if (inside(mouseX, mouseY, chipX, sidebarY - 13, w, 11)) {
                    filterGroup = i;
                    sidebarScroll = 0;
                    refreshIndex();
                    clampScrolls();
                    return true;
                }
                chipX += w + 3;
            }
            // сайдбар
            if (inside(mouseX, mouseY, sidebarX, sidebarY, sidebarW, sidebarH)) {
                int row = (int) ((mouseY - sidebarY + sidebarScroll) / 14);
                if (row >= 0 && row < indexList.size()) {
                    navigate(indexList.get(row).id, true);
                    return true;
                }
            }
            // связанные страницы в карточке справа
            if (hasInfobox && !page.links.isEmpty() && inside(mouseX, mouseY, infoX, infoY + 124, infoW, infoH - 124)) {
                int linksStartY = infoY + 124 + 13
                    + (page.categoryLabel.length() > 0 ? 11 : 0)
                    + (page.itemId != null ? 24 : 0) + 11;
                int idx = (int) ((mouseY - linksStartY + 1) / 10);
                if (idx >= 0 && idx < Math.min(7, page.links.size()) && mouseY >= linksStartY) {
                    navigate(page.links.get(idx), true);
                    return true;
                }
            }
            // начало вращения модели
            if (hasInfobox && inside(mouseX, mouseY, previewCX - previewR, previewCY - previewR,
                previewR * 2, previewR * 2)) {
                draggingPreview = true;
                return true;
            }
            // ссылки в статье
            String link = linkAt(mouseX, mouseY);
            if (link != null) {
                navigate(link, true);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingPreview && button == 0) {
            spinDragOffset += deltaX * 0.03;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingPreview = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        int step = (int) Math.signum(scroll) * 24;
        if (inside(mouseX, mouseY, sidebarX, sidebarY, sidebarW + 4, sidebarH)) {
            sidebarScroll -= step;
        } else {
            articleScroll -= step;
        }
        clampScrolls();
        return true;
    }

    // ---- клавиатура: один обработчик на все пути доставки событий ----

    private boolean onKeyCode(int keyCode) {
        if (searchFocused) {
            if (keyCode == 259) { // Backspace
                if (query.length() > 0) {
                    query = query.substring(0, query.length() - 1);
                    sidebarScroll = 0;
                    refreshIndex();
                    clampScrolls();
                }
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                if (!indexList.isEmpty()) {
                    searchFocused = false;
                    navigate(indexList.get(0).id, true);
                }
                return true;
            }
            if (keyCode == 256) { // Esc: сначала снять фокус
                searchFocused = false;
                return true;
            }
            if (keyCode == 85 && query.length() == 0) {
                return false;
            }
            // не даём буквам утекать в BaseScreen (H/K закрыли бы экран во время печати)
            return true;
        }
        if (keyCode == 264) { // Down
            articleScroll += 30;
            clampScrolls();
            return true;
        }
        if (keyCode == 265) { // Up
            articleScroll -= 30;
            clampScrolls();
            return true;
        }
        if (keyCode == 266) { // PageUp
            articleScroll -= articleH;
            clampScrolls();
            return true;
        }
        if (keyCode == 267) { // PageDown
            articleScroll += articleH;
            clampScrolls();
            return true;
        }
        return false;
    }

    private boolean onChar(char c) {
        if (!searchFocused) {
            return false;
        }
        if (c >= 32 && c != 127 && query.length() < 48) {
            query += c;
            sidebarScroll = 0;
            refreshIndex();
            clampScrolls();
        }
        return true;
    }

    // совместимый слой (int-сигнатуры BaseScreen/compat Screen)
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (onKeyCode(keyCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (onChar(codePoint)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    // ванильные события Minecraft 26.x (KeyEvent/CharacterEvent)
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (onKeyCode(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        int cp = event.codepoint();
        if (cp > 0 && cp < 0x10000 && onChar((char) cp)) {
            return true;
        }
        return super.charTyped(event);
    }

    // ---------------------------------------------------------------- помощники

    /** Обрезать строку под ширину в пикселях (только font.width — без редких API). */
    private String trimToWidth(String text, int width) {
        if (text == null) {
            return "";
        }
        if (mc.font.width(text) <= width) {
            return text;
        }
        String ellipsis = "…";
        int limit = Math.max(0, width - mc.font.width(ellipsis));
        int end = text.length();
        while (end > 0 && mc.font.width(text.substring(0, end)) > limit) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void fill(PoseStack matrix, int x1, int y1, int x2, int y2, int color) {
        AbstractGui.fill(matrix, x1, y1, x2, y2, color);
    }

    private void rect(PoseStack matrix, int x1, int y1, int x2, int y2, int color) {
        fill(matrix, x1, y1, x2, y1 + 1, color);
        fill(matrix, x1, y2 - 1, x2, y2, color);
        fill(matrix, x1, y1, x1 + 1, y2, color);
        fill(matrix, x2 - 1, y1, x2, y2, color);
    }
}
