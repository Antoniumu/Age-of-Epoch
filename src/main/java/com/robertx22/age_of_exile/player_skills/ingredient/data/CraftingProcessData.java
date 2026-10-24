package com.robertx22.age_of_exile.player_skills.ingredient.data;

import com.robertx22.age_of_exile.database.data.StatModifier;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.database.data.ingredient.SlashIngredient;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.data.stats.Stat;
import com.robertx22.age_of_exile.database.data.stats.types.crafting.*;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.MMORPG;
import com.robertx22.age_of_exile.player_skills.ingredient.GridHelper;
import com.robertx22.age_of_exile.saveclasses.ExactStatData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.CraftStatData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.CraftedStatsData;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ClientOnly;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.library_of_exile.utils.RandomUtils;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Storable
public class CraftingProcessData {

    @Store
    public String prof = "";

    @Store
    public List<CraftSlotData> ingredients = new ArrayList<>();

    public List<IngredientData> getAllIngredients() {
        return ingredients.stream()
            .filter(x -> x.ing != null)
            .map(x -> x.ing)
            .collect(Collectors.toList());
    }

    public int getSuccessChance(Player player) {
        int plus = getStat(CraftingSuccessChance.getInstance());

        int total = 100 - ((this.getIngredientsCount() - 1) * 10);
        total += plus;

        PlayerSkillEnum skill = getProfession();
        int skillLvl = Load.playerRPGData(player).professions.getProfessionLevel(skill);

        int itemLvl = LevelUtils.tierToLevel(getAverageTier());

        // probably a bad way to do it, todo
        if (skillLvl > itemLvl + 9) {
            total += 10;
        } else if (skillLvl > itemLvl + 4) {
            total += 5;
        }

        return Mth.clamp(total, 0, 100);
    }

    public int getFailChance(Player player) {
        return 100 - getSuccessChance(player);
    }

    public int getAverageTier() {

        return getAllIngredients().stream()
            .mapToInt(x -> x.tier)
            .sum()
            / getAllIngredients().size();
    }

    public float getStatMulti() {

        int total = ingredients.size();

        List<SlashIngredient> distinct = getAllIngredients().stream()
            .map(x -> x.getIngredient())
            .distinct()
            .collect(Collectors.toList());

        float multi = 1F;

        for (SlashIngredient x : distinct) {
            int amount = (int) this.getAllIngredients()
                .stream()
                .filter(e -> e.getIngredient()
                    .GUID()
                    .equals(x.GUID()))
                .count();

            int morethantwo = Mth.clamp(amount - 2, 0, 100);
            multi -= morethantwo * 0.1F;
        }

        return multi;
    }

