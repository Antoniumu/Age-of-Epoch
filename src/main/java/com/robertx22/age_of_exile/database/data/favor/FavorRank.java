package com.robertx22.age_of_exile.database.data.favor;

import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class FavorRank implements JsonExileRegistry<FavorRank>, IAutoGson<FavorRank>, IAutoLocName {

    public static FavorRank SERIALIZER = new FavorRank("");

    String id = "";

    // excludes min and max
    public int min = 0;

    public int rank = 0;

    public boolean drop_unique_gears = true;
    public boolean drop_runes = true;
    public boolean drop_gems = true;
    public boolean drop_currency = true;
    public boolean drop_lvl_rewards = true;

    public float extra_item_favor_cost = 1;
    public int extra_items_per_boss = 0;
    public int extra_items_per_chest = 0;

    public boolean can_salvage_loot = true;
    public float exp_multi = 1;

    public float favor_drain_per_item = 1;// THIS CAN A PROBLEM. THIS CAN BE GAMED WITH MAGIC FIND!!!!!

    public String text_format = com.robertx22.forge_compat.common.CompatChatFormatting.getName(ChatFormatting.GREEN);

    public List<String> excludedRarities = new ArrayList<>();

    public transient String locname = "";

    public ChatFormatting textFormatting() {
        try {
            return com.robertx22.forge_compat.common.CompatChatFormatting.getByName(text_format);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ChatFormatting.GRAY;
    }

    public Identifier getTexture() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/favor/" + GUID() + ".png");
    }

    public List<MutableComponent> getTooltip() {
        List<MutableComponent> list = new ArrayList<>();

        list.add(Words.Favor.locName()
            .withStyle(textFormatting())
            .append(": ")
            .append(locName().withStyle(textFormatting())));

        list.add(Component.literal(""));

        boolean hasBad = false;

        if (!can_salvage_loot) {
            list.add(Component.literal("Добытое снаряжение нельзя разобрать!").withStyle(ChatFormatting.RED)
                .withStyle(ChatFormatting.BOLD));
            hasBad = true;
        }
        if (exp_multi < 1) {
            list.add(Component.literal("Вы получаете меньше опыта.").withStyle(ChatFormatting.RED)
                .withStyle(ChatFormatting.BOLD));
            hasBad = true;
        } else if (exp_multi > 1) {
            list.add(Component.literal("Вы получаете больше опыта.").withStyle(ChatFormatting.AQUA)
                .withStyle(ChatFormatting.BOLD));
        }

        if (!drop_unique_gears) {
            list.add(Component.literal("Уникальное снаряжение не выпадает.").withStyle(ChatFormatting.RED));
            hasBad = true;
        }
        if (!drop_currency) {
            list.add(Component.literal("Валюта не выпадает.").withStyle(ChatFormatting.RED));
            hasBad = true;
        }
        if (!drop_gems) {
            list.add(Component.literal("Самоцветы не выпадают.").withStyle(ChatFormatting.RED));
            hasBad = true;
        }
        if (!drop_runes) {
            list.add(Component.literal("Руны не выпадают.").withStyle(ChatFormatting.RED));
            hasBad = true;
        }

        if (!excludedRarities.isEmpty()) {
            hasBad = true;
            excludedRarities.forEach(x -> {
                list.add(ExileDB.GearRarities()
                    .get(x)
                    .locName()
                    .append(" редкость не может выпасть.")
                    .withStyle(ChatFormatting.GOLD));
            });
        }

        if (!hasBad) {
            list.add(Component.literal("Вы благословлены Азуной").withStyle(ChatFormatting.GREEN));
        } else {
            list.add(Component.literal(""));
            list.add(Component.literal("Азуна разочарована в вас.").withStyle(ChatFormatting.RED));
        }

        if (extra_items_per_boss > 0) {
            list.add(Component.literal(""));
            list.add(Component.literal("Боссы оставляют дополнительные предметы.").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (extra_items_per_chest > 0) {
            list.add(Component.literal("В сундуках с добычей больше предметов.").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        list.add(Component.literal(""));
        list.add(Component.literal("Восстанавливайте благосклонность, обыскивая сундуки в мире.").withStyle(ChatFormatting.BLUE));

        return list;

    }

    public FavorRank(String id) {
        this.id = id;
    }

    @Override
    public Class<FavorRank> getClassForSerialization() {
        return FavorRank.class;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.FAVOR_RANK;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Misc;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".favor." + GUID();
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }

    @Override
    public int Weight() {
        return 1000;
    }
}
