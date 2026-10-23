package com.robertx22.age_of_exile.player_skills.ingredient.data;

import com.robertx22.age_of_exile.saveclasses.ExactStatData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipInfo;
import com.robertx22.age_of_exile.saveclasses.item_classes.tooltips.MergedStats;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TooltipUtils;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

@Storable
public class CraftedConsumableData {

    @Store
    public int seconds = 60;
    @Store
    public String crafter = "";

    @Store
    public String prof = "";
    @Store
    public int tier = 1;
    @Store
    public int uses = 1;
    @Store
    public int maxuses = 1;
    @Store
    public List<ExactStatData> stats = new ArrayList<>();

    public boolean canEat(Player player) {

        int lvl = LevelUtils.tierToLevel(tier);

        return Load.Unit(player)
            .getLevel() >= lvl;
    }

    public void makeTooltip(ItemStack stack, List<Component> tip) {

        tip.clear();

        MutableComponent name = Component.literal("").append(stack.getHoverName())
            .withStyle(ChatFormatting.DARK_AQUA);

        if (maxuses > 1) {
            name.append(Component.literal(" [" + uses + "/" + maxuses + "]").withStyle(ChatFormatting.AQUA));
        }

        tip.add(name);
        if (!crafter.isEmpty()) {
            tip.add(Component.literal(ChatFormatting.DARK_GRAY + "Создано: " + ChatFormatting.LIGHT_PURPLE + crafter));
        }

        tip.add(Component.literal(""));

        MergedStats merged = new MergedStats(stats, new TooltipInfo());

        tip.add(Words.Stats.locName()
            .withStyle(ChatFormatting.AQUA)
            .append(":"));
        tip.addAll(merged.GetTooltipString(new TooltipInfo(), null));

        tip.add(Component.literal(""));

        tip.add(Component.literal(ChatFormatting.DARK_GRAY + "Длительность: " + ChatFormatting.BLUE + seconds + " с"));

        tip.add(Component.literal(""));

        tip.add(TooltipUtils.gearLevel(LevelUtils.tierToLevel(tier)));
        tip.add(TooltipUtils.gearTier(tier));

    }
}
