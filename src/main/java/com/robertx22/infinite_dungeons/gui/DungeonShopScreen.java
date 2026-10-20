package com.robertx22.infinite_dungeons.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import com.robertx22.infinite_dungeons.components.PlayerIDCap;
import com.robertx22.infinite_dungeons.database.db_types.layout.DungeonLayout;
import com.robertx22.infinite_dungeons.database.db_types.layout.ShopEntry;
import com.robertx22.infinite_dungeons.gui.buttons.shop.ShopArrowButton;
import com.robertx22.infinite_dungeons.gui.buttons.shop.ShopCostButton;
import com.robertx22.infinite_dungeons.gui.buttons.shop.ShopDifficultyButton;
import com.robertx22.infinite_dungeons.gui.buttons.shop.ShopRewardButton;
import com.robertx22.infinite_dungeons.main.MainID;
import net.minecraft.resources.Identifier;

import java.util.*;

public class DungeonShopScreen extends BaseScreen {

    static int X = 192;
    static int Y = 166;

    List<ShopEntry> all = new ArrayList<>();

    public DungeonShopScreen() {
        super(X, Y);

        DungeonLayout layout = getDungeon().data.getDungeonLayout();
        all = layout.getShopList().shop_entries;

    }

    static Identifier BACKGROUND = MainID.id("textures/gui/shop.png");

    public ControlBlockEntity getDungeon() {
        Optional<ControlBlockEntity> opt = PlayerIDCap.get(mc.player)
            .getControlBlock();
        if (opt.isPresent()) {
            return opt.get();
        }
        this.onClose();
        return null;

    }

    public int index = 0;

    static int MAX_TO_SHOW = 5;

    public HashMap<Integer, ShopEntry> getEntriesToShow() {

        HashMap<Integer, ShopEntry> map = new HashMap<>();

        for (int i = index; i < index + MAX_TO_SHOW; i++) {
            if (all.size() > i) {
                map.put(i, all.get(i));
            }
        }

        return map;

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (scroll < 0) {
            index++;
            if (index > all.size() - 1) {
                index = all.size() - 1;
            }
            init();
            return true;
        }
        if (scroll > 0) {
            index--;
            if (index < 1) {
                index = 0;
            }
            init();
            return true;
        }
        return true;
    }

    @Override
    public void init() {
        super.init();

        this.buttons.clear();
        this.clearWidgets();

        int x = guiLeft + 40;
        int y = guiTop + 22;

        for (Map.Entry<Integer, ShopEntry> e : getEntriesToShow().entrySet()) {

            ShopEntry entry = e.getValue();
            int index = e.getKey();

            int xpos = x;

            publicAddButton(new ShopDifficultyButton(entry.getDifficulty(), xpos, y + 2));
            xpos += 24;
            publicAddButton(new ShopCostButton(entry, xpos, y));
            xpos += 24;
            publicAddButton(new ShopArrowButton(xpos, y));
            xpos += 24;
            publicAddButton(new ShopRewardButton(index, entry, xpos, y));
            y += ShopRewardButton.SIZE_Y + 3;

        }

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(PoseStack matrix, int x, int y, float ticks) {

        renderBackground(matrix, BACKGROUND);

        super.render(matrix, x, y, ticks);

        buttons.forEach(b -> b.renderToolTip(matrix, x, y));

    }

}
