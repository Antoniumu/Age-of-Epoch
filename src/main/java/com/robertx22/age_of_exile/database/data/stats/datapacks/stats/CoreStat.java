package com.robertx22.age_of_exile.database.data.stats.datapacks.stats;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.age_of_exile.database.OptScaleExactStat;
import com.robertx22.age_of_exile.database.data.stats.StatScaling;
import com.robertx22.age_of_exile.database.data.stats.datapacks.base.BaseDatapackStat;
import com.robertx22.age_of_exile.database.data.stats.datapacks.base.CoreStatData;
import com.robertx22.age_of_exile.database.data.stats.name_regex.StatNameRegex;
import com.robertx22.age_of_exile.database.data.stats.types.core_stats.base.ICoreStat;
import com.robertx22.age_of_exile.saveclasses.ExactStatData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipInfo;
import com.robertx22.age_of_exile.saveclasses.unit.InCalcStatData;
import com.robertx22.age_of_exile.saveclasses.unit.StatData;
import com.robertx22.age_of_exile.uncommon.wrappers.SText;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CoreStat extends BaseDatapackStat implements ICoreStat {

    public static String SER_ID = "core_stat";

    public CoreStatData data = new CoreStatData();

    transient String locname;

    public CoreStat(String id, String locname, CoreStatData data) {
        super(SER_ID);

        this.id = id;

        this.data = data;

        this.is_perc = true;
        this.min = 0;
        this.group = StatGroup.CORE;
        this.scaling = StatScaling.NORMAL;
        this.is_long = false;

        this.locname = locname;
    }

    @Override
    public final String locDescForLangFile() {
        // because i tend to change things and then the damn tooltip becomes outdated.
        String str = "Определяет ваш общий показатель: ";
        for (OptScaleExactStat x : this.statsThatBenefit()) {
            str += x.getStat()
                .translate() + ", ";
        }
        str = str.substring(0, str.length() - 2);

        return str;
    }

    public float getValue(StatData data) {
        return data.getValue();
    }

    public List<Component> getCoreStatTooltip(EntityData unitdata, StatData data) {

        TooltipInfo info = new TooltipInfo(unitdata, null);

        int val = (int) getValue(data);
        int lvl = unitdata != null ? unitdata.getLevel() : 1;

        List<Component> list = new ArrayList<>();

        list.add(
            Component.literal("За каждое очко: ").withStyle(ChatFormatting.GREEN));
        getMods(1, lvl).forEach(x -> list.addAll(x.GetTooltipString(info)));

        list.add(net.minecraft.network.chat.Component.literal(""));

        list.add(
            Component.literal("Всего: ").withStyle(ChatFormatting.GREEN));
        getMods(val, lvl).forEach(x -> list.addAll(x.GetTooltipString(info)));

        return list;

    }

    public List<ExactStatData> getMods(int amount) {
        return getMods(amount, 1);
    }

    public List<ExactStatData> getMods(int amount, int lvl) {

        // Вложения очков раньше умножались на полный уровень (x10.8 к 50-му), из-за чего
        // 100 очков VIT давали x17 HP, а 100 STR — только +150% урона.
        // Множитель уровня сжимается до CORE_STAT_LEVEL_FACTOR (по умолчанию x3.45).
        float factor = 1F;
        try {
            factor = Math.max(0.01F, Math.min(1F, GameBalanceConfig.get().CORE_STAT_LEVEL_FACTOR));
        } catch (Exception ignored) {
        }
        int effectiveLevel = 1 + Math.round(Math.max(0, lvl - 1) * factor);

        return data.stats.stream()
            .map(x -> {
                ExactStatData exact = ExactStatData.of(amount * x.v1, x.getStat()
                    .getStat(), x.getStat()
                    .getModType(), Math.max(1, effectiveLevel));
                return exact;
            })
            .collect(Collectors.toList());

    }

    @Override
    public final List<OptScaleExactStat> statsThatBenefit() {
        return data.stats.stream()
            .map(e -> e.getStat())
            .collect(Collectors.toList());
    }

    @Override
    public void addToOtherStats(EntityData unitdata, InCalcStatData data) {
        int lvl = unitdata != null ? unitdata.getLevel() : 1;
        for (ExactStatData x : getMods((int) data.getValue(), lvl)) {
            x.applyStats(unitdata);
        }
    }

    @Override
    public StatNameRegex getStatNameRegex() {
        return StatNameRegex.BASIC;
    }

    @Override
    public boolean IsPercent() {
        return false;
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }

}