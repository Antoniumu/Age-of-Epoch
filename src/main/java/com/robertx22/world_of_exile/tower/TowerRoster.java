package com.robertx22.world_of_exile.tower;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every tower theme ships five bosses: four main bosses of increasing rank and
 * one elite that is used as a champion.  A tower picks the strongest boss its
 * rank allows and fills the hall with lower ranked champions, so a colossal
 * spire feels different from a small keep even in the same biome.
 *
 * <p>Ranks: 1 = common, 2 = veteran, 3 = elite, 4 = legendary.  The final rank
 * of a tower is {@code clamp(1 + heightBonus + theme.baseRank - 2, 1, 4)}.</p>
 */
public final class TowerRoster {

    private static final Map<String, List<TowerBossDef>> BY_THEME = new LinkedHashMap<>();

    static {
        // ------------------------------------------------------------ plains
        add("plains",
            TowerBossDef.of("bandit_chief", "Атаман разбойников", "minecraft:pillager", 1, 1.3D, 1.1D,
                TowerAbility.KNOCKBACK_SLAM).withWeapon("minecraft:iron_axe"),
            TowerBossDef.of("grave_knight", "Могильный рыцарь", "minecraft:skeleton", 2, 1.7D, 1.25D,
                TowerAbility.SHIELD, TowerAbility.KNOCKBACK_SLAM).withWeapon("minecraft:iron_sword"),
            TowerBossDef.of("warlord", "Военный вождь", "minecraft:vindicator", 3, 2.2D, 1.5D,
                TowerAbility.RAGE, TowerAbility.SUMMON_MINIONS).withWeapon("minecraft:diamond_axe"),
            TowerBossDef.of("headsman", "Палач равнин", "minecraft:vindicator", 3, 2.4D, 1.7D,
                TowerAbility.CURSE_OF_WEAKNESS, TowerAbility.KNOCKBACK_SLAM).withWeapon("minecraft:iron_axe"),
            TowerBossDef.of("conqueror", "Завоеватель", "minecraft:ravager", 4, 3.4D, 1.9D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.RAGE, TowerAbility.SUMMON_MINIONS));

        // ------------------------------------------------------------ forest
        add("forest",
            TowerBossDef.of("broodmother", "Паук-матриарх", "minecraft:spider", 1, 1.3D, 1.1D,
                TowerAbility.POISON_CLOUD),
            TowerBossDef.of("strangler", "Душитель-древень", "minecraft:zombie", 2, 1.8D, 1.2D,
                TowerAbility.POISON_CLOUD, TowerAbility.KNOCKBACK_SLAM),
            TowerBossDef.of("herbalist", "Ведьма-травница", "minecraft:witch", 3, 2.0D, 1.4D,
                TowerAbility.POISON_CLOUD, TowerAbility.HEAL_SELF),
            TowerBossDef.of("grove_keeper", "Хранитель рощи", "minecraft:evoker", 3, 2.1D, 1.5D,
                TowerAbility.SUMMON_MINIONS, TowerAbility.CURSE_OF_WEAKNESS),
            TowerBossDef.of("ancient_beast", "Древний зверь", "minecraft:ravager", 4, 3.2D, 1.8D,
                TowerAbility.RAGE, TowerAbility.SUMMON_MINIONS, TowerAbility.KNOCKBACK_SLAM));

        // --------------------------------------------------------------- ice
        add("ice",
            TowerBossDef.of("hoarfrost_sentry", "Иний-стражник", "minecraft:stray", 1, 1.3D, 1.1D,
                TowerAbility.FROST_NOVA),
            TowerBossDef.of("frost_knight", "Ледяной рыцарь", "minecraft:skeleton", 2, 1.8D, 1.3D,
                TowerAbility.FROST_NOVA, TowerAbility.SHIELD).withWeapon("minecraft:iron_sword"),
            TowerBossDef.of("winter_priestess", "Морозная жрица", "minecraft:stray", 3, 2.1D, 1.5D,
                TowerAbility.FROST_NOVA, TowerAbility.SUMMON_MINIONS, TowerAbility.HEAL_SELF),
            TowerBossDef.of("glacier_warden", "Страж ледника", "minecraft:skeleton", 3, 2.4D, 1.6D,
                TowerAbility.SHIELD, TowerAbility.THUNDER_STOMP),
            TowerBossDef.of("snow_titan", "Снежный титан", "minecraft:ravager", 4, 3.6D, 1.9D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.FROST_NOVA, TowerAbility.RAGE));

        // ------------------------------------------------------------ desert
        add("desert",
            TowerBossDef.of("tomb_guard", "Гробничный страж", "minecraft:husk", 1, 1.4D, 1.1D,
                TowerAbility.CURSE_OF_WEAKNESS),
            TowerBossDef.of("sand_priest", "Песчаный жрец", "minecraft:skeleton", 2, 1.7D, 1.2D,
                TowerAbility.SUMMON_MINIONS, TowerAbility.POISON_CLOUD),
            TowerBossDef.of("mummy_pharaoh", "Мумия-фараон", "minecraft:husk", 3, 2.5D, 1.5D,
                TowerAbility.CURSE_OF_WEAKNESS, TowerAbility.HEAL_SELF, TowerAbility.KNOCKBACK_SLAM),
            TowerBossDef.of("scorpion_matriarch", "Скорпион-матриарх", "minecraft:cave_spider", 3, 2.0D, 1.7D,
                TowerAbility.POISON_CLOUD, TowerAbility.KNOCKBACK_SLAM),
            TowerBossDef.of("dune_lord", "Владыка дюн", "minecraft:ravager", 4, 3.3D, 1.9D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.RAGE, TowerAbility.SUMMON_MINIONS));

        // ------------------------------------------------------------ jungle
        add("jungle",
            TowerBossDef.of("stalker", "Паук-охотник", "minecraft:cave_spider", 1, 1.3D, 1.2D,
                TowerAbility.POISON_CLOUD),
            TowerBossDef.of("serpent_priest", "Змеиный жрец", "minecraft:witch", 2, 1.7D, 1.3D,
                TowerAbility.POISON_CLOUD, TowerAbility.HEAL_SELF),
            TowerBossDef.of("temple_guardian", "Охранник храма", "minecraft:vindicator", 3, 2.3D, 1.6D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.SUMMON_MINIONS).withWeapon("minecraft:iron_axe"),
            TowerBossDef.of("shadow_matriarch", "Матриарх теней", "minecraft:spider", 3, 2.0D, 1.6D,
                TowerAbility.POISON_CLOUD, TowerAbility.BLINK),
            TowerBossDef.of("ancient_yaksha", "Древний якш", "minecraft:ravager", 4, 3.3D, 1.9D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.RAGE, TowerAbility.SUMMON_MINIONS));

        // ------------------------------------------------------------- swamp
        add("swamp",
            TowerBossDef.of("bog_drowned", "Болотный утопленник", "minecraft:drowned", 1, 1.3D, 1.1D,
                TowerAbility.LEECH).withWeapon("minecraft:iron_sword"),
            TowerBossDef.of("bog_witch", "Ведьма-болотница", "minecraft:witch", 2, 1.7D, 1.3D,
                TowerAbility.POISON_CLOUD, TowerAbility.HEAL_SELF),
            TowerBossDef.of("slime_ancestor", "Слизень-прародитель", "minecraft:slime", 3, 2.4D, 1.4D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.SUMMON_MINIONS),
            TowerBossDef.of("mire_warden", "Хозяин трясины", "minecraft:drowned", 3, 2.2D, 1.6D,
                TowerAbility.LEECH, TowerAbility.CURSE_OF_WEAKNESS),
            TowerBossDef.of("peat_giant", "Тинный гигант", "minecraft:ravager", 4, 3.4D, 1.8D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.POISON_CLOUD, TowerAbility.RAGE));

        // ---------------------------------------------------------- badlands
        add("badlands",
            TowerBossDef.of("dust_raider", "Пыльный налётчик", "minecraft:pillager", 1, 1.3D, 1.1D,
                TowerAbility.KNOCKBACK_SLAM),
            TowerBossDef.of("canyon_spirit", "Каньонный дух", "minecraft:husk", 2, 1.7D, 1.3D,
                TowerAbility.CURSE_OF_WEAKNESS, TowerAbility.LEECH),
            TowerBossDef.of("fortress_master", "Начальник заставы", "minecraft:vindicator", 3, 2.3D, 1.6D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.RAGE).withWeapon("minecraft:diamond_axe"),
            TowerBossDef.of("bone_cowboy", "Костяной ковбой", "minecraft:skeleton", 3, 2.1D, 1.5D,
                TowerAbility.SUMMON_MINIONS, TowerAbility.CURSE_OF_WEAKNESS),
            TowerBossDef.of("clay_titan", "Глиняный титан", "minecraft:ravager", 4, 3.5D, 1.8D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.SHIELD, TowerAbility.RAGE));

        // ----------------------------------------------------------- savanna
        add("savanna",
            TowerBossDef.of("steppe_marauder", "Степной мародёр", "minecraft:pillager", 1, 1.3D, 1.1D,
                TowerAbility.KNOCKBACK_SLAM),
            TowerBossDef.of("pride_chief", "Вождь прайда", "minecraft:vindicator", 2, 1.8D, 1.4D,
                TowerAbility.RAGE).withWeapon("minecraft:iron_axe"),
            TowerBossDef.of("savanna_storm", "Гроза саванны", "minecraft:ravager", 3, 2.6D, 1.7D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.RAGE),
            TowerBossDef.of("bone_hunter", "Костяной охотник", "minecraft:skeleton", 3, 2.0D, 1.5D,
                TowerAbility.SUMMON_MINIONS, TowerAbility.POISON_CLOUD),
            TowerBossDef.of("beast_king", "Царь зверей", "minecraft:ravager", 4, 3.4D, 1.9D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.RAGE, TowerAbility.HEAL_SELF));

        // ---------------------------------------------------------- mountain
        add("mountain",
            TowerBossDef.of("rocksplitter", "Камнелом", "minecraft:zombie", 1, 1.4D, 1.2D,
                TowerAbility.KNOCKBACK_SLAM).withWeapon("minecraft:stone_axe"),
            TowerBossDef.of("ridge_archer", "Горный стрелок", "minecraft:skeleton", 2, 1.7D, 1.3D,
                TowerAbility.SUMMON_MINIONS),
            TowerBossDef.of("pass_warden", "Страж перевала", "minecraft:vindicator", 3, 2.4D, 1.6D,
                TowerAbility.SHIELD, TowerAbility.KNOCKBACK_SLAM).withWeapon("minecraft:iron_axe"),
            TowerBossDef.of("cliff_spirit", "Скальный дух", "minecraft:skeleton", 3, 2.1D, 1.6D,
                TowerAbility.CURSE_OF_WEAKNESS, TowerAbility.BLINK),
            TowerBossDef.of("mountain_colossus", "Горный колосс", "minecraft:ravager", 4, 3.7D, 1.9D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.SHIELD, TowerAbility.RAGE));

        // -------------------------------------------------------------- coast
        add("coast",
            TowerBossDef.of("drowned_sailor", "Утопший матрос", "minecraft:drowned", 1, 1.3D, 1.1D,
                TowerAbility.LEECH),
            TowerBossDef.of("reef_guardian", "Страж рифа", "minecraft:guardian", 2, 1.8D, 1.3D,
                TowerAbility.FROST_NOVA, TowerAbility.SHIELD),
            TowerBossDef.of("pirate_captain", "Пират-капитан", "minecraft:drowned", 3, 2.3D, 1.6D,
                TowerAbility.LEECH, TowerAbility.RAGE, TowerAbility.SUMMON_MINIONS).withWeapon("minecraft:iron_sword"),
            TowerBossDef.of("abyss_priest", "Глубинный жрец", "minecraft:drowned", 3, 2.2D, 1.5D,
                TowerAbility.SHIELD, TowerAbility.HEAL_SELF),
            TowerBossDef.of("ancient_leviathan", "Древний левиафан", "minecraft:elder_guardian", 4, 3.6D, 1.8D,
                TowerAbility.FROST_NOVA, TowerAbility.LEECH, TowerAbility.SHIELD, TowerAbility.SUMMON_ELITE));

        // ----------------------------------------------------------- mushroom
        add("mushroom",
            TowerBossDef.of("spore_spider", "Споровый паук", "minecraft:spider", 1, 1.3D, 1.1D,
                TowerAbility.POISON_CLOUD),
            TowerBossDef.of("myconid_seer", "Грибной ведун", "minecraft:witch", 2, 1.7D, 1.3D,
                TowerAbility.POISON_CLOUD, TowerAbility.HEAL_SELF),
            TowerBossDef.of("rot_matriarch", "Матриарх гнили", "minecraft:cave_spider", 3, 2.2D, 1.6D,
                TowerAbility.POISON_CLOUD, TowerAbility.SUMMON_MINIONS),
            TowerBossDef.of("walking_mycelium", "Ходячий мицелий", "minecraft:zombie", 3, 2.5D, 1.5D,
                TowerAbility.LEECH, TowerAbility.POISON_CLOUD),
            TowerBossDef.of("spore_colossus", "Споролиск", "minecraft:ravager", 4, 3.4D, 1.8D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.POISON_CLOUD, TowerAbility.SUMMON_MINIONS));

        // -------------------------------------------------------- dark forest
        add("dark_forest",
            TowerBossDef.of("gloom_herald", "Сумрачный вестник", "minecraft:vex", 1, 1.2D, 1.2D,
                TowerAbility.BLINK),
            TowerBossDef.of("order_executioner", "Палач ордена", "minecraft:vindicator", 2, 1.9D, 1.5D,
                TowerAbility.CURSE_OF_WEAKNESS, TowerAbility.KNOCKBACK_SLAM).withWeapon("minecraft:iron_axe"),
            TowerBossDef.of("shadow_caster", "Заклинатель теней", "minecraft:evoker", 3, 2.3D, 1.6D,
                TowerAbility.SUMMON_MINIONS, TowerAbility.CURSE_OF_WEAKNESS),
            TowerBossDef.of("renegade_witch", "Ведьма-отступница", "minecraft:witch", 3, 2.2D, 1.6D,
                TowerAbility.POISON_CLOUD, TowerAbility.HEAL_SELF, TowerAbility.BLINK),
            TowerBossDef.of("sanctum_lord", "Владыка святилища", "minecraft:evoker", 4, 3.4D, 1.9D,
                TowerAbility.SUMMON_MINIONS, TowerAbility.SUMMON_ELITE, TowerAbility.WITHER_CURSE,
                TowerAbility.SHIELD, TowerAbility.BLINK));

        // ------------------------------------------------------- nether wastes
        add("nether_wastes",
            TowerBossDef.of("ash_adept", "Пепельный послушник", "minecraft:wither_skeleton", 1, 1.4D, 1.2D,
                TowerAbility.WITHER_CURSE).withWeapon("minecraft:stone_sword"),
            TowerBossDef.of("piglin_overseer", "Пиглин-надзиратель", "minecraft:piglin_brute", 2, 1.9D, 1.5D,
                TowerAbility.KNOCKBACK_SLAM).withWeapon("minecraft:golden_axe"),
            TowerBossDef.of("flame_mage", "Огненный маг", "minecraft:blaze", 3, 2.2D, 1.6D,
                TowerAbility.FIRE_NOVA, TowerAbility.METEOR),
            TowerBossDef.of("bone_baron", "Костяной барон", "minecraft:wither_skeleton", 3, 2.5D, 1.7D,
                TowerAbility.WITHER_CURSE, TowerAbility.SUMMON_MINIONS).withWeapon("minecraft:netherite_axe"),
            TowerBossDef.of("blackstone_lord", "Владыка чернокамня", "minecraft:piglin_brute", 4, 3.6D, 2.0D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.METEOR, TowerAbility.SUMMON_MINIONS, TowerAbility.RAGE)
                .withWeapon("minecraft:netherite_axe"));

        // --------------------------------------------------- soul sand valley
        add("soul_sand_valley",
            TowerBossDef.of("soul_warden", "Страж душ", "minecraft:skeleton", 1, 1.3D, 1.1D,
                TowerAbility.WITHER_CURSE),
            TowerBossDef.of("wailer", "Плакальщик", "minecraft:ghast", 2, 1.7D, 1.3D,
                TowerAbility.FIRE_NOVA),
            TowerBossDef.of("bone_priest", "Костяной жрец", "minecraft:wither_skeleton", 3, 2.4D, 1.6D,
                TowerAbility.WITHER_CURSE, TowerAbility.HEAL_SELF, TowerAbility.SUMMON_MINIONS),
            TowerBossDef.of("soul_collector", "Собиратель душ", "minecraft:ghast", 3, 2.2D, 1.6D,
                TowerAbility.METEOR, TowerAbility.LEECH),
            TowerBossDef.of("ancient_witherer", "Древний иссушитель", "minecraft:wither_skeleton", 4, 3.7D, 2.0D,
                TowerAbility.WITHER_CURSE, TowerAbility.FIRE_NOVA, TowerAbility.SUMMON_MINIONS, TowerAbility.SHIELD)
                .withWeapon("minecraft:netherite_sword"));

        // ------------------------------------------------------ crimson forest
        add("crimson_forest",
            TowerBossDef.of("crimson_boar", "Багровый вепрь", "minecraft:hoglin", 1, 1.4D, 1.2D,
                TowerAbility.KNOCKBACK_SLAM),
            TowerBossDef.of("piglin_butcher", "Пиглин-мясник", "minecraft:piglin", 2, 1.8D, 1.4D,
                TowerAbility.RAGE).withWeapon("minecraft:golden_axe"),
            TowerBossDef.of("rabid_zoglin", "Бешеный зоглин", "minecraft:zoglin", 3, 2.5D, 1.7D,
                TowerAbility.RAGE, TowerAbility.KNOCKBACK_SLAM),
            TowerBossDef.of("crimson_priest", "Жрец багрового огня", "minecraft:piglin", 3, 2.2D, 1.6D,
                TowerAbility.FIRE_NOVA, TowerAbility.SUMMON_MINIONS),
            TowerBossDef.of("crimson_lord", "Владыка чертога", "minecraft:piglin_brute", 4, 3.6D, 2.0D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.METEOR, TowerAbility.SUMMON_MINIONS, TowerAbility.RAGE)
                .withWeapon("minecraft:netherite_axe"));

        // ------------------------------------------------------- basalt deltas
        add("basalt_deltas",
            TowerBossDef.of("magma_cube_elder", "Магмовый куб", "minecraft:magma_cube", 1, 1.4D, 1.1D,
                TowerAbility.FIRE_NOVA),
            TowerBossDef.of("flame_vortex", "Пламенный вихрь", "minecraft:blaze", 2, 1.8D, 1.4D,
                TowerAbility.FIRE_NOVA, TowerAbility.METEOR),
            TowerBossDef.of("basalt_sentinel", "Базальтовый страж", "minecraft:piglin_brute", 3, 2.5D, 1.6D,
                TowerAbility.SHIELD, TowerAbility.KNOCKBACK_SLAM).withWeapon("minecraft:netherite_axe"),
            TowerBossDef.of("incinerator", "Испепелитель", "minecraft:blaze", 3, 2.3D, 1.7D,
                TowerAbility.METEOR, TowerAbility.SUMMON_MINIONS),
            TowerBossDef.of("delta_titan", "Гигант дельт", "minecraft:piglin_brute", 4, 3.8D, 2.0D,
                TowerAbility.KNOCKBACK_SLAM, TowerAbility.METEOR, TowerAbility.RAGE,
                TowerAbility.SHIELD, TowerAbility.SUMMON_ELITE)
                .withWeapon("minecraft:netherite_axe"));
    }

