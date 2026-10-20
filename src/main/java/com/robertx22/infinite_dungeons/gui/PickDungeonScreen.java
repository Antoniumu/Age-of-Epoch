package com.robertx22.infinite_dungeons.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.database.db_types.DungeonDifficulty;
import com.robertx22.infinite_dungeons.database.db_types.group.CoinDrop;
import com.robertx22.infinite_dungeons.database.db_types.group.DungeonGroup;
import com.robertx22.infinite_dungeons.database.db_types.layout.DungeonLayout;
import com.robertx22.infinite_dungeons.database.db_types.layout.ShopEntry;
import com.robertx22.infinite_dungeons.gui.buttons.*;
import com.robertx22.infinite_dungeons.item.DungeonKeyItem;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.library_of_exile.gui.ItemSlotButton;
import com.robertx22.library_of_exile.registry.FilterListWrap;
import com.robertx22.library_of_exile.utils.CLOC;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PickDungeonScreen extends BaseScreen implements ILeftRight {

    static int X = 206;
    static int Y = 178;

    public PickDungeonScreen() {
        super(X, Y);

        DungeonGroup group = DungeonKeyItem.getDungeonGroup(this.mc.player.getMainHandItem()
            .getItem());

        if (group == null) {
            this.onClose();
        } else {

            this.layouts = DungeonsDB.DungeonLayouts()
                .getFiltered(x -> x.dungeon_group.equals(group.GUID()));

            this.currentLayout = layouts.get(0);
        }
    }

    static Identifier BACKGROUND = MainID.id("textures/gui/dungeon_picker.png");

    public List<DungeonLayout> layouts = new ArrayList<>();
    public DungeonLayout currentLayout;

    public DungeonDifficulty difficulty = DungeonsDB.Difficulties()
        .getList()
        .stream()
        .min(Comparator.comparingInt(x -> x.tier))
        .get();

    @Override
    public void init() {
        super.init();

        this.children.clear();
        this.buttons.clear();

        int x = guiLeft - PickDungeonButton.SIZE_X;
        int y = guiTop + 30;

        publicAddButton(new DifficultyButton(this, guiLeft + 153, guiTop + 43));
        publicAddButton(new StartDungeonButton(this, guiLeft + 15, guiTop + 152));

        publicAddButton(new LeftRightButton(this, guiLeft + 132, guiTop + 40, true));
        publicAddButton(new LeftRightButton(this, guiLeft + 173, guiTop + 40, false));

        for (DungeonLayout layout : layouts) {
            publicAddButton(new PickDungeonButton(this, layout, x, y));
            y += PickDungeonButton.SIZE_Y;
        }

        int xpos = guiLeft + 136;
        int ypos = guiTop + 119;

        int shopIndex = 0;

        for (ShopEntry entry : this.currentLayout.getShopList().shop_entries.stream()
            .filter(d -> this.difficulty.tier >= d.getDifficulty().tier)
            .collect(Collectors.toList())) {

            publicAddButton(new ItemSlotButton(entry.reward.type.getStack(entry.reward), xpos, ypos));

            if (shopIndex == 2 || shopIndex == 5) {
                xpos = guiLeft + 136;
                ypos += 18;
            } else {
                xpos += 18;
            }

            shopIndex++;

            if (shopIndex == 8) {
                break; // max 9 items to show
            }

        }
        xpos = guiLeft + 136;
        ypos = guiTop + 83;

        for (CoinDrop coin : currentLayout.getGroup().coin_drops) {
            publicAddButton(new ItemSlotButton(new ItemStack(coin.getCoinItem()), xpos, ypos));
            xpos += 18;
        }

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(PoseStack matrix, int x, int y, float ticks) {

        renderBackground(matrix, BACKGROUND);

        if (currentLayout != null) {

            String title = CLOC.translate(currentLayout.getTranslatable());
            GuiUtils.renderScaledText(matrix, this.guiLeft + sizeX / 2, guiTop + 12, 1, title, ChatFormatting.YELLOW);

            String difftext = "Difficulty:";
            GuiUtils.renderScaledText(matrix, this.guiLeft + 162, guiTop + 32, 1, difftext, ChatFormatting.RED);

            String cointext = "Coin Drops:";
            GuiUtils.renderScaledText(matrix, this.guiLeft + 162, guiTop + 74, 1, cointext, ChatFormatting.GREEN);

            String shoptext = "Shop:";
            GuiUtils.renderScaledText(matrix, this.guiLeft + 162, guiTop + 111, 1, shoptext, ChatFormatting.GREEN);

            int ypos = guiTop + 28;

            com.robertx22.forge_compat.client.AbstractGui.drawWordWrap(mc.font, currentLayout.getDungeonType()
                .getTranslatableDesc(), guiLeft + 7, ypos, 115, com.robertx22.forge_compat.common.CompatChatFormatting.getColor(ChatFormatting.WHITE));

        }

        super.render(matrix, x, y, ticks);

        buttons.forEach(b -> b.renderToolTip(matrix, x, y));

    }

    @Override
    public void goLeft() {

        FilterListWrap<DungeonDifficulty> opt = DungeonsDB.Difficulties()
            .getFilterWrapped(x -> x.tier == difficulty.tier - 1);

        if (!opt.list.isEmpty()) {
            this.difficulty = opt.list.get(0);
        }

        init();

    }

    @Override
    public void goRight() {
        FilterListWrap<DungeonDifficulty> opt = DungeonsDB.Difficulties()
            .getFilterWrapped(x -> x.tier == difficulty.tier + 1);

        if (!opt.list.isEmpty()) {
            this.difficulty = opt.list.get(0);
        }
        init();

    }
}
