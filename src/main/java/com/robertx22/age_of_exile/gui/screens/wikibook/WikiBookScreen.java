package com.robertx22.age_of_exile.gui.screens.wikibook;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.gui.bases.INamedScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.wikibook.WikiLibrary;
import com.robertx22.age_of_exile.wikibook.WikiMarkdown;
import com.robertx22.age_of_exile.wikibook.WikiPage;
import com.robertx22.forge_compat.client.AbstractGui;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Полноэкранная внутриигровая вики.
 *
 * Слева — дерево категорий (или результаты поиска), в центре — markdown-страница
 * с гиперссылками, справа — «3D»-витрина предмета и связанные страницы.
 *
 * Всё рисование идёт через совместимый слой (AbstractGui), поэтому экран работает
 * и на клиенте без шейдерных хаков. Обрезка текста — по геометрии строк, а не ножницами
 * (scissor в совместимом слое нет).
 */
public class WikiBookScreen extends BaseScreen implements INamedScreen {

    private static final Identifier ICON = Identifier.fromNamespaceAndPath(SlashRef.MODID,
        "textures/gui/main_hub/icons/wiki.png");

    private static final int PAD = 8;
    private static final int TOP_H = 34;
    private static final int FOOT_H = 18;
    private static final int LINE = 10;

    private static final int COL_BG_TOP = 0xFF06090F;
    private static final int COL_BG_BOTTOM = 0xFF121C2B;
    private static final int COL_PANEL = 0xE60C1220;
    private static final int COL_PANEL_HI = 0xE6121A2B;
    private static final int COL_BORDER = 0xFF243149;
    private static final int COL_BORDER_SOFT = 0xFF18202F;
    private static final int COL_ACCENT = 0xFF52C7FF;
    private static final int COL_GOLD = 0xFFFFC857;
    private static final int COL_TEXT = 0xFFD8E3F2;
    private static final int COL_DIM = 0xFF93A3BA;
    private static final int COL_MUTED = 0xFF66748A;

    private enum Style {
        TEXT,
        H1,
        H2,
        H3,
        BULLET,
        QUOTE,
        TABLE_HEAD,
        TABLE_ROW,
        RULE
    }

    private record VisualLine(int y, int height, Style style, int indent,
                              List<WikiMarkdown.Span> spans, List<String> cells, int x) {
    }

