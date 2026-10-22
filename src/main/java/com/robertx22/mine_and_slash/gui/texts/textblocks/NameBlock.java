package com.robertx22.mine_and_slash.gui.texts.textblocks;

import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;

public class NameBlock extends AbstractTextBlock {

    @NotNull
    private final List<? extends Component> name;

    public NameBlock(@NotNull List<? extends Component> name) {
        this.name = name;
    }

    public NameBlock(Component name) {
        this.name = Collections.singletonList(name);
    }

    @Override
    public List<? extends Component> getAvailableComponents() {
        //not colorize, leave it to ExileTooltips
        return name;
    }


    @Override
    public ExileTooltips.BlockCategories getCategory() {
        return ExileTooltips.BlockCategories.NAME;
    }
}
