package com.robertx22.world_of_exile.tower;

import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of every tower theme.  The ids and mob pools are mirrored by
 * {@code tools/tower_gen/themes.py}; keep both lists in sync when adding a
 * biome.
 */
public final class TowerThemes {

    private static final Map<String, TowerTheme> THEMES = new LinkedHashMap<>();

    public static final TowerTheme PLAINS;

    static {
        PLAINS = add("plains", "Равнинная крепость", 1, false, "minecraft:stone_bricks",
            List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:pillager", "minecraft:vindicator"));

        add("forest", "Лесной дозор", 2, false, "minecraft:mossy_cobblestone",
            List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:vindicator"));

        add("ice", "Ледяной шпиль", 2, false, "minecraft:packed_ice",
            List.of("minecraft:stray", "minecraft:skeleton", "minecraft:zombie", "minecraft:cave_spider"));

        add("desert", "Песчаный мавзолей", 2, false, "minecraft:sandstone",
            List.of("minecraft:husk", "minecraft:skeleton", "minecraft:cave_spider", "minecraft:pillager"));

        add("jungle", "Храм джунглей", 3, false, "minecraft:mossy_stone_bricks",
            List.of("minecraft:cave_spider", "minecraft:spider", "minecraft:skeleton", "minecraft:witch"));

        add("swamp", "Болотный склеп", 2, false, "minecraft:mud_bricks",
            List.of("minecraft:witch", "minecraft:slime", "minecraft:drowned", "minecraft:cave_spider"));

        add("badlands", "Каньонная цитадель", 3, false, "minecraft:red_sandstone",
            List.of("minecraft:husk", "minecraft:pillager", "minecraft:vindicator", "minecraft:cave_spider"));

        add("savanna", "Саванный форт", 2, false, "minecraft:acacia_planks",
            List.of("minecraft:pillager", "minecraft:vindicator", "minecraft:husk", "minecraft:skeleton"));

        add("mountain", "Горный бастион", 3, false, "minecraft:stone",
            List.of("minecraft:skeleton", "minecraft:zombie", "minecraft:vindicator", "minecraft:cave_spider"));

        add("coast", "Затонувший шпиль", 2, false, "minecraft:prismarine",
            List.of("minecraft:drowned", "minecraft:guardian", "minecraft:skeleton", "minecraft:cave_spider"));

        add("mushroom", "Споровый шпиль", 2, false, "minecraft:mushroom_stem",
            List.of("minecraft:spider", "minecraft:cave_spider", "minecraft:zombie", "minecraft:witch"));

        add("dark_forest", "Тёмное святилище", 4, false, "minecraft:deepslate_bricks",
            List.of("minecraft:vindicator", "minecraft:evoker", "minecraft:vex", "minecraft:witch"));

        add("nether_wastes", "Чернокаменная цитадель", 4, true, "minecraft:polished_blackstone_bricks",
            List.of("minecraft:wither_skeleton", "minecraft:piglin", "minecraft:blaze", "minecraft:magma_cube"));

        add("soul_sand_valley", "Долина душ", 4, true, "minecraft:soul_sand",
            List.of("minecraft:wither_skeleton", "minecraft:skeleton", "minecraft:ghast", "minecraft:piglin"));

        add("crimson_forest", "Багровый чертог", 4, true, "minecraft:nether_wart_block",
            List.of("minecraft:piglin", "minecraft:hoglin", "minecraft:zoglin", "minecraft:magma_cube"));

        add("basalt_deltas", "Базальтовый гигант", 4, true, "minecraft:blackstone",
            List.of("minecraft:magma_cube", "minecraft:blaze", "minecraft:piglin_brute", "minecraft:wither_skeleton"));
    }

    private TowerThemes() {
    }

    private static TowerTheme add(String id, String name, int baseRank, boolean nether, String foundation, List<String> mobs) {
        TowerTheme theme = new TowerTheme(id, name, baseRank, nether, foundation, mobs);
        THEMES.put(id, theme);
        return theme;
    }

    public static TowerTheme byId(String id) {
        if (id != null) {
            TowerTheme theme = THEMES.get(id);
            if (theme != null) {
                return theme;
            }
        }
        return PLAINS;
    }

    public static boolean exists(String id) {
        return id != null && THEMES.containsKey(id);
    }

    /** Fallback when a structure carries no theme tag. */
    public static TowerTheme forDimension(Level level) {
        if (Level.NETHER.equals(level.dimension())) {
            return byId("nether_wastes");
        }
        return PLAINS;
    }

    public static Collection<TowerTheme> all() {
        return THEMES.values();
    }
}
