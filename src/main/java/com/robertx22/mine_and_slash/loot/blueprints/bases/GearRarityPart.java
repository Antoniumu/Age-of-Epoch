package com.robertx22.mine_and_slash.loot.blueprints.bases;

import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarityType;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.TreasureQuality;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.ItemBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.MapBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.SkillGemBlueprint;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;

import java.util.ArrayList;
import java.util.List;

public class GearRarityPart extends BlueprintPart<GearRarity, ItemBlueprint> {


    GearRarity specialRar = null;

    public float chanceForHigherRarity = 0;

    public boolean canRollUnique = false;
    public boolean canRollRuned = false;


    public GearRarityPart(ItemBlueprint blueprint) {
        super(blueprint);
        if (blueprint instanceof GearBlueprint) {
            canRollUnique = true;
            canRollRuned = true;
        }
    }

    public void setupChances(LootInfo info) {
        if (info.playerEntityData != null) {
            chanceForHigherRarity += info.playerEntityData.getUnit().getCalculatedStat(TreasureQuality.getInstance()).getValue();
        }
    }

    // todo clean this up
    public List<GearRarity> getPossibleRarities() {

        // todo fix this better
        if (blueprint instanceof SkillGemBlueprint) {
            return ExileDB.GearRarities().getFiltered(x -> this.blueprint.info.level >= x.min_lvl && !x.is_unique_item && x.getLowerRarity().isPresent() || x.hasHigherRarity());
        }

        if (this.blueprint instanceof MapBlueprint) {
            return ExileDB.GearRarities().getFiltered(x -> x.type == GearRarityType.NORMAL);
        }

        var filt = ExileDB.GearRarities().getFilterWrapped(x -> this.blueprint.info.level >= x.min_lvl);

        if (!canRollUnique) {
            filt = filt.of(x -> !x.is_unique_item);
        }
        if (!canRollRuned) {
            filt = filt.of(x -> !x.guid.equals(IRarity.RUNEWORD_ID));
        }

        // prevent high rarity gear from dropping in low rarity maps
        filt = filt.of(x -> this.blueprint.info.map_tier >= ExileDB.GearRarities().get(x.min_map_rarity_to_drop).map_tiers.min);

        return filt.list;

    }

    /**
     * Returns a list of WeightedRarity, where unique rarities have their weight increased by magic find.
     */
    private List<WeightedRarity> getWeightedRarities(List<GearRarity> possible) {
        List<WeightedRarity> weighted = new ArrayList<>();
        for (GearRarity rar : possible) {
            int weight = rar.Weight();
            if (rar.guid.equals(IRarity.UNIQUE_ID) && canRollUnique) {
                // Increase unique weight, tweak as needed
                float multiplier = 1.0f + (chanceForHigherRarity / 200f);
                // Clamp multiplier to avoid excessive values
                multiplier = MathHelper.clamp(multiplier, 1.0f, 10.0f);
                weight = Math.round(weight * multiplier);
            }
            if (rar.guid.equals(IRarity.RUNEWORD_ID) && canRollRuned) {
                // Increase runed weight, tweak as needed
                float multiplier = 1.0f + (chanceForHigherRarity / 200f);
                // Clamp multiplier to avoid excessive values
                multiplier = MathHelper.clamp(multiplier, 1.0f, 10.0f);
                weight = Math.round(weight * multiplier);
            }
            weighted.add(new WeightedRarity(rar, weight));
        }
        return weighted;
    }

    private static class WeightedRarity {
        public final GearRarity rarity;
        public final int weight;

        public WeightedRarity(GearRarity rarity, int weight) {
            this.rarity = rarity;
            this.weight = weight;
        }
    }

    private GearRarity weightedRandomRarity(List<WeightedRarity> weighted) {
        int totalWeight = weighted.stream().mapToInt(w -> w.weight).sum();
        if (totalWeight <= 0) {
            // fallback: pick any
            return weighted.get(0).rarity;
        }
        int roll = RandomUtils.RandomRange(1, totalWeight);
        int cumulative = 0;
        for (WeightedRarity wr : weighted) {
            cumulative += wr.weight;
            if (roll <= cumulative) {
                return wr.rarity;
            }
        }
        // fallback
        return weighted.get(0).rarity;
    }

    @Override
    protected GearRarity generateIfNull() {

        if (this.specialRar != null) {
            return specialRar;
        }
        var possible = getPossibleRarities();

        // Adjust weights for unique rarities based on magic find
        var weighted = getWeightedRarities(possible);

        GearRarity rar = weightedRandomRarity(weighted);

        if (rar.hasHigherRarity()) {
            var higher = rar.getHigherRarity();

            // the rarer the higher rarity, the harder it should be for magic find to upgrade the rarity
            float chanceMulti = MathHelper.clamp((float) higher.Weight() / (float) rar.Weight(), 0F, 1F);

            float chance = chanceForHigherRarity * chanceMulti;

            if (RandomUtils.roll(chance)) {
                if (rar.hasHigherRarity() && getPossibleRarities().contains(rar.getHigherRarity())) {
                    if (blueprint.info.level >= rar.getHigherRarity().min_lvl) {
                        rar = rar.getHigherRarity();
                    }
                }
            }
        }

        return rar;
    }
}