    public void makeTooltip(ItemStack stack, List<Component> tip) {
        tip.clear();

        GridHelper grid = new GridHelper(ingredients);
        grid.calcStats();

        PlayerSkillEnum skill = getProfession();

        MutableComponent name = Component.literal("").append(stack.getHoverName())
            .withStyle(ChatFormatting.DARK_AQUA);
        tip.add(name);

        tip.add(Component.literal(""));

        getAllIngredients().forEach(x -> {
            x.getIngredient().stats.forEach(s -> {
                if (s.GetStat() instanceof DoNotTransferToCraftedMarker == false) {
                    tip.addAll(s.getEstimationTooltip(LevelUtils.tierToLevel(x.tier)));
                }
            });
        });

        if (skill.isGearCraftingProf()) {

            GearRarity min = getMinRarity();
            GearRarity max = getMaxRarity();

            tip.add(Component.literal(""));
            tip.add(Component.literal("Мин. редкость: ").append(min.locName()
                .withStyle(min.textFormatting())));
            tip.add(Component.literal("Макс. редкость: ").append(max.locName()
                .withStyle(max.textFormatting())));

        }

        tip.add(Component.literal(""));
        tip.add(Component.literal("Шанс успеха: " + getSuccessChance(ClientOnly.getPlayer()) + "%").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tip.add(Component.literal(""));
        tip.add(Component.literal("Множитель характеристик: " + (int) (getStatMulti() * 100) + "%").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

        if (MMORPG.RUN_DEV_TOOLS) {
            ingredients.forEach(x -> {
                if (x.ing != null && x.multi != 1) {
                    tip.add(Component.literal(x.ing.id + " даёт " + x.multi + "x характеристик"));
                }
            });
        }

    }

    private List<ExactStatData> getResultingStats() {
        GridHelper grid = new GridHelper(ingredients);
        grid.calcStats();

        PlayerSkillEnum skill = getProfession();

        List<ExactStatData> stats = new ArrayList<>();

        for (CraftSlotData data : ingredients) {
            if (data.ing == null) {
                continue;
            }
            IngredientData x = data.ing;
            int lvl = LevelUtils.tierToLevel(x.tier);
            int perc = RandomUtils.RandomRange(0, 100);

            for (StatModifier s : x.getIngredient().stats) {
                if (s.GetStat() instanceof DoNotTransferToCraftedMarker == false) {
                    ExactStatData stat = s.ToExactStat(perc, lvl);
                    stat.multiplyBy(skill.craftedStatMulti);
                    stat.multiplyBy(getStatMulti());
                    stat.multiplyBy(data.multi);
                    stats.add(stat);
                }

                if (s.GetStat() instanceof RandomCraftingStat) {
                    int random = (int) s.max;
                    for (int i = 0; i < random; i++) {
                        StatModifier mod = RandomUtils.randomFromList(ExileDB.Ingredients()
                            .getFilterWrapped(e -> e.isAllowedInProfession(prof))
                            .random().stats);
                        while (mod.GetStat() instanceof DoNotTransferToCraftedMarker) {
                            mod = mod = RandomUtils.randomFromList(ExileDB.Ingredients()
                                .getFilterWrapped(e -> e.isAllowedInProfession(prof))
                                .random().stats);
                        }
                        ExactStatData stat = mod.ToExactStat(perc, lvl);
                        stat.multiplyBy(skill.craftedStatMulti);
                        stat.multiplyBy(getStatMulti());
                        stat.multiplyBy(data.multi);
                        stats.add(stat);
                    }
                }
            }

        }

        return stats;
    }

    public PlayerSkillEnum getProfession() {
        return ExileDB.PlayerSkills()
            .get(prof).type_enum;

    }

    GearRarity getMinRarity() {
        GearRarity rar = ExileDB.GearRarities()
            .get(IRarity.COMMON_ID);
        int uptimes = getIngredientsCount() / 4;
        int plusmin = getStat(IncreaseMinRarityStat.getInstance());
        uptimes += plusmin;

        for (int i = 0; i < uptimes; i++) {
            if (rar.hasHigherRarity()) {
                rar = rar.getHigherRarity();
            }
        }

        return rar;
    }

    int getStat(Stat stat) {
        int num = 0;

        for (IngredientData x : getAllIngredients()) {

            for (StatModifier e : x.getIngredient().stats) {
                if (e.GetStat()
                    .equals(stat)) {
                    num += (e.min + e.max) / 2;
                }
            }

        }

        return num;

    }

    GearRarity getMaxRarity() {
        GearRarity rar = ExileDB.GearRarities()
            .get(IRarity.COMMON_ID);
        int uptimes = getIngredientsCount();

        for (int i = 0; i < uptimes; i++) {
            if (rar.hasHigherRarity()) {
                rar = rar.getHigherRarity();
            }
        }
        return rar;
    }

    public int getIngredientsCount() {
        return ingredients.stream()
            .filter(x -> x.ing != null)
            .collect(Collectors.toList())
            .size();
    }

    public List<GearRarity> getPossibleRarities() {

        GearRarity min = getMinRarity();
        GearRarity max = getMaxRarity();

        List<GearRarity> possible = new ArrayList<>();

        possible.add(min);
        possible.add(max);

        while (min.hasHigherRarity() && min.item_tier < max.item_tier) {
            possible.add(min.getHigherRarity());
            min = min.getHigherRarity();
        }

        possible = possible.stream()
            .distinct()
            .collect(Collectors.toList());

        return possible;

    }

    public GearItemData craftGear(ItemStack stack, Player player) {
        GearItemData data = new GearItemData();

        GearSlot slot = GearSlot.getSlotOf(stack.getItem());

        BaseGearType baseGearType = ExileDB.GearTypes()
            .getFilterWrapped(x -> x.gear_slot.equals(slot.GUID()))
            .random();

        GearRarity rar = RandomUtils.weightedRandom(getPossibleRarities());

        data.rarity = rar.GUID();
        int tier = getAverageTier();
        int lvl = LevelUtils.tierToLevel(tier);

        data.lvl = lvl;
        data.gear_type = baseGearType.GUID();

        data.baseStats.RerollFully(data);
        data.imp.RerollFully(data);

        List<ExactStatData> stats = getResultingStats();

        data.cr = new CraftedStatsData();

        int maxCrafted = getMaxCraftedStats(rar);

        while (stats.size() > maxCrafted) {
            stats.remove(RandomUtils.RandomRange(0, stats.size() - 1));
        }

        stats.forEach(x -> {
            if (x.getStat() instanceof DoNotTransferToCraftedMarker == false) {
                CraftStatData singleStatData = new CraftStatData();
                singleStatData.s = x;
                data.cr.stats.add(singleStatData);
            }
        });

        data.up.regenerate(data.getRarity());

        return data; // todo
    }

    /**
     * Сколько статов от ингредиентов остаётся на скрафченном предмете.
     *
     * Раньше лимитом был {@code rar.affixes} — а у common он равен 0. Из-за этого
     * 67% крафтов выходили вообще без статов, а 6 ингредиентов давали в среднем
     * 1.2 стата из 6-12. Теперь потолок свой: редкость задаёт максимум, число
     * ингредиентов — сколько игрок реально вложил (но не меньше 1).
     */
    int getMaxCraftedStats(GearRarity rar) {
        String id = rar == null ? "" : rar.GUID();
        int byRarity;
        if (IRarity.COMMON_ID.equals(id)) {
            byRarity = 2;
        } else if (IRarity.UNCOMMON.equals(id)) {
            byRarity = 3;
        } else if (IRarity.RARE_ID.equals(id)) {
            byRarity = 4;
        } else if (IRarity.EPIC_ID.equals(id)) {
            byRarity = 5;
        } else {
            byRarity = 3;
        }
        return Math.max(1, Math.min(byRarity, Math.max(1, getIngredientsCount())));
    }

    public CraftedConsumableData craftConsumable(Player player) {

        PlayerSkillEnum skill = getProfession();

        List<ExactStatData> stats = getResultingStats();

        CraftedConsumableData data = new CraftedConsumableData();
        data.prof = prof;

        data.stats.addAll(stats);

        Item item = skill.getCraftResultItem();

        data.tier = this.getAverageTier();
        data.crafter = player.getGameProfile()
            .name();

        if (skill == PlayerSkillEnum.COOKING) {
            data.uses = 3;
            data.maxuses = 3;
            data.seconds = 60 * 30;
        } else if (skill == PlayerSkillEnum.ALCHEMY) {
            data.uses = 6;
            data.maxuses = 6;
            data.seconds = 60 * 10;
        } else if (skill == PlayerSkillEnum.INSCRIBING) {
            data.uses = 3;
            data.maxuses = 3;
            data.seconds = 60 * 5;
        }

        int plusUses = getStat(MaxUsesStat.getInstance());

        data.uses += plusUses;
        data.maxuses += plusUses;

        return data;

    }
}