    private record Hit(int x, int y, int w, int h, String action) {
        private boolean inside(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final Deque<String> historyBack = new ArrayDeque<>();
    private final Deque<String> historyForward = new ArrayDeque<>();
    private final List<Hit> hits = new ArrayList<>();

    private WikiLibrary library;
    private WikiPage page;
    private ItemStack itemStack = ItemStack.EMPTY;

    private int contentScroll;
    private int navScroll;
    private boolean searchFocused;
    private String query = "";

    private List<VisualLine> lines = List.of();
    private String linesFor;
    private int linesWidth = -1;
    private int maxContentScroll;

    private long openedAt = System.currentTimeMillis();
    private String flash;
    private long flashUntil;

    // геометрия кадра
    private int panelX, panelY, panelW, panelH;
    private int navX, navY, navW, navTop, navBottom;
    private int contentPanelX, contentPanelY, contentPanelW;
    private int textX, textW, contentTop, contentBottom;
    private int rightX, rightY, rightW, rightH;
    private int searchX, searchY, searchW, searchH;

    public WikiBookScreen() {
        super(320, 240);
    }

    public WikiBookScreen(String pageId) {
        super(320, 240);
        WikiPage target = WikiLibrary.get().page(pageId);
        if (target != null) {
            this.page = target;
            this.library = WikiLibrary.get();
        }
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

    @Override
    protected void init() {
        this.sizeX = this.width;
        this.sizeY = this.height;
        super.init();
        if (library == null) {
            library = WikiLibrary.get();
        }
        if (page == null) {
            page = library.home();
        }
        if (page != null) {
            refreshItem();
        }
    }

    // ── навигация ─────────────────────────────────────────────────────────────

    private void navigate(String id) {
        WikiPage target = library.page(id);
        if (target == null) {
            flash("Страница не найдена: " + id);
            return;
        }
        if (page != null && page.id.equals(target.id)) {
            contentScroll = 0;
            return;
        }
        if (page != null) {
            historyBack.push(page.id);
            if (historyBack.size() > 64) {
                historyBack.removeLast();
            }
        }
        historyForward.clear();
        open(target);
    }

    private void open(WikiPage target) {
        page = target;
        contentScroll = 0;
        navScroll = 0;
        linesFor = null;
        refreshItem();
    }

    private void goBack() {
        if (historyBack.isEmpty()) {
            return;
        }
        if (page != null) {
            historyForward.push(page.id);
        }
        WikiPage target = library.page(historyBack.pop());
        if (target != null) {
            open(target);
        }
    }

    private void goForward() {
        if (historyForward.isEmpty()) {
            return;
        }
        if (page != null) {
            historyBack.push(page.id);
        }
        WikiPage target = library.page(historyForward.pop());
        if (target != null) {
            open(target);
        }
    }

    private void randomPage() {
        if (library.pages.isEmpty()) {
            return;
        }
        WikiPage target = library.pages.get((int) (Math.random() * library.pages.size()));
        navigate(target.id);
    }

    private void flash(String message) {
        this.flash = message;
        this.flashUntil = System.currentTimeMillis() + 2600L;
    }

    /** Человеческое название первого предмета страницы: локализованное имя из ItemStack. */
    private String itemLabel(WikiPage source) {
        for (String id : source.items) {
            try {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
                if (item != null && item != ItemStack.EMPTY.getItem()) {
                    return new ItemStack(item).getHoverName().getString();
                }
            } catch (Throwable ignored) {
                // ниже отдадим сырой id
            }
        }
        return source.items.get(0);
    }

    private void refreshItem() {
        itemStack = ItemStack.EMPTY;
        if (page == null) {
            return;
        }
        for (String id : page.items) {
            try {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
                if (item != null && item != ItemStack.EMPTY.getItem()) {
                    itemStack = new ItemStack(item);
                    return;
                }
            } catch (Throwable ignored) {
                // битый id в .md не должен ломать экран
            }
        }
    }

    // ── ввод ──────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (hit.inside(mouseX, mouseY)) {
                    act(hit.action());
                    return true;
                }
            }
            searchFocused = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void act(String action) {
        if (action.equals("back")) {
            goBack();
        } else if (action.equals("forward")) {
            goForward();
        } else if (action.equals("random")) {
            randomPage();
        } else if (action.equals("close")) {
            this.onClose();
        } else if (action.equals("search")) {
            searchFocused = true;
        } else if (action.startsWith("page:")) {
            navigate(action.substring(5));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        int step = (int) (Math.signum(scroll) * 26);
        if (inside(mouseX, mouseY, navX, navTop, navW, navBottom - navTop)) {
            navScroll = clamp(navScroll - step, 0, maxNavScroll());
            return true;
        }
        contentScroll = clamp(contentScroll - step, 0, maxContentScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Esc
            if (searchFocused) {
                searchFocused = false;
                return true;
            }
            this.onClose();
            return true;
        }
        if (keyCode == 47) { // '/'
            searchFocused = true;
            return true;
        }
        if (searchFocused) {
            if (keyCode == 259) { // Backspace
                if (!query.isEmpty()) {
                    query = query.substring(0, query.length() - 1);
                    navScroll = 0;
                }
                return true;
            }
            if (keyCode == 257) { // Enter
                List<WikiPage> results = results();
                if (!results.isEmpty()) {
                    navigate(results.get(0).id);
                    searchFocused = false;
                }
                return true;
            }
        }
        if (keyCode == 263) {
            goBack();
            return true;
        }
        if (keyCode == 262) {
            goForward();
            return true;
        }
        if (keyCode == 265 || keyCode == 266) {
            contentScroll = clamp(contentScroll - 40, 0, maxContentScroll);
            return true;
        }
        if (keyCode == 264 || keyCode == 267) {
            contentScroll = clamp(contentScroll + 40, 0, maxContentScroll);
            return true;
        }
        if (KeybindsRegister.WIKI_KEY.matches(InputConstants.Type.KEYBOARD.getOrCreate(keyCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused && codePoint >= ' ' && query.length() < 40) {
            query += codePoint;
            navScroll = 0;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private List<WikiPage> results() {
        return library.search(query, 40);
    }

    // ── отрисовка ─────────────────────────────────────────────────────────────

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        hits.clear();
        layoutGeometry();
        drawBackdrop(pose);
        drawTopBar(pose, mouseX, mouseY);
        drawNav(pose, mouseX, mouseY);
        drawContent(pose, mouseX, mouseY);
        if (rightW > 0) {
            drawRightPanel(pose, mouseX, mouseY);
        }
        drawFooter(pose, mouseX, mouseY);
        drawHoverTip(pose, mouseX, mouseY);
    }

    private void layoutGeometry() {
        int w = Math.max(320, this.width);
        int h = Math.max(240, this.height);

        panelX = PAD;
        panelY = PAD;
        panelW = w - PAD * 2;
        panelH = h - PAD * 2;

        int leftW = w < 720 ? Math.max(116, panelW / 4) : 206;
        int right = w < 620 ? 0 : (w < 900 ? 166 : 198);

        navX = panelX + 6;
        navY = panelY + TOP_H + 4;
        navW = leftW - 12;
        navTop = navY;
        navBottom = panelY + panelH - FOOT_H - 4;

        contentPanelX = panelX + leftW + 4;
        contentPanelY = panelY + TOP_H + 4;
        contentPanelW = panelW - leftW - right - 8 - (right > 0 ? 4 : 0);

        textX = contentPanelX + 14;
        textW = Math.max(120, contentPanelW - 28);
        contentTop = contentPanelY;
        contentBottom = navBottom;

        rightX = panelX + panelW - right - 2;
        rightY = contentPanelY;
        rightW = right > 0 ? right - 8 : 0;
        rightH = contentBottom - contentTop;

        searchH = 14;
        searchY = panelY + 9;
        searchW = Math.max(90, Math.min(300, contentPanelW - 110));
        searchX = contentPanelX;
    }

    private void drawBackdrop(PoseStack pose) {
        int bands = 48;
        int bandH = Math.max(1, this.height / bands + 1);
        for (int i = 0; i < bands; i++) {
            int y1 = i * bandH;
            int y2 = Math.min(this.height, y1 + bandH);
            if (y1 >= this.height) {
                break;
            }
            AbstractGui.fill(pose, 0, y1, this.width, y2, lerp(COL_BG_TOP, COL_BG_BOTTOM, i / (float) (bands - 1)));
        }
        // мягкое свечение сверху
        for (int i = 0; i < 6; i++) {
            AbstractGui.fill(pose, 0, i, this.width, i + 1, (0x0A - i) << 24 | 0x52C7FF);
        }
        panel(pose, panelX, panelY, panelW, panelH, COL_PANEL, COL_BORDER);
        panel(pose, navX - 2, navY - 2, navW + 4, (navBottom - navY) + 4, COL_PANEL_HI, COL_BORDER_SOFT);
        panel(pose, contentPanelX - 2, contentPanelY - 2, contentPanelW + 4, (contentBottom - contentTop) + 4,
            0xE60A101B, COL_BORDER_SOFT);
        if (rightW > 0) {
            panel(pose, rightX - 2, rightY - 2, rightW + 4, rightH + 4, COL_PANEL_HI, COL_BORDER_SOFT);
        }
    }

    private void drawTopBar(PoseStack pose, int mouseX, int mouseY) {
        // логотип: на узких окнах длинный заголовок не влезает — оставляем только «ВИКИ»
        float logoScale = 1.15f;
        int longWidth = (int) (mc.font.width("ЭПОХА ИЗГНАННИКА") * logoScale)
            + 6 + mc.font.width("ВИКИ");
        boolean fits = panelX + 12 + longWidth < searchX - 6;
        if (fits) {
            scaledText(pose, "ЭПОХА ИЗГНАННИКА", panelX + 12, panelY + 11, logoScale, COL_GOLD);
            AbstractGui.drawString(mc.font, pose, "ВИКИ",
                panelX + 18 + (int) (mc.font.width("ЭПОХА ИЗГНАННИКА") * logoScale), panelY + 13, COL_ACCENT);
        } else {
            AbstractGui.drawString(mc.font, pose, "ВИКИ", panelX + 12, panelY + 13, COL_GOLD);
        }

        // строка поиска
        boolean hovered = inside(mouseX, mouseY, searchX, searchY, searchW, searchH);
        AbstractGui.fill(pose, searchX, searchY, searchX + searchW, searchY + searchH,
            searchFocused ? 0xCC14243A : (hovered ? 0xCC111C2E : 0xB30C1422));
        border(pose, searchX, searchY, searchW, searchH,
            searchFocused ? COL_ACCENT : (hovered ? 0xFF35507A : COL_BORDER_SOFT));
        AbstractGui.drawString(mc.font, pose, "/", searchX + 4, searchY + 3, COL_MUTED);
        String shown = query.isEmpty() ? "поиск по вики…" : query;
        int color = query.isEmpty() ? COL_MUTED : COL_TEXT;
        AbstractGui.drawString(mc.font, pose, shown, searchX + 13, searchY + 3, color);
        if (searchFocused && (System.currentTimeMillis() / 500L) % 2 == 0) {
            int caret = searchX + 13 + mc.font.width(query);
            AbstractGui.fill(pose, caret, searchY + 3, caret + 1, searchY + 11, COL_ACCENT);
        }
        hits.add(new Hit(searchX, searchY, searchW, searchH, "search"));

        // кнопки справа
        int bx = panelX + panelW - 8;
        bx = iconButton(pose, "✕", bx, panelY + 8, "close", mouseX, mouseY, 0xFFE06C75);
        bx = iconButton(pose, "✦", bx, panelY + 8, "random", mouseX, mouseY, COL_GOLD);
        bx = iconButton(pose, "›", bx, panelY + 8, "forward", mouseX, mouseY,
            historyForward.isEmpty() ? COL_MUTED : COL_TEXT);
        bx = iconButton(pose, "‹", bx, panelY + 8, "back", mouseX, mouseY,
            historyBack.isEmpty() ? COL_MUTED : COL_TEXT);

        AbstractGui.fill(pose, panelX + 6, panelY + TOP_H - 3, panelX + panelW - 6, panelY + TOP_H - 2,
            COL_BORDER_SOFT);
    }

    private int iconButton(PoseStack pose, String glyph, int right, int y, String action,
                           int mouseX, int mouseY, int color) {
        int size = 18;
        int x = right - size;
        boolean hovered = inside(mouseX, mouseY, x, y, size, size);
        if (hovered) {
            AbstractGui.fill(pose, x, y, x + size, y + size, 0x332F4A70);
            border(pose, x, y, size, size, 0xFF35507A);
        }
        AbstractGui.drawCenteredString(pose, mc.font, glyph, x + size / 2, y + 5, color);
        hits.add(new Hit(x, y, size, size, action));
        return x - 4;
    }

    private void drawNav(PoseStack pose, int mouseX, int mouseY) {
        int rowH = 13;
        int y = navTop - navScroll;
        boolean searching = !query.isEmpty();

        if (searching) {
            List<WikiPage> found = results();
            AbstractGui.drawString(mc.font, pose, "РЕЗУЛЬТАТЫ · " + found.size(), navX, y, COL_ACCENT);
            y += 14;
            if (found.isEmpty()) {
                AbstractGui.drawString(mc.font, pose, "ничего не найдено", navX, y, COL_MUTED);
                return;
            }
            for (WikiPage result : found) {
                y = navRow(pose, result, y, rowH, mouseX, mouseY, "› " + result.title, false);
            }
            return;
        }

        for (WikiLibrary.Category category : library.categories) {
            if (y + 12 >= navTop && y <= navBottom) {
                AbstractGui.drawString(mc.font, pose, category.title().toUpperCase(Locale.ROOT), navX, y,
                    COL_ACCENT);
                AbstractGui.fill(pose, navX, y + 10, navX + navW, y + 11, COL_BORDER_SOFT);
            }
            y += 13;
            for (WikiPage categoryPage : category.pages()) {
                if (y > navBottom) {
                    break;
                }
                y = navRow(pose, categoryPage, y, rowH, mouseX, mouseY, null, true);
            }
            y += 4;
        }

        int contentHeight = y + navScroll - navTop;
        int view = navBottom - navTop;
        if (contentHeight > view) {
            int barH = Math.max(12, view * view / contentHeight);
            int barY = navTop + (int) ((view - barH) * (navScroll / (float) Math.max(1, contentHeight - view)));
            AbstractGui.fill(pose, navX + navW + 1, navTop, navX + navW + 3, navBottom, 0x33000000);
            AbstractGui.fill(pose, navX + navW + 1, barY, navX + navW + 3, barY + barH, COL_ACCENT);
        }
    }

    private int navRow(PoseStack pose, WikiPage target, int y, int rowH, int mouseX, int mouseY,
                       String overrideLabel, boolean showMark) {
        if (y + rowH < navTop || y > navBottom) {
            return y + rowH;
        }
        boolean selected = page != null && page.id.equals(target.id);
        boolean hovered = inside(mouseX, mouseY, navX, y, navW, rowH - 1);
        if (selected) {
            AbstractGui.fill(pose, navX - 2, y - 1, navX + navW + 2, y + rowH - 1, 0x3352C7FF);
            AbstractGui.fill(pose, navX - 2, y - 1, navX, y + rowH - 1, COL_ACCENT);
        } else if (hovered) {
            AbstractGui.fill(pose, navX - 2, y - 1, navX + navW + 2, y + rowH - 1, 0x1FFFFFFF);
        }
        String label = overrideLabel != null ? overrideLabel
            : (showMark ? (selected ? "▸ " : "· ") : "") + target.shortTitle();
        AbstractGui.drawString(mc.font, pose, trim(label, navW - 6), navX + 2, y + 2,
            selected ? 0xFFFFFFFF : (hovered ? COL_TEXT : COL_DIM));
        hits.add(new Hit(navX - 2, y - 1, navW + 4, rowH, "page:" + target.id));
        return y + rowH;
    }

    private void drawContent(PoseStack pose, int mouseX, int mouseY) {
        if (page == null) {
            return;
        }
        int headerY = contentTop + 8;

        scaledText(pose, trim(page.title, (int) (textW / 1.3f)), textX, headerY, 1.3f, COL_GOLD);
        int titleH = 15;

        if (!page.titleEn.isEmpty()) {
            AbstractGui.drawString(mc.font, pose, trim(page.titleEn, textW), textX, headerY + titleH, COL_DIM);
            titleH += 11;
        }

        // чипы: категория / количество связанных страниц / предмет
        int chipX = textX;
        int chipY = headerY + titleH + 2;
        WikiLibrary.Category category = library.categoryOf(page);
        chipX = chip(pose, category == null ? "вики" : category.title().toLowerCase(Locale.ROOT), chipX, chipY,
            COL_ACCENT);
        if (!page.items.isEmpty()) {
            chipX = chip(pose, itemLabel(page), chipX, chipY, COL_GOLD);
        }
        List<WikiPage> related = library.backlinksOf(page.id);
        if (!related.isEmpty()) {
            chip(pose, "упоминается в " + related.size(), chipX, chipY, COL_DIM);
        }

        int bodyTop = chipY + 16;
        AbstractGui.fill(pose, contentPanelX + 6, bodyTop - 5, contentPanelX + contentPanelW - 6, bodyTop - 4,
            COL_BORDER_SOFT);

        ensureLines();
        int viewTop = bodyTop;
        int viewBottom = contentBottom - 4;
        int viewHeight = Math.max(10, viewBottom - viewTop);

        int totalHeight = lines.isEmpty() ? 0
            : lines.get(lines.size() - 1).y() + lines.get(lines.size() - 1).height() + 12;
        maxContentScroll = Math.max(0, totalHeight - viewHeight);
        contentScroll = clamp(contentScroll, 0, maxContentScroll);

        int originY = viewTop - contentScroll;
        for (VisualLine line : lines) {
            int y = originY + line.y();
            int lineH = line.height();
            if (y + lineH < viewTop || y > viewBottom) {
                continue;
            }
            drawLine(pose, line, textX + line.x(), y, mouseX, mouseY);
        }

        // полоса прокрутки
        if (totalHeight > viewHeight) {
            int barH = Math.max(14, viewHeight * viewHeight / totalHeight);
            int barY = viewTop + (int) ((viewHeight - barH) *
                (contentScroll / (float) Math.max(1, totalHeight - viewHeight)));
            AbstractGui.fill(pose, contentPanelX + contentPanelW - 5, viewTop, contentPanelX + contentPanelW - 3,
                viewBottom, 0x33000000);
            AbstractGui.fill(pose, contentPanelX + contentPanelW - 5, barY, contentPanelX + contentPanelW - 3,
                barY + barH, 0xAA52C7FF);
        }

        if (contentScroll > 0 && totalHeight > viewHeight) {
            int percent = Math.round(100f * contentScroll / Math.max(1, totalHeight - viewHeight));
            String label = percent + "%";
            AbstractGui.drawString(mc.font, pose, label,
                contentPanelX + contentPanelW - 6 - mc.font.width(label), contentPanelY + 2, COL_MUTED);
        }
    }

    private void drawLine(PoseStack pose, VisualLine line, int x, int y, int mouseX, int mouseY) {
        Style style = line.style();
        if (style == Style.RULE) {
            AbstractGui.fill(pose, x, y + 2, x + textW, y + 3, COL_BORDER);
            return;
        }
        if (style == Style.TABLE_HEAD || style == Style.TABLE_ROW) {
            drawTableRow(pose, line, x, y);
            return;
        }

        int textY = y;
        int indent = 0;
        switch (style) {
            case H1 -> {
                scaledText(pose, WikiMarkdown.plainOf(line.spans()), x, y, 1.22f, COL_GOLD);
                AbstractGui.fill(pose, x, y + 14, Math.min(x + 120, x + textW), y + 15, 0x6652C7FF);
                return;
            }
            case H2 -> {
                AbstractGui.fill(pose, x - 5, y + 2, x - 3, y + 9, COL_ACCENT);
                for (WikiMarkdown.Span span : line.spans()) {
                    AbstractGui.drawString(mc.font, pose, span.text(), x, y, COL_ACCENT);
                    x += mc.font.width(span.text());
                }
                return;
            }
            case H3 -> {
                for (WikiMarkdown.Span span : line.spans()) {
                    AbstractGui.drawString(mc.font, pose, span.text(), x, y, COL_TEXT);
                    x += mc.font.width(span.text());
                }
                return;
            }
            case QUOTE -> {
                AbstractGui.fill(pose, x - 6, y - 1, x + textW + 6, y + LINE, 0x2A2B5C8C);
                AbstractGui.fill(pose, x - 6, y - 1, x - 4, y + LINE, 0xAA52C7FF);
                textY = y;
            }
            case BULLET -> {
                AbstractGui.drawString(mc.font, pose, "•", x, y, COL_ACCENT);
                indent = line.indent() > 0 ? 10 : 8;
            }
            default -> {
            }
        }

        int cursor = x + indent;
        int colorOverride = style == Style.QUOTE ? 0xFFA9C8DC : 0;
        for (WikiMarkdown.Span span : line.spans()) {
            int width = mc.font.width(span.text());
            int color = colorOverride != 0 && span.color() == WikiMarkdown.C_TEXT ? colorOverride : span.color();
            if (span.bold()) {
                color = brighten(color);
            }
            boolean link = span.isLink();
            boolean hovered = link && inside(mouseX, mouseY, cursor, textY - 1, Math.max(4, width), LINE - 1);
            int drawColor = hovered ? WikiMarkdown.C_LINK_HOVER : color;
            if (link) {
                if (hovered) {
                    AbstractGui.fill(pose, cursor, textY - 1, cursor + width, textY + LINE - 1, 0x22FFE08A);
                }
                AbstractGui.fill(pose, cursor, textY + 8, cursor + width, textY + 9, hovered ? 0xFFFFE08A : 0x7752C7FF);
                hits.add(new Hit(cursor, textY - 1, Math.max(4, width), LINE - 1, "page:" + span.link()));
            }
            AbstractGui.drawString(mc.font, pose, span.text(), cursor, textY, drawColor);
            if (span.italic()) {
                AbstractGui.drawString(mc.font, pose, span.text(), cursor + 1, textY, 0x55000000);
            }
            cursor += width;
        }
    }

    private void drawTableRow(PoseStack pose, VisualLine line, int x, int y) {
        List<String> cells = line.cells();
        if (cells.isEmpty()) {
            return;
        }
        int cols = cells.size();
        int colW = Math.max(24, textW / cols);
        boolean head = line.style() == Style.TABLE_HEAD;
        int rowH = 11;
        AbstractGui.fill(pose, x - 3, y - 1, x + Math.min(textW, colW * cols) + 3, y + rowH, 
            head ? 0x332B5C8C : (line.indent() % 2 == 0 ? 0x14FFFFFF : 0x08FFFFFF));
        for (int i = 0; i < cols; i++) {
            String cell = trim(cells.get(i), colW - 4);
            AbstractGui.drawString(mc.font, pose, cell, x + i * colW, y, head ? COL_GOLD : COL_TEXT);
        }
        if (head) {
            AbstractGui.fill(pose, x - 3, y + rowH - 1, x + Math.min(textW, colW * cols) + 3, y + rowH,
                COL_BORDER_SOFT);
        }
    }

    private void drawRightPanel(PoseStack pose, int mouseX, int mouseY) {
        int top = rightY + 6;
        AbstractGui.drawString(mc.font, pose, "ВИТРИНА", rightX + 6, top, COL_ACCENT);
        AbstractGui.drawString(mc.font, pose, page != null && page.items.isEmpty() ? "нет предмета" : "3D-поворот",
            rightX + 6, top + 10, COL_MUTED);

        int centerX = rightX + rightW / 2;
        int centerY = top + 66;
        drawTurntable(pose, centerX, centerY);

        int y = top + 108;
        String name = page != null ? page.title : "—";
        for (String piece : wrapPlain(name, rightW - 8)) {
            AbstractGui.drawCenteredString(pose, mc.font, piece, centerX, y, COL_GOLD);
            y += 10;
        }
        if (page != null && !page.titleEn.isEmpty()) {
            for (String piece : wrapPlain(page.titleEn, rightW - 8)) {
                AbstractGui.drawCenteredString(pose, mc.font, piece, centerX, y, COL_DIM);
                y += 9;
            }
        }
        if (!itemStack.isEmpty()) {
            y += 2;
            AbstractGui.drawCenteredString(pose, mc.font, "◆ предмет в каталоге", centerX, y, COL_MUTED);
            y += 11;
        }

        y += 4;
        AbstractGui.fill(pose, rightX + 4, y - 4, rightX + rightW - 4, y - 3, COL_BORDER_SOFT);

        if (page != null) {
            WikiPage previous = library.neighbor(page, -1);
            WikiPage next = library.neighbor(page, +1);
            if (previous != null) {
                y = linkRow(pose, "← " + previous.shortTitle(), rightX + 6, y, rightW - 12,
                    "page:" + previous.id, mouseX, mouseY);
            }
            if (next != null) {
                y = linkRow(pose, "→ " + next.shortTitle(), rightX + 6, y, rightW - 12,
                    "page:" + next.id, mouseX, mouseY);
            }

            List<WikiPage> related = library.backlinksOf(page.id);
            if (!related.isEmpty()) {
                y += 4;
                AbstractGui.drawString(mc.font, pose, "ССЫЛАЮТСЯ СЮДА", rightX + 6, y, COL_ACCENT);
                y += 12;
                int shown = 0;
                for (WikiPage relatedPage : related) {
                    if (shown >= 7 || y > rightY + rightH - 12) {
                        break;
                    }
                    y = linkRow(pose, "· " + relatedPage.shortTitle(), rightX + 6, y, rightW - 12,
                        "page:" + relatedPage.id, mouseX, mouseY);
                    shown++;
                }
            }
        }

        AbstractGui.drawString(mc.font, pose, "стр. " + (page == null ? 0 : library.pages.indexOf(page) + 1)
            + "/" + library.pages.size(), rightX + 6, rightY + rightH - 11, COL_MUTED);
    }

    private int linkRow(PoseStack pose, String label, int x, int y, int width, String action,
                        int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, 10);
        if (hovered) {
            AbstractGui.fill(pose, x - 2, y - 1, x + width + 2, y + 10, 0x22FFE08A);
        }
        AbstractGui.drawString(mc.font, pose, trim(label, width), x, y, hovered ? WikiMarkdown.C_LINK_HOVER : COL_DIM);
        hits.add(new Hit(x - 2, y - 1, width + 4, 11, action));
        return y + 11;
    }

    private void drawTurntable(PoseStack pose, int cx, int cy) {
        float time = (System.currentTimeMillis() - openedAt) / 1000f;

        // пьедестал: три «орбиты» из точек + подставка
        for (int ring = 0; ring < 3; ring++) {
            float radius = 26f + ring * 9f;
            float phase = time * (0.5f + ring * 0.25f);
            int alpha = 0x55 - ring * 0x14;
            for (int i = 0; i < 24; i++) {
                double angle = phase + i * (Math.PI * 2 / 24);
                int px = cx + (int) (Math.cos(angle) * radius);
                int py = cy + 26 + (int) (Math.sin(angle) * radius * 0.26f);
                int color = ((alpha & 0xFF) << 24) | (ring == 1 ? 0xFFC857 : 0x52C7FF);
                AbstractGui.fill(pose, px, py, px + 2, py + 1, color);
            }
        }
        int pedestalY = cy + 28;
        AbstractGui.fill(pose, cx - 34, pedestalY, cx + 34, pedestalY + 1, 0x5552C7FF);
        AbstractGui.fill(pose, cx - 24, pedestalY + 2, cx + 24, pedestalY + 3, 0x3352C7FF);

        if (itemStack.isEmpty()) {
            scaledText(pose, "✦", cx - 8, cy - 10, 3.0f, 0x66FFC857);
            AbstractGui.drawCenteredString(pose, mc.font, "без модели", cx, cy + 14, COL_MUTED);
            return;
        }

        float angle = time * 1.4f;
        float cos = (float) Math.cos(angle);
        float magnitude = Math.max(0.16f, Math.abs(cos));
        float scale = 4.0f;
        boolean drawn = false;
        var graphics = AbstractGui.currentGraphics();
        if (graphics != null) {
            boolean pushed = false;
            try {
                graphics.pose().pushMatrix();
                pushed = true;
                graphics.pose().translate(cx, cy + (float) Math.sin(time * 2.1f) * 2f);
                graphics.pose().scale(magnitude * scale * (cos >= 0 ? 1f : -1f), scale);
                AbstractGui.renderFakeItem(itemStack, -8, -8);
                drawn = true;
            } catch (Throwable ignored) {
                drawn = false;
            } finally {
                if (pushed) {
                    try {
                        graphics.pose().popMatrix();
                    } catch (Throwable ignored) {
                        // поза могла уже развалиться — не даём вики упасть
                    }
                }
            }
        }
        if (!drawn) {
            AbstractGui.renderFakeItem(itemStack, cx - 8, cy - 8);
        }
        AbstractGui.drawCenteredString(pose, mc.font, "◐", cx - 40, cy + 22, 0x66FFC857);
        AbstractGui.drawCenteredString(pose, mc.font, "◑", cx + 34, cy + 22, 0x66FFC857);
    }

    private void drawFooter(PoseStack pose, int mouseX, int mouseY) {
        int y = panelY + panelH - FOOT_H + 5;
        String hint = "ЛКМ — ссылка · колесо — прокрутка · / — поиск · ←/→ — история · P / Esc — закрыть";
        AbstractGui.drawString(mc.font, pose, trim(hint, panelW - 150), panelX + 10, y, COL_MUTED);
        String stats = library.pages.size() + " стр. · " + (library.totalChars() / 1000) + "k симв.";
        AbstractGui.drawString(mc.font, pose, stats, panelX + panelW - 10 - mc.font.width(stats), y, COL_MUTED);

        if (flash != null && System.currentTimeMillis() < flashUntil) {
            int w = mc.font.width(flash) + 10;
            AbstractGui.fill(pose, panelX + panelW / 2 - w / 2, y - 12, panelX + panelW / 2 + w / 2, y + 1,
                0xDD442222);
            AbstractGui.drawCenteredString(pose, mc.font, flash, panelX + panelW / 2, y - 9, 0xFFFFD7D7);
        }
        if (!library.error.isEmpty()) {
            AbstractGui.drawString(mc.font, pose, "! " + library.error, panelX + 10, y - 22, 0xFFFF9A9A);
        }
    }

    private void drawHoverTip(PoseStack pose, int mouseX, int mouseY) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (!hit.action().startsWith("page:") || !hit.inside(mouseX, mouseY)) {
                continue;
            }
            WikiPage target = library.page(hit.action().substring(5));
            if (target == null) {
                return;
            }
            String text = "→ " + target.title;
            int width = mc.font.width(text) + 8;
            int x = Math.min(mouseX + 8, this.width - width - 2);
            int y = mouseY + 10;
            AbstractGui.fill(pose, x, y, x + width, y + 12, 0xF00A1220);
            border(pose, x, y, width, 12, 0xFF52C7FF);
            AbstractGui.drawString(mc.font, pose, text, x + 4, y + 2, COL_TEXT);
            return;
        }
    }

    // ── раскладка текста ──────────────────────────────────────────────────────

    private void ensureLines() {
        if (page == null) {
            lines = List.of();
            return;
        }
        if (page.id.equals(linesFor) && textW == linesWidth) {
            return;
        }
        lines = buildLines(page, textW);
        linesFor = page.id;
        linesWidth = textW;
        contentScroll = 0;
    }

    private List<VisualLine> buildLines(WikiPage source, int width) {
        List<VisualLine> out = new ArrayList<>();
        int y = 0;
        for (WikiMarkdown.Block block : source.blocks) {
            switch (block.kind()) {
                case HEADING -> {
                    int level = Math.max(1, Math.min(3, block.level()));
                    int height = level == 1 ? 17 : (level == 2 ? 13 : 11);
                    y += level == 1 ? 8 : 5;
                    out.add(new VisualLine(y, height, level == 1 ? Style.H1 : (level == 2 ? Style.H2 : Style.H3),
                        0, block.spans(), List.of(), 0));
                    y += height + 2;
                }
                case PARAGRAPH -> {
                    for (List<WikiMarkdown.Span> row : wrap(block.spans(), width)) {
                        out.add(new VisualLine(y, LINE, Style.TEXT, 0, row, List.of(), 0));
                        y += LINE;
                    }
                    y += 5;
                }
                case BULLET -> {
                    boolean first = true;
                    for (List<WikiMarkdown.Span> row : wrap(block.spans(), width - 10)) {
                        out.add(new VisualLine(y, LINE, Style.BULLET, first ? 0 : 1, row, List.of(), 0));
                        y += LINE;
                        first = false;
                    }
                    y += 3;
                }
                case QUOTE -> {
                    y += 3;
                    for (List<WikiMarkdown.Span> row : wrap(block.spans(), width - 12)) {
                        out.add(new VisualLine(y, LINE, Style.QUOTE, 0, row, List.of(), 4));
                        y += LINE;
                    }
                    y += 6;
                }
                case TABLE -> {
                    List<List<String>> rows = block.rows();
                    int index = 0;
                    y += 3;
                    for (List<String> row : rows) {
                        Style style = index == 0 ? Style.TABLE_HEAD : Style.TABLE_ROW;
                        out.add(new VisualLine(y, 11, style, index, List.of(), row, 0));
                        y += 12;
                        index++;
                    }
                    y += 6;
                }
                case RULE -> {
                    y += 6;
                    out.add(new VisualLine(y, 5, Style.RULE, 0, List.of(), List.of(), 0));
                    y += 9;
                }
                default -> {
                }
            }
        }
        return out;
    }

    private List<List<WikiMarkdown.Span>> wrap(List<WikiMarkdown.Span> spans, int maxWidth) {
        List<List<WikiMarkdown.Span>> rows = new ArrayList<>();
        List<WikiMarkdown.Span> current = new ArrayList<>();
        int width = 0;
        Font font = mc.font;

        for (WikiMarkdown.Span span : spans) {
            String text = span.text();
            String[] words = text.split(" ");
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                if (word.isEmpty()) {
                    continue;
                }
                int wordWidth = font.width(word);
                int spaceWidth = width > 0 ? font.width(" ") : 0;
                if (width > 0 && width + spaceWidth + wordWidth > maxWidth) {
                    rows.add(current);
                    current = new ArrayList<>();
                    width = 0;
                    spaceWidth = 0;
                }
                if (width > 0) {
                    append(current, " ", span);
                    width += spaceWidth;
                }
                append(current, word, span);
                width += wordWidth;
            }
        }
        if (!current.isEmpty()) {
            rows.add(current);
        }
        return rows;
    }

