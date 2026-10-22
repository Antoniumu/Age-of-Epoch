package com.robertx22.age_of_exile.questbook;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.gui.screens.character_screen.MainHubScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.stream.Collectors;

public class QuestBookScreen extends BaseScreen implements INamedScreen {

    private QuestBookCatalog.Chapter selectedChapter = QuestBookCatalog.Chapter.AWAKENING;
    private int scrollOffset = 0;

    public QuestBookScreen() {
        super(380, 236);
    }

    @Override
    public Identifier iconLocation() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/main_hub/icons/journal.png");
    }

    @Override
    public Words screenName() {
        return Words.QuestBook;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        Packets.sendToServer(new QuestBookPacket("sync"));

        int navX = this.guiLeft + 8;
        int navY = this.guiTop + 28;
        int idx = 0;
        for (QuestBookCatalog.Chapter ch : QuestBookCatalog.Chapter.values()) {
            final QuestBookCatalog.Chapter target = ch;
            String label = (ch == selectedChapter ? "§6▶ " : "§7") + ch.title;
            publicAddButton(Button.builder(Component.literal(label), b -> {
                this.selectedChapter = target;
                this.scrollOffset = 0;
                this.init();
            }).bounds(navX, navY + idx * 24, 118, 20).tooltip(Tooltip.create(Component.literal(ch.subtitle))).build());
            idx++;
        }

        publicAddButton(Button.builder(Component.literal("§e← В меню персонажа"), b ->
            Minecraft.getInstance().setScreen(new MainHubScreen())
        ).bounds(navX, this.guiTop + this.sizeY - 28, 118, 20).build());

        List<QuestBookCatalog.QuestEntry> entries = getVisibleEntries();
        int bodyX = this.guiLeft + 134;
        int bodyY = this.guiTop + 44;
        int maxRows = 6;
        for (int i = 0; i < Math.min(maxRows, Math.max(0, entries.size() - scrollOffset)); i++) {
            QuestBookCatalog.QuestEntry q = entries.get(scrollOffset + i);
            int cur = QuestBookService.getProgress(mc.player, q);
            int tgt = QuestBookService.getTarget(q);
            boolean done = cur >= tgt;
            String line = (done ? "§a✔ " : "§f• ") + q.title() + " §7[" + Math.min(cur, tgt) + "/" + tgt + "]";
            publicAddButton(Button.builder(Component.literal(line), b -> {})
                .bounds(bodyX, bodyY + i * 28, 238, 24)
                .tooltip(Tooltip.create(Component.literal("§e" + q.title() + "\n§7" + q.description() + "\n§bПрогресс: " + cur + " / " + tgt)))
                .build());
        }
    }

    private List<QuestBookCatalog.QuestEntry> getVisibleEntries() {
        return QuestBookCatalog.ALL.stream()
            .filter(q -> q.chapter() == selectedChapter)
            .collect(Collectors.toList());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, getVisibleEntries().size() - 6);
        if (scrollY < 0) scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        else if (scrollY > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        this.init();
        return true;
    }

    @Override
    public void render(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTicks) {
        gui.fill(this.guiLeft, this.guiTop, this.guiLeft + this.sizeX, this.guiTop + this.sizeY, 0xE614141C);
        gui.fill(this.guiLeft, this.guiTop, this.guiLeft + this.sizeX, this.guiTop + 2, 0xFFD4AF37);
        gui.fill(this.guiLeft + 129, this.guiTop + 24, this.guiLeft + 130, this.guiTop + this.sizeY - 8, 0x60D4AF37);

        QuestBookData book = QuestBookService.get(mc.player);
        int doneTotal = book.completedQuests.size();
        gui.text(mc.font, "§6Книга Квестов Age of Epoch §7(" + doneTotal + "/" + QuestBookCatalog.ALL.size() + ")",
            this.guiLeft + 10, this.guiTop + 9, 0xFFFFFFFF, true);
        gui.text(mc.font, "§e" + selectedChapter.title + " §8— §7" + selectedChapter.subtitle,
            this.guiLeft + 134, this.guiTop + 28, 0xFFCCCCCC, false);
        gui.text(mc.font, "§8Руны: §f" + book.runes.size() + "/" + ExileDB.Runes().getList().size()
            + "  §8Рунворды: §f" + book.runewords.size() + "/" + ExileDB.RuneWords().getList().size()
            + "  §8Уники: §f" + book.uniqueGears.size() + "/" + ExileDB.UniqueGears().getList().size(),
            this.guiLeft + 134, this.guiTop + this.sizeY - 18, 0xFFAAAAAA, false);

        super.extractRenderState(gui, mouseX, mouseY, partialTicks);
    }
}
