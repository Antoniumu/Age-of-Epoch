package com.robertx22.age_of_exile.vanilla_mc.items.gemrunes;

import com.robertx22.age_of_exile.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.age_of_exile.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.data.runes.Rune;
import com.robertx22.age_of_exile.database.data.runewords.RuneWord;
import com.robertx22.age_of_exile.database.data.unique_items.UniqueGear;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.library_of_exile.registry.IGUID;
import com.robertx22.library_of_exile.registry.IWeighted;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
public class RuneItem extends Item implements IGUID, IAutoModel, IAutoLocName, IWeighted {

    public int weight = 1000;

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Misc;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.GOLD);
    }

    @Override
    public String locNameLangFileGUID() {
        return BuiltInRegistries.ITEM.getKey(this)
            .toString();
    }

    @Override
    public String locNameForLangFile() {
        return type.locName + " Rune";
    }

    @Override
    public void generateModel(ItemModelManager manager) {
        manager.generated(this);
    }

    public RuneType type;

    public RuneItem(RuneType type) {
        super(new Properties().stacksTo(64));
        this.type = type;

        this.weight = type.weight;
    }

    @Override
    public String GUID() {
        return "runes/" + type.id;
    }

    @Override
    public int Weight() {
        return weight;
    }

    /**
     * 15 рун. Вес — редкость внутри своего тира, tier — минимальный тир уровня,
     * с которого руна падает (тир 1 = с 10 уровня, 2 = с 20, 3 = с 30, 4 = с 40).
     *
     * YUN и VEN раньше были тиром 5, то есть падали только с 50 уровня: четыре из
     * семи рунных слов требуют YUN, и до 50-го уровня их нельзя было начать собирать.
     * Теперь они тир 4; веса ключевых рун YUN/VEN/ORU/WIR подняты до 800/800/800/500
     * при суммарном весе 13 900, чтобы компания чаще собирала редкие слова без гарантии дропа.
     */
    public enum RuneType {
        YUN(800, "yun", "Yun", 4),
        VEN(800, "ven", "Ven", 4),
        NOS(1000, "nos", "Nos", 1),
        MOS(1000, "mos", "Mos", 1),
        ITA(1000, "ita", "Ita", 1),
        CEN(1000, "cen", "Cen", 2),
        FEY(1000, "fey", "Fey", 2),
        DOS(1000, "dos", "Dos", 2),
        ANO(1000, "ano", "Ano", 2),
        TOQ(1000, "toq", "Toq", 2),
        ORU(800, "oru", "Oru", 4),
        WIR(500, "wir", "Wir", 4),
        ENO(1000, "eno", "Eno", 3),
        HAR(1000, "har", "Har", 3),
        XER(1000, "xer", "Xer", 3);

        public String id;
        public String locName;
        public int tier;
        public int weight;

        RuneType(int weight, String id, String locName, int tier) {
            this.id = id;
            this.locName = locName;
            this.tier = tier;
            this.weight = weight;
        }

    }

    public Rune getRune() {
        return ExileDB.Runes()
            .get(type.id);
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {

        try {
            tooltip.add(Component.literal("Руна, тир " + type.tier)
                .withStyle(ChatFormatting.DARK_GRAY));

            Rune rune = getRune();

            if (rune != null) {
                int lvl = rune.getReqLevelToDrop();
                if (lvl > 1) {
                    tooltip.add(Component.literal("Начинает падать с " + lvl + " уровня")
                        .withStyle(ChatFormatting.GRAY));
                }
            }

            List<RuneWord> words = ExileDB.RuneWords()
                .getFiltered(x -> x.runes.contains(type.id));

            tooltip.add(Component.literal(""));

            if (words.isEmpty()) {
                tooltip.add(Component.literal("Компонент для рунных слов")
                    .withStyle(ChatFormatting.AQUA));
            } else {
                tooltip.add(Component.literal("Рунные слова:").withStyle(ChatFormatting.GOLD));
                for (RuneWord word : words) {
                    UniqueGear uniq = ExileDB.UniqueGears()
                        .get(word.uniq_id);
                    String name = uniq == null ? word.uniq_id : uniq.locName()
                        .getString();
                    tooltip.add(Component.literal(" • " + name + " (" + word.runes.size() + " рун)")
                        .withStyle(ChatFormatting.YELLOW));
                }
            }

            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("Сама по себе статов не даёт.")
                .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("Положите набор рун в станцию рунных слов вместе с предметом.")
                .withStyle(ChatFormatting.DARK_GRAY));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