    private void append(List<WikiMarkdown.Span> target, String text, WikiMarkdown.Span style) {
        if (!target.isEmpty()) {
            WikiMarkdown.Span last = target.get(target.size() - 1);
            if (last.color() == style.color() && last.bold() == style.bold()
                && last.italic() == style.italic()
                && java.util.Objects.equals(last.link(), style.link())) {
                target.set(target.size() - 1, new WikiMarkdown.Span(last.text() + text, last.color(),
                    last.bold(), last.italic(), last.link()));
                return;
            }
        }
        target.add(new WikiMarkdown.Span(text, style.color(), style.bold(), style.italic(), style.link()));
    }

    private List<String> wrapPlain(String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String next = current.length() == 0 ? word : current + " " + word;
            if (mc.font.width(next) > width && current.length() > 0) {
                out.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(next);
            }
        }
        if (current.length() > 0) {
            out.add(current.toString());
        }
        if (out.isEmpty()) {
            out.add("");
        }
        return out;
    }

    private int maxNavScroll() {
        int total = 0;
        for (WikiLibrary.Category category : library.categories) {
            total += 13 + category.pages().size() * 13 + 4;
        }
        return Math.max(0, total - (navBottom - navTop));
    }

    // ── утилиты ───────────────────────────────────────────────────────────────

