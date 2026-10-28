package com.robertx22.age_of_exile.player_skills.ingredient.data;

import com.robertx22.age_of_exile.database.data.StatModifier;
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
import com.robertx22.age_of_exile.saveclasses.stat_soul.SoullessGear;
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
        List<IngredientData> all = getAllIngredients();
        if (all.isEmpty()) {
            return 0; // без ингредиентов среднего тира нет; такой крафт всё равно не проходит canCraft
        }
        return all.stream()
            .mapToInt(x -> x.tier)
            .sum()
            / all.size();
    }

    public float getStatMulti() {

        int total = ingredients.size();

        List<SlashIngredient> distinct = getAllIngredients().stream()
            .map(x -> x.getIngredient())
            .filter(x -> x != null)
            .distinct()
            .collect(Collectors.toList());

        float multi = 1F;

        for (SlashIngredient x : distinct) {
            int amount = (int) this.getAllIngredients()
                .stream()
                .filter(e -> e.getIngredient() != null && e.getIngredient()
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

            tip.add(Component.literal(""));
            tip.add(Component.literal("Результат: бесдушная основа (обычный предмет)")
                .withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("Сила основы: " + SoullessGear.basePercent(craftCostFactor()) + "% от базовых статов")
                .withStyle(ChatFormatting.DARK_GRAY));
            tip.add(Component.literal("Редкость, аффиксы и имя даёт наложенная душа")
                .withStyle(ChatFormatting.DARK_GRAY));

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
                        StatModifier mod = randomTransferableStat();
                        if (mod == null) {
                            break; // нет подходящих статов — просто ничего не добавляем
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

    /**
     * Случайный стат из случайного ингредиента, доступного профессии, из тех, что переносятся в крафт.
     * Ограничено числом попыток, чтобы не зависнуть, если таких статов нет.
     */
    private StatModifier randomTransferableStat() {
        for (int attempt = 0; attempt < 50; attempt++) {
            SlashIngredient ing = ExileDB.Ingredients()
                .getFilterWrapped(e -> e.isAllowedInProfession(prof))
                .random();
            if (ing == null) {
                return null;
            }
            List<StatModifier> transferable = ing.stats.stream()
                .filter(s -> !(s.GetStat() instanceof DoNotTransferToCraftedMarker))
                .collect(Collectors.toList());
            if (!transferable.isEmpty()) {
                return RandomUtils.randomFromList(transferable);
            }
        }
        return null;
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
        // Слот определяем тем же путём, что и выдача основы у дропа: крафт тоже
        // обязан давать бесдушную основу, в том числе для модового оружия без слота AoE.
        String slotId = SoullessGear.slotIdFor(stack);
        if (slotId == null) {
            return null;
        }

        int tier = getAverageTier();
        int lvl = LevelUtils.tierToLevel(tier);

        BaseGearType baseGearType = SoullessGear.pickType(slotId, lvl);
        if (baseGearType == null) {
            return null;
        }

        // Крафт всегда даёт «бесдушную основу» common-редкости: редкость, случайные аффиксы
        // и имя предмета приносит только настоящая душа. Чем дороже крафт (больше и выше
        // тир ингредиентов, лучше множитель), тем выше базовые статы этой основы.
        GearItemData data = SoullessGear.create(baseGearType, lvl, craftCostFactor());

        List<ExactStatData> stats = getResultingStats();

        data.cr = new CraftedStatsData();

        while (stats.size() > CRAFTED_STATS_CAP) {
            // don't allow more stats than a soulless base can hold
            stats.remove(RandomUtils.RandomRange(0, stats.size() - 1));
        }

        stats.forEach(x -> {
            if (x.getStat() instanceof DoNotTransferToCraftedMarker == false) {
                CraftStatData singleStatData = new CraftStatData();
                singleStatData.s = x;
                data.cr.stats.add(singleStatData);
            }
        });

        data.item_id = SoullessGear.itemId(stack);
        return data;
    }

    /** Сколько статов из ингредиентов помещается в бесдушную основу. */
    public static final int CRAFTED_STATS_CAP = 3;

    /**
     * «Дороговизна» крафта: количество ингредиентов, их средний тир и множитель
     * характеристик (штраф за повторы). 0 — дешёвый крафт, 1 — максимально дорогой.
     */
    public float craftCostFactor() {
        int count = Mth.clamp(getIngredientsCount(), 1, 9);
        int avgTier = getAverageTier();

        float factor = 0.15F + 0.55F * (count / 9F) + 0.03F * avgTier;
        factor *= getStatMulti();

        return Mth.clamp(factor, 0F, 1F);
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