    private TowerRoster() {
    }

    private static void add(String theme, TowerBossDef... defs) {
        BY_THEME.put(theme, List.of(defs));
    }

    public static int size() {
        int total = 0;
        for (List<TowerBossDef> list : BY_THEME.values()) {
            total += list.size();
        }
        return total;
    }

    public static List<TowerBossDef> all(String themeId) {
        return BY_THEME.getOrDefault(themeId, BY_THEME.get(TowerThemes.PLAINS.id()));
    }

    /** Strongest boss the given rank unlocks. */
    public static TowerBossDef mainBoss(String themeId, int rank, RandomSource random) {
        List<TowerBossDef> defs = all(themeId);
        int best = 0;
        for (TowerBossDef def : defs) {
            if (def.rank() <= rank) {
                best = Math.max(best, def.rank());
            }
        }
        if (best == 0) {
            return defs.get(0);
        }
        List<TowerBossDef> candidates = new ArrayList<>();
        for (TowerBossDef def : defs) {
            if (def.rank() == best) {
                candidates.add(def);
            }
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    /** Distinct bosses that guard the main boss (always weaker than it). */
    public static List<TowerBossDef> champions(String themeId, int rank, int count, TowerBossDef main, RandomSource random) {
        int ceiling = Math.max(2, rank);
        List<TowerBossDef> pool = new ArrayList<>();
        for (TowerBossDef def : all(themeId)) {
            if (!def.id().equals(main.id()) && def.rank() <= ceiling) {
                pool.add(def);
            }
        }
        if (pool.isEmpty()) {
            for (TowerBossDef def : all(themeId)) {
                if (!def.id().equals(main.id())) {
                    pool.add(def);
                }
            }
        }
        List<TowerBossDef> picked = new ArrayList<>();
        java.util.Collections.shuffle(pool, new java.util.Random(random.nextLong()));
        for (int i = 0; i < count && i < pool.size(); i++) {
            picked.add(pool.get(i));
        }
        return picked;
    }

    public static String subtitle(TowerBossDef def) {
        StringBuilder builder = new StringBuilder();
        for (TowerAbility ability : def.abilities()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(ability.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
        }
        return builder.toString();
    }

    static {
        // fail fast on duplicate ids while the data is still hand written
        Map<String, String> seen = new HashMap<>();
        for (Map.Entry<String, List<TowerBossDef>> entry : BY_THEME.entrySet()) {
            for (TowerBossDef def : entry.getValue()) {
                String previous = seen.put(def.id(), entry.getKey());
                if (previous != null) {
                    throw new IllegalStateException("duplicate tower boss id " + def.id());
                }
            }
        }
    }
}
