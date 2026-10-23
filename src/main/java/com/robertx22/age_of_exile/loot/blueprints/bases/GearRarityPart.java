package com.robertx22.age_of_exile.loot.blueprints.bases;

import com.robertx22.age_of_exile.database.data.groups.GearRarityGroups;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.data.stats.types.loot.TreasureQuality;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.database.registry.RarityRegistryContainer;
import com.robertx22.age_of_exile.gamemaster.GameMaster;
import com.robertx22.age_of_exile.loot.LootInfo;
import com.robertx22.age_of_exile.loot.blueprints.GearBlueprint;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class GearRarityPart extends BlueprintPart<GearRarity, GearBlueprint> {

    RarityRegistryContainer<GearRarity> container;

    GearRarity specialRar = null;

    public List<GearRarity> possible = ExileDB.GearRarityGroups()
        .get(GearRarityGroups.DROPPABLE_RARITIES_ID)
        .getRarities();

    public float chanceForHigherRarity = 0;

    /** Информация о дропе — нужна для pity и гарантированных наград удержания. */
    private LootInfo lootInfo;

    public GearRarityPart(GearBlueprint blueprint) {
        super(blueprint);
        this.container = blueprint.getRarityContainer();
    }

    public void setupChances(LootInfo info) {

        this.lootInfo = info;

        this.possible.removeIf(x -> !info.favorRank.drop_unique_gears && x.is_unique_item);

        if (info.playerData != null) {
            if (info.lootOrigin == LootInfo.LootOrigin.CHEST) {
                chanceForHigherRarity += info.playerData.getUnit()
                    .getCalculatedStat(TreasureQuality.getInstance())
                    .getValue();
            }
        }

        if (info.favorRank != null) {
            possible.removeIf(x -> info.favorRank.excludedRarities.stream()
                .anyMatch(e -> e.equals(x.GUID())));
        }

    }

    @Override
    protected GearRarity generateIfNull() {

        if (this.specialRar != null) {
            trackRarity(specialRar);
            return specialRar;
        }

        GearRarity rar = RandomUtils.weightedRandom(possible);

        if (rar.hasHigherRarity()) {
            if (RandomUtils.roll(chanceForHigherRarity)) {
                if (rar.hasHigherRarity() && possible.contains(rar.getHigherRarity())) {
                    rar = rar.getHigherRarity();
                }
            }
        }

        GearRarity result = applyRetention(rar);
        trackRarity(result);
        return result;
    }

    /** Телеметрия: какая редкость реально выпала (см. Telemetry). */
    private void trackRarity(GearRarity rarity) {
        try {
            if (rarity == null || lootInfo == null) {
                return;
            }
            if (lootInfo.player instanceof ServerPlayer player) {
                GameMaster.get()
                    .telemetry()
                    .gear(player, rarity.GUID(), "", lootInfo.level);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Удержание: защита от невезения (pity) и гарантированные награды за вехи/серии.
     * Работает только на сервере и только если в дропе есть игрок; любые сбои
     * не должны ломать генерацию лута.
     */
    private GearRarity applyRetention(GearRarity rar) {
        try {
            if (lootInfo == null || !(lootInfo.player instanceof ServerPlayer player)) {
                return rar;
            }
            GameMaster gm = GameMaster.get();
            if (!gm.config().retention_enabled) {
                return rar;
            }

            boolean uniqueAllowed = possible.stream()
                .anyMatch(x -> IRarity.UNIQUE_ID.equals(x.GUID()));

            // 1. Сухая серия: после долгого отсутствия уникатов он выпадает принудительно.
            if (uniqueAllowed && !rar.is_unique_item
                && RandomUtils.roll(gm.uniquePityChance(player) * 100D)) {
                GearRarity unique = ExileDB.GearRarities()
                    .get(IRarity.UNIQUE_ID);
                if (unique != null) {
                    gm.onUniqueDropped(player);
                    return unique;
                }
            }
            if (rar.is_unique_item) {
                gm.onUniqueDropped(player);
                return rar;
            }

            // 2. Гарантированная редкость: сначала смотрим, что даст заряд,
            // и только если он реально повышает предмет — тратим его.
            int charge = gm.guaranteedRarityCharges(player);
            if (charge >= 2 && possible.contains(ExileDB.GearRarities()
                .get(IRarity.EPIC_ID))) {
                gm.consumeGuaranteedRarity(player);
                return ExileDB.GearRarities()
                    .get(IRarity.EPIC_ID);
            }
            if (charge >= 1 && isLowRarity(rar) && possible.contains(ExileDB.GearRarities()
                .get(IRarity.RARE_ID))) {
                gm.consumeGuaranteedRarity(player);
                return ExileDB.GearRarities()
                    .get(IRarity.RARE_ID);
            }
        } catch (Exception ignored) {
            // награда удержания не должна ломать лут
        }
        return rar;
    }

    private static boolean isLowRarity(GearRarity rar) {
        return rar != null && (IRarity.COMMON_ID.equals(rar.GUID()) || IRarity.UNCOMMON.equals(rar.GUID()));
    }

}
