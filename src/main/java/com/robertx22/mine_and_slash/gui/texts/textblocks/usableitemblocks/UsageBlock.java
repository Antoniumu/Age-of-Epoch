package com.robertx22.mine_and_slash.gui.texts.textblocks.usableitemblocks;

import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import com.robertx22.mine_and_slash.gui.texts.textblocks.AbstractTextBlock;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.function.Supplier;

// This block use to handle the text line, like the currency usage etc. these lines are not stat but still important.
public class UsageBlock extends AbstractTextBlock {

    @NotNull
    public List<? extends Component> components;

    public UsageBlock(@NotNull List<? extends Component> components) {
        this.components = components;
    }

    public UsageBlock(@NotNull Supplier<List<? extends Component>> components) {
        this.components = components.get();
    }


    @Override
    public List<? extends Component> getAvailableComponents() {
        return components;
    }

    @Override
    public ExileTooltips.BlockCategories getCategory() {
        return ExileTooltips.BlockCategories.USAGE;
    }
}
