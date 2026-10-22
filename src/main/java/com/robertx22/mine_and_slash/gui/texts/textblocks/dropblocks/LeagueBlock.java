package com.robertx22.mine_and_slash.gui.texts.textblocks.dropblocks;

import com.mojang.blaze3d.platform.InputConstants;

import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.util.UNICODE;
import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import com.robertx22.mine_and_slash.gui.texts.textblocks.AbstractTextBlock;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.forge_compat.client.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public class LeagueBlock extends AbstractTextBlock {

    League league;

    public LeagueBlock(League l) {
        this.league = l;
    }

    @Override
    public List<? extends Component> getAvailableComponents() {
        var list = new ArrayList<MutableComponent>();

        list.add(Component.literal(UNICODE.SKULL + " ").withStyle(league.getTextColor()).append(league.getTranslation(TranslationType.NAME).getTranslatedName().withStyle(league.getTextColor())));

        if (InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)) {
            list.add(Chats.ITEM_AQUIRED_THROUGH_LEAGUE.locName().withStyle(league.getTextColor()));
        }
        return list;
    }

    @Override
    public ExileTooltips.BlockCategories getCategory() {
        return ExileTooltips.BlockCategories.ADDITIONAL;
    }
}