    private int chip(PoseStack pose, String text, int x, int y, int color) {
        int width = mc.font.width(text) + 8;
        if (x + width > contentPanelX + contentPanelW - 8) {
            return x;
        }
        AbstractGui.fill(pose, x, y, x + width, y + 12, (color & 0x00FFFFFF) | 0x22000000);
        border(pose, x, y, width, 12, (color & 0x00FFFFFF) | 0x66000000);
        AbstractGui.drawString(mc.font, pose, trim(text, width - 6), x + 4, y + 2, color);
        return x + width + 4;
    }

    private void panel(PoseStack pose, int x, int y, int w, int h, int fill, int border) {
        AbstractGui.fill(pose, x, y, x + w, y + h, fill);
        border(pose, x, y, w, h, border);
    }

    private void border(PoseStack pose, int x, int y, int w, int h, int color) {
        AbstractGui.fill(pose, x, y, x + w, y + 1, color);
        AbstractGui.fill(pose, x, y + h - 1, x + w, y + h, color);
        AbstractGui.fill(pose, x, y, x + 1, y + h, color);
        AbstractGui.fill(pose, x + w - 1, y, x + w, y + h, color);
    }

    private void scaledText(PoseStack pose, String text, int x, int y, float scale, int color) {
        pose.pushPose();
        pose.scale(scale, scale, 1f);
        AbstractGui.drawString(mc.font, pose, text, x / scale, y / scale, color);
        pose.popPose();
    }

    private static int lerp(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((from >> 24) & 0xFF) + (((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * t);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int brighten(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 26);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 26);
        int b = Math.min(255, (color & 0xFF) + 26);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String trim(String text, int width) {
        if (text == null) {
            return "";
        }
        if (mc.font.width(text) <= width) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (mc.font.width(sb.toString() + text.charAt(i) + "…") > width) {
                break;
            }
            sb.append(text.charAt(i));
        }
        return sb + "…";
    }
}
