package com.robertx22.age_of_exile.player_skills.ingredient.data;
import net.minecraft.network.chat.MutableComponent;

import com.robertx22.age_of_exile.database.data.ingredient.SlashIngredient;
import com.robertx22.age_of_exile.database.data.player_skills.PlayerSkill;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TooltipUtils;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;

@Storable
public class IngredientData {

    @Store
    public String id = "";
    @Store
    public int tier = 1;

    public static IngredientData of(SlashIngredient ing, int tier) {
        IngredientData data = new IngredientData();
        data.id = ing.GUID();
        data.tier = tier;
        return data;
    }

    static String STAR = "\u272B";

    public MutableComponent getStars() {
        GearRarity rar = ExileDB.GearRarities()
            .get(getIngredient().rar);

        ChatFormatting BLANK = ChatFormatting.GRAY;
        ChatFormatting format = rar.textFormatting();

        String txt = "";
        if (rar.item_tier == 0) {
            txt = " [" + STAR + STAR + STAR + "]";
        } else if (rar.item_tier == 1) {
            txt = " " + format + "[" + STAR + BLANK + STAR + STAR + format + "]";
        } else if (rar.item_tier == 2) {
            txt = " " + format + "[" + STAR + STAR + BLANK + STAR + format + "]";
        } else if (rar.item_tier == 3) {
            txt = " " + format + "[" + STAR + STAR + STAR + "]";
        }

        return Component.literal(txt);
    }

    public void makeTooltip(List<Component> tip) {
        GearRarity rar = ExileDB.GearRarities()
            .get(getIngredient().rar);

        SlashIngredient ingredient = getIngredient();
        tip.clear();

        int lvl = LevelUtils.tierToLevel(tier);

        tip.add(ingredient.locName()
            .withStyle(rar.textFormatting())
            .append(getStars()));

        //  tip.add(Component.literal(""));

        tip.add(Words.Ingredient.locName()
            .withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal(""));

        ingredient.stats.forEach(x -> {
            tip.addAll(x.getEstimationTooltip(lvl));
            if (x.GetStat().is_long) {
                tip.add(Component.literal(""));
            }
        });

        if (ingredient.isOneOfAKind()) {
            tip.add(Component.literal(""));
            tip.add(Component.literal("Только один из: ").append(ingredient.getOneOfAKindId())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        tip.add(Component.literal(""));

        tip.add(TooltipUtils.gearTier(tier));
        tip.add(TooltipUtils.gearRarity(ExileDB.GearRarities()
            .get(ingredient.rar)));

        tip.add(Component.literal(""));

        tip.add(Component.literal("Используется в:"));

        ingredient.allowed_in.forEach(x -> {
            PlayerSkill skill = ExileDB.PlayerSkills()
                .get(x);
            if (skill != null) {
                tip.add(Component.literal(TooltipUtils.CHECKMARK + " ").withStyle(ChatFormatting.GREEN)
                    .append(skill.type_enum.word.locName())
                    .withStyle(ChatFormatting.GRAY));
            }
        });

        TooltipUtils.removeDoubleBlankLines(tip);

    }

    public SlashIngredient getIngredient() {
        return ExileDB.Ingredients()
            .get(id);
    }
}
