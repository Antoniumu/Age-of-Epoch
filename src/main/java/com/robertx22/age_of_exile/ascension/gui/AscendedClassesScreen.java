package com.robertx22.age_of_exile.ascension.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.ascension.AscensionClasses;
import com.robertx22.age_of_exile.ascension.AscensionData;
import com.robertx22.age_of_exile.ascension.AscensionLogic;
import com.robertx22.age_of_exile.ascension.packets.AscensionAllocPacket;
import com.robertx22.age_of_exile.ascension.packets.AscensionHotbarPacket;
import com.robertx22.age_of_exile.ascension.packets.AscensionPickPacket;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.gui.bases.INamedScreen;
import com.robertx22.age_of_exile.gui.screens.spell.SpellScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран Вознесённых классов (50+). Три режима:
 *
 * <ul>
 *   <li><b>Замок</b> (уровень &lt; 50): серый силуэт пути, большой замок и
 *   красная строка «У вас ещё нет 50-го уровня».</li>
 *   <li><b>Выбор пути</b>: карточки классов с эмблемами.</li>
 *   <li><b>Путь класса</b>: линейная цепочка из 6 узлов волной; неоткрытые
 *   узлы скрыты «?», тема GUI (фон, цвета, потоки) — от выбранного класса.</li>
 * </ul>
 *
 * Открывается из Навыков кнопкой «✦ Классы» через {@link AscensionTransitionScreen}.
 */
public class AscendedClassesScreen extends BaseScreen implements INamedScreen {

    private static final Identifier LOCK_TEX = SlashRef.guiId("ascension/lock");

    static int sizeX = 250;
    static int sizeY = 233;

    private static final int NODE = 26;

    private final long openedAt = System.currentTimeMillis();

    private String statusMessage = "";
    private long statusMessageTime = 0L;

    /**
     * Режим «просмотр списка» — пригодится, когда классов станет больше одного.
     */
    private boolean browsingList = false;

    private final Minecraft mc = Minecraft.getInstance();

    public AscendedClassesScreen() {
        super(sizeX, sizeY);
    }

    private static class Node {
        String spellId;
        int index;
        int x, y;

        boolean isInside(int mx, int my) {
            return GuiUtils.isInRect(x, y, NODE, NODE, mx, my);
        }
    }

    private final List<Node> nodes = new ArrayList<>();

    @Override
    public Identifier iconLocation() {
        return AscensionClasses.TIDE_DANCER.getEmblemLoc();
    }

