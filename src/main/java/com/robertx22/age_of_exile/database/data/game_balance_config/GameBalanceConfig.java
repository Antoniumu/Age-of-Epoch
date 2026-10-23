package com.robertx22.age_of_exile.database.data.game_balance_config;

import com.robertx22.age_of_exile.database.data.MinMax;
import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;

import java.util.HashMap;

public class GameBalanceConfig implements JsonExileRegistry<GameBalanceConfig>, IAutoGson<GameBalanceConfig> {

    public static GameBalanceConfig SERIALIZER = new GameBalanceConfig();

    public String id = "game_balance";
    public static String ID = "game_balance";

    public static GameBalanceConfig get() {
        return (GameBalanceConfig) Database.getRegistry(ExileRegistryTypes.GAME_BALANCE)
            .get(ID);
    }

    public int MAX_LEVEL = 50;

    /**
     * До какого уровня гайд ведёт игрока. Дальше подсказки, подсветка кнопок и
     * цели по характеристикам/талантам/заклинаниям отключаются — игрок решает сам.
     *
     * Настраивается хостом: датапак, файл
     * {@code data/mmorpg/game_balance/game_balance.json} (поле GUIDE_MAX_LEVEL).
     * 0 или отрицательное значение означает «как задумано по умолчанию» (20),
     * чтобы старые датапаки без этого поля не отключали гайды совсем.
     */
    public static final int DEFAULT_GUIDE_MAX_LEVEL = 20;

    public int GUIDE_MAX_LEVEL = DEFAULT_GUIDE_MAX_LEVEL;

    /** Предел гайда с защитой от 0/мусора и от выхода за MAX_LEVEL. */
    public int guideMaxLevel() {
        int cap = GUIDE_MAX_LEVEL;
        if (cap <= 0) {
            cap = DEFAULT_GUIDE_MAX_LEVEL; // поля не было в датапаке — GSON оставил 0
        }
        int max = MAX_LEVEL > 0 ? MAX_LEVEL : 50;
        return Math.min(cap, max);
    }

    public LevelScalingConfig NORMAL_STAT_SCALING = new LevelScalingConfig(1, 0.2F, false);
    public LevelScalingConfig SLOW_STAT_SCALING = new LevelScalingConfig(1, 0.01F, true);

    /**
     * Стоимость маны растёт медленнее уровня: x3.45 к 50-му вместо x10.8.
     * Иначе один каст базового заклинания на 50-м уровне стоит 12 секунд регена.
     */
    public LevelScalingConfig MANA_COST_SCALING = new LevelScalingConfig(1, 0.05F, true);

    /**
     * Стоимость удара оружием. Раньше энергия стоила {@code slot.energy_cost * N(lvl)},
     * из-за чего к 50-му уровню устойчивый темп падал до 0.1 удара в секунду.
     * Теперь стоимость растёт всего в 3.45 раза, а шкала держит ~23 удара.
     */
    public LevelScalingConfig ENERGY_COST_SCALING = new LevelScalingConfig(1, 0.05F, true);

    public LevelScalingConfig CORE_STAT_SCALING = new LevelScalingConfig(1, 0.05F, true);
    public LevelScalingConfig STAT_REQ_SCALING = new LevelScalingConfig(2, 2, true);

    /**
     * Какую долю максимального запаса восстанавливает регенерация за секунду
     * (сверх плоского значения стата регена). 0.08 = 8% в секунду.
     * Благодаря этому темп боя не отстаёт от роста стоимости ударов.
     */
    public float ENERGY_REGEN_PERCENT_PER_SECOND = 0.08F;
    public float MANA_REGEN_PERCENT_PER_SECOND = 0.08F;

    /** Сколько «плоских» HP даёт каждый уровень игрока сверх базовых. */
    public float PLAYER_HEALTH_PER_LEVEL = 1.5F;

    /**
     * Насколько сильно вложения в характеристики масштабируются уровнем.
     * 1.0 — как раньше (x10.8 к 50-му, что давало x17 HP за 100 очков VIT).
     * 0.25 — вклад растёт как x3.45 к 50-му, и очки остаются сопоставимы с лутом.
     */
    public float CORE_STAT_LEVEL_FACTOR = 0.25F;

    public double TALENT_POINTS_AT_MAX_LEVEL = 100;
    public double STAT_POINTS_PER_LEVEL = 2;
    public double SPELL_POINTS_PER_LEVEL = 2;
    public double STARTING_TALENT_POINTS = 1;

    public int levels_per_tier = 10;

    public HashMap<MinMax, Integer> getTierMap() {

        HashMap<MinMax, Integer> tiermap = new HashMap<>();

        int tier = 0;

        int min = 0;
        int max = levels_per_tier;

        while (min <= MAX_LEVEL) {

            int addlast = min + levels_per_tier >= MAX_LEVEL ? 1000 : 0;

            tiermap.put(new MinMax(min, max + addlast), tier);
            min += levels_per_tier;
            max += levels_per_tier;
            tier++;

        }

        return tiermap;

    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.GAME_BALANCE;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public Class<GameBalanceConfig> getClassForSerialization() {
        return GameBalanceConfig.class;
    }

    @Override
    public void onLoadedFromJson() {
        // GSON создаёт объект без конструктора: если в датапаке нет GUIDE_MAX_LEVEL,
        // поле станет 0. Нормализуем, чтобы гайды не выключились молча.
        GUIDE_MAX_LEVEL = guideMaxLevel();
    }

    @Override
    public int Weight() {
        return 1000;
    }
}