    @Override
    public Words screenName() {
        return Words.Ascension;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private AscensionClasses currentClass() {
        AscensionData data = AscensionLogic.data(mc.player);
        if (data.hasChosenClass() && !browsingList) {
            return data.getChosenClass();
        }
        return null;
    }

    private boolean isUnlocked() {
        return AscensionLogic.canPickClass(mc.player);
    }

    @Override
    public void init() {
        super.init();
        this.buttons.clear();
        this.children.clear();
        this.nodes.clear();

        // Назад, к Навыкам — всегда доступно.
        addButton(new Button(guiLeft + 6, guiTop + 6, 64, 14,
            Component.literal("← Навыки"), b -> {
            mc.gui.setScreen(new AscensionTransitionScreen(new SpellScreen(), guiLeft + 38, guiTop + 13));
        }));

        if (!isUnlocked()) {
            return;
        }

        AscensionClasses cls = currentClass();

        if (cls == null) {
            // карточки выбора класса
            int cardX = guiLeft + 33;
            int cardY = guiTop + 52;
            for (AscensionClasses c : AscensionClasses.listAll()) {
                addButton(new Button(cardX + 30, cardY + 118, 130, 16,
                    Component.literal("§bВыбрать путь"), b -> {
                    Packets.sendToServer(new AscensionPickPacket(c.id));
                    showStatus("§bПрилив откликается…");
                    // оптимистичный переход — сервер пришлёт подтверждённые данные
                    Load.playerRPGData(mc.player).ascension.chosen_class = c.id;
                    browsingList = false;
                    init();
                }));
                cardX += 0; // пока класс один
            }
            return;
        }

        // путь класса: узлы линейной цепочки (волной)
        int baseY = guiTop + 126;
        for (int i = 0; i < cls.skillIds.size(); i++) {
            Node n = new Node();
            n.spellId = cls.skillIds.get(i);
            n.index = i;
            n.x = guiLeft + 22 + i * 39;
            n.y = baseY + (int) Math.round(Math.sin(i * 1.25D) * 30.0D);
            nodes.add(n);
        }

        // вернуться к списку классов (для будущих путей и пересмены)
        addButton(new Button(guiLeft + sizeX - 62, guiTop + 6, 56, 14,
            Component.literal("Классы"), b -> {
            browsingList = true;
            init();
        }));
    }

    private void showStatus(String msg) {
        this.statusMessage = msg == null ? "" : msg;
        this.statusMessageTime = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        AscensionClasses cls = currentClass();
        if (cls != null && isUnlocked()) {
            for (Node n : nodes) {
                if (n.isInside((int) mouseX, (int) mouseY)) {
                    if (button == 0) {
                        AscensionData data = AscensionLogic.data(mc.player);
                        boolean revealed = AscensionLogic.isNodeRevealed(data, cls, n.index);
                        if (!revealed || AscensionLogic.freePoints(mc.player) < 1) {
                            showStatus(revealed ? "§cНет свободных очков Вознесения." : "§8Это умение ещё скрыто туманом глубин.");
                            return true;
                        }
                        Packets.sendToServer(new AscensionAllocPacket(n.spellId));
                        showStatus("§bОчко вложено. Поток послушен.");
                    } else if (button == 1) {
                        AscensionData data = AscensionLogic.data(mc.player);
                        if (data.rankOf(n.spellId) > 0 && Load.spells(mc.player) != null) {
                            int slot = AscensionLogic.firstFreeHotbarSlot(mc.player, n.spellId);
                            // локально назначаем сразу, сервер подтвердит синхронизацией
                            Load.spells(mc.player)
                                .getSpellsData().hotbars.entrySet()
                                .removeIf(e -> e.getValue()
                                    .equals(n.spellId));
                            Load.spells(mc.player)
                                .getSpellsData().hotbars.put(slot, n.spellId);
                            Packets.sendToServer(new AscensionHotbarPacket(n.spellId, slot));
                            showStatus("§bУмение назначено в ячейку #" + (slot + 1));
                        } else {
                            showStatus("§8Сначала изучите это умение.");
                        }
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ============================ РЕНДЕР ============================

    @Override
    public void render(PoseStack matrix, int x, int y, float ticks) {
        AscensionClasses theme = currentClass();
        if (theme == null) {
            theme = AscensionClasses.TIDE_DANCER; // тема по умолчанию — водный поток
        }

        // фон класса
        AbstractGui.bindTexture(theme.getBackgroundLoc());
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, guiLeft, guiTop, 0, 0, sizeX, sizeY);

        renderLivingWater(matrix, theme);

        if (!isUnlocked()) {
            renderLocked(matrix, x, y);
        } else if (currentClass() == null) {
            renderClassCards(matrix, x, y);
        } else {
            renderPath(matrix, x, y, currentClass());
        }

        super.render(matrix, x, y, ticks);

        // шапка
        String title = currentClass() == null ? "ВОЗНЕСЁННЫЕ КЛАССЫ" : currentClass().locName;
        GuiUtils.renderScaledText(matrix, guiLeft + 132, guiTop + 22, 0.95D, title,
            currentClass() == null ? ChatFormatting.AQUA : ChatFormatting.BLUE);

        if (isUnlocked()) {
            int free = AscensionLogic.freePoints(mc.player);
            ChatFormatting col = free > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            GuiUtils.renderScaledText(matrix, guiLeft + 200, guiTop + 220, 0.95D, "Очки Вознесения: " + free, col);
        }

        if (!this.statusMessage.isEmpty() && System.currentTimeMillis() - this.statusMessageTime < 4000L) {
            GuiUtils.renderScaledText(matrix, guiLeft + sizeX / 2, guiTop + sizeY + 10, 0.9D,
                net.minecraft.network.chat.Component.literal(statusMessage)
                    .getString(), ChatFormatting.YELLOW);
        }

        renderIntro(matrix);

        // тултипы — поверх всего
        if (currentClass() != null && isUnlocked()) {
            renderNodeTooltips(matrix, x, y, currentClass());
        }
    }

    /**
     * Анимированный «водный поток» поверх фона: бегущие блики-синусоиды,
     * дрейфующие пузыри, пульс эмблемы. Всё процедурно — без лишних текстур.
     */
    private void renderLivingWater(PoseStack matrix, AscensionClasses theme) {
        long now = System.currentTimeMillis();
        double time = now / 1000.0D;

        // три световых потока
        for (int band = 0; band < 3; band++) {
            double speed = 0.35D + band * 0.17D;
            int alpha = 14 + band * 7;
            int col = (alpha << 24) | (0x00FFFFFF & (band == 2 ? theme.prismColor : theme.lightColor));
            for (int px = 0; px < sizeX; px += 2) {
                double wob = Math.sin((px * 0.045D) + time * speed * Math.PI * 2D + band * 2.1D);
                double wob2 = Math.cos((px * 0.021D) - time * speed * 2.6D);
                int cy = guiTop + 55 + band * 52 + (int) (wob * 10 + wob2 * 7);
                AbstractGui.fill(matrix, guiLeft + px, cy, guiLeft + px + 2, cy + 2 + band, col);
            }
        }

        // пузыри, детерминированно дрейфующие вверх
        for (int i = 0; i < 14; i++) {
            double seed = i * 61.7D;
            double bx = guiLeft + 8 + (seed * 37 % (sizeX - 16));
            double period = 7.0D + (i % 4);
            double yy = (seed * 13 + time * 12 / period) % sizeY;
            int by = guiTop + sizeY - (int) yy - 6;
            int alpha = 60 + (i % 3) * 30;
            int col = (alpha << 24) | (0x00FFFFFF & theme.lightColor);
            AbstractGui.fill(matrix, (int) bx, by, (int) bx + 2, by + 2, col);
        }
    }

    private void renderLocked(PoseStack matrix, int mouseX, int mouseY) {
        // серая вуаль поверх темы — «ещё не время»
        AbstractGui.fill(matrix, guiLeft, guiTop, guiLeft + sizeX, guiTop + sizeY, 0xB0101010);

        // силуэт пути: серые узлы-«?» для интриги
        AscensionClasses ghost = AscensionClasses.TIDE_DANCER;
        int baseY = guiTop + 128;
        for (int i = 0; i < ghost.skillIds.size(); i++) {
            int nx = guiLeft + 22 + i * 39;
            int ny = baseY + (int) Math.round(Math.sin(i * 1.25D) * 30.0D);
            AbstractGui.fill(matrix, nx - 2, ny - 2, nx + NODE + 2, ny + NODE + 2, 0xFF2A2A2A);
            GuiUtils.renderScaledText(matrix, nx + NODE / 2 + 1, ny + NODE / 2 + 1, 1.5D, "?", ChatFormatting.DARK_GRAY);
            if (i > 0) {
                Node prev = new Node();
                prev.x = guiLeft + 22 + (i - 1) * 39 + NODE / 2;
                prev.y = baseY + (int) Math.round(Math.sin((i - 1) * 1.25D) * 30.0D) + NODE / 2;
                drawConnector(matrix, prev.x, prev.y, nx + NODE / 2, ny + NODE / 2, 0xFF3A3A3A, 0);
            }
        }

        // замок
        AbstractGui.bindTexture(LOCK_TEX);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        int lx = guiLeft + sizeX / 2 - 32;
        int ly = guiTop + 52;
        blit(matrix, lx, ly, 64, 64, 0, 0, 64, 64, 64, 64);

        GuiUtils.renderScaledText(matrix, guiLeft + sizeX / 2, guiTop + 150, 1.25D,
            "У вас ещё нет 50-го уровня", ChatFormatting.RED);

        int lvl = Load.Unit(mc.player)
            .getLevel();
        GuiUtils.renderScaledText(matrix, guiLeft + sizeX / 2, guiTop + 182, 0.95D,
            "Уровень: " + lvl + " / " + AscensionClasses.UNLOCK_LEVEL, ChatFormatting.DARK_RED);
        GuiUtils.renderScaledText(matrix, guiLeft + sizeX / 2, guiTop + 198, 0.85D,
            "Вознесённые классы открываются на " + AscensionClasses.UNLOCK_LEVEL + " уровне", ChatFormatting.GRAY);
    }

    private void renderClassCards(PoseStack matrix, int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        int cardX = guiLeft + 33;
        int cardY = guiTop + 52;
        for (AscensionClasses c : AscensionClasses.listAll()) {
            int w = 184;
            int h = 140;

            // рамка-свечение карточки
            float pulse = 0.6F + 0.4F * (float) Math.sin(now / 380.0D);
            int frame = ((int) (120 + 90 * pulse) << 24) | (0x00FFFFFF & c.accentColor);
            AbstractGui.fill(matrix, cardX - 2, cardY - 2, cardX + w + 2, cardY + h + 2, frame);
            AbstractGui.fill(matrix, cardX - 1, cardY - 1, cardX + w + 1, cardY + h + 1, 0xE6082136);

            // эмблема с пульсирующим ореолом
            AbstractGui.bindTexture(c.getEmblemLoc());
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            blit(matrix, cardX + w / 2 - 32, cardY + 6, 64, 64, 0, 0, 64, 64, 64, 64);

            GuiUtils.renderScaledText(matrix, cardX + w / 2, cardY + 82, 1.1D, c.locName, ChatFormatting.AQUA);
            GuiUtils.renderScaledText(matrix, cardX + w / 2, cardY + 96, 0.7D, "Путь из " + c.skillIds.size() + " умений", ChatFormatting.GRAY);

            // лор в две строки, центрированный
            List<FormattedCharSequence> loreLines = mc.font.split(FormattedText.of(c.lore), w - 14);
            int ly = cardY + 104;
            for (FormattedCharSequence l : loreLines) {
                AbstractGui.drawString(matrix, mc.font, l, cardX + w / 2 - mc.font.width(l) / 2, ly, 0xFF9BB8C9);
                ly += 9;
                if (ly > cardY + 112) {
                    break;
                }
            }
            cardX += 200;
        }
    }

    private void renderPath(PoseStack matrix, int mouseX, int mouseY, AscensionClasses cls) {
        long now = System.currentTimeMillis();
        AscensionData data = AscensionLogic.data(mc.player);
        int actable = AscensionLogic.firstActableNode(data, cls);

        double intro = introProgress();
        int lift = (int) ((1.0D - intro) * 24);

        // соединительные струи
        for (int i = 1; i < nodes.size(); i++) {
            Node a = nodes.get(i - 1);
            Node b = nodes.get(i);
            boolean flowed = data.rankOf(a.spellId) > 0;
            int col = flowed ? (0xC8 << 24) | (0x00FFFFFF & cls.accentColor) : 0x7A35506B;
            drawConnector(matrix, a.x + NODE / 2, a.y + lift + NODE / 2, b.x + NODE / 2, b.y + lift + NODE / 2, col, now);
        }

        // узлы
        for (Node n : nodes) {
            int rank = data.rankOf(n.spellId);
            boolean revealed = AscensionLogic.isNodeRevealed(data, cls, n.index);
            boolean maxed = rank >= AscensionClasses.MAX_RANK;
            int nx = n.x;
            int ny = n.y + lift;

            // рамка-состояние
            int border;
            if (!revealed) {
                border = 0xFF3F3F3F;
            } else if (rank > 0 && maxed) {
                border = 0xFFFFD24A; // предел пути — золото
            } else if (n.index == actable) {
                float pulse = 0.5F + 0.5F * (float) Math.sin(now / 300.0D);
                border = ((int) (150 + 105 * pulse) << 24) | (0x00FFFFFF & cls.lightColor);
            } else if (rank > 0) {
                border = (0xFF << 24) | (0x00FFFFFF & cls.accentColor);
            } else {
                border = 0xB0557F96;
            }

            AbstractGui.fill(matrix, nx - 2, ny - 2, nx + NODE + 2, ny + NODE + 2, border);
            AbstractGui.fill(matrix, nx - 1, ny - 1, nx + NODE + 1, ny + NODE + 1, 0xFF0A1B29);

            if (revealed) {
                Spell spell = ExileDB.Spells()
                    .get(n.spellId);
                if (spell != null) {
                    AbstractGui.bindTexture(spell.getIconLoc());
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, rank > 0 || n.index == actable ? 1.0F : 0.75F);
                    blit(matrix, nx, ny, NODE, NODE, 0, 0, 32, 32, 32, 32);
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                }

                // пипсы рангов
                for (int p = 0; p < AscensionClasses.MAX_RANK; p++) {
                    int pc = p < rank ? (0xFF << 24) | (0x00FFFFFF & cls.lightColor) : 0xFF26313B;
                    AbstractGui.fill(matrix, nx + 2 + p * 8, ny + NODE + 3, nx + 7 + p * 8, ny + NODE + 6, pc);
                }
            } else {
                // загадка глубин
                GuiUtils.renderScaledText(matrix, nx + NODE / 2 + 1, ny + NODE / 2 + 1, 1.45D, "?",
                    n.index <= actable + 1 ? ChatFormatting.DARK_AQUA : ChatFormatting.DARK_GRAY);
            }
        }

        // подписи у подножия
        GuiUtils.renderScaledText(matrix, guiLeft + sizeX / 2, guiTop + 196, 0.8D,
            "ЛКМ — вложить очко Вознесения • ПКМ — назначить на панель", ChatFormatting.AQUA);
        GuiUtils.renderScaledText(matrix, guiLeft + sizeX / 2, guiTop + 208, 0.75D,
            "Цепочка линейна: вложи в прошлые умения, чтобы открыть следующее", ChatFormatting.GRAY);
    }

    private void renderNodeTooltips(PoseStack matrix, int mouseX, int mouseY, AscensionClasses cls) {
        AscensionData data = AscensionLogic.data(mc.player);
        for (Node n : nodes) {
            if (!n.isInside(mouseX, mouseY)) {
                continue;
            }
            List<Component> tip = new ArrayList<>();
            boolean revealed = AscensionLogic.isNodeRevealed(data, cls, n.index);
            int rank = data.rankOf(n.spellId);

            if (!revealed) {
                tip.add(Component.literal("? ? ?")
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
                tip.add(Component.literal("Глубины скрывают это умение.")
                    .withStyle(ChatFormatting.GRAY));
                tip.add(Component.literal("Вложите очки в предыдущие умения пути.")
                    .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                Spell spell = ExileDB.Spells()
                    .get(n.spellId);
                if (spell != null) {
                    tip.add(spell.locName()
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                    tip.addAll(mc.font.split(FormattedText.of(
                        Component.translatable("spell.desc." + n.spellId)
                            .getString()), 210)
                        .stream()
                        .map(s -> (Component) Component.literal(formattedSeqToString(s))
                            .withStyle(ChatFormatting.GRAY))
                        .toList());
                }
                tip.add(Component.literal("Ранг: " + rank + " / " + AscensionClasses.MAX_RANK)
                    .withStyle(ChatFormatting.YELLOW));
                if (rank < AscensionClasses.MAX_RANK) {
                    tip.add(Component.literal(AscensionLogic.freePoints(mc.player) > 0
                            ? "ЛКМ — вложить очко (осталось: " + AscensionLogic.freePoints(mc.player) + ")"
                            : "Нет свободных очков Вознесения")
                        .withStyle(AscensionLogic.freePoints(mc.player) > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
                }
                if (rank > 0) {
                    tip.add(Component.literal("ПКМ — назначить на панель заклинаний")
                        .withStyle(ChatFormatting.DARK_AQUA));
                }
            }
            GuiUtils.renderTooltip(matrix, tip, mouseX, mouseY);
            return;
        }
    }

    /**
     * Финальный кадр перехода: вспышка колец из центра + всплытие содержимого.
     */
    private void renderIntro(PoseStack matrix) {
        double p = introProgress();
        if (p >= 1.0D) {
            return;
        }
        float fade = (float) (1.0D - p);
        double rr = p * Math.hypot(sizeX, sizeY);
        AscensionClasses theme = currentClass() != null ? currentClass() : AscensionClasses.TIDE_DANCER;
        AscensionTransitionScreen.drawRing(matrix, guiLeft + sizeX / 2.0, guiTop + sizeY / 2.0, rr, 40,
            ((int) (fade * 200) << 24) | (0x00FFFFFF & theme.accentColor));
        AscensionTransitionScreen.drawRing(matrix, guiLeft + sizeX / 2.0, guiTop + sizeY / 2.0, rr * 0.66D, 34,
            ((int) (fade * 170) << 24) | (0x00FFFFFF & theme.prismColor));
        int a = (int) (fade * 120);
        AbstractGui.fill(matrix, guiLeft, guiTop, guiLeft + sizeX, guiTop + sizeY, (a << 24) | (0x00FFFFFF & theme.deepColor));
    }

    private double introProgress() {
        return Math.min(1.0D, (System.currentTimeMillis() - openedAt) / 700.0D);
    }

    /**
     * Струя между узлами: пунктирная линия с бегущей каплей света.
     */
    private void drawConnector(PoseStack matrix, int x1, int y1, int x2, int y2, int color, long now) {
        int steps = Math.max(2, (int) (Math.hypot(x2 - x1, y2 - y1) / 3.0D));
        for (int s = 0; s <= steps; s++) {
            double t = s / (double) steps;
            int cx = (int) (x1 + (x2 - x1) * t);
            int cy = (int) (y1 + (y2 - y1) * t);
            AbstractGui.fill(matrix, cx, cy, cx + 2, cy + 2, color);
        }
        if (now > 0) {
            double flow = ((now / 900.0D) % 1.0D);
            int cx = (int) (x1 + (x2 - x1) * flow);
            int cy = (int) (y1 + (y2 - y1) * flow);
            AbstractGui.fill(matrix, cx - 1, cy - 1, cx + 3, cy + 3, 0xFF7DFFE9);
        }
    }

    private static String formattedSeqToString(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((i, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }
}
