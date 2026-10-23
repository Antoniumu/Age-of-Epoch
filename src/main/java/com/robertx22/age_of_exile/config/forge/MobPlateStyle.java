package com.robertx22.age_of_exile.config.forge;

/**
 * Visual styles for the nameplates rendered above mobs.
 *
 * <p>Selectable from the client config ({@code mob_plate_style}), and cycled in game
 * with the "Mob Plate Style" keybind (J by default).</p>
 */
public enum MobPlateStyle {

    /**
     * The original Age of Exile look: two name-tag lines (level + name, then a
     * pipe-based health bar). Nothing is drawn behind the text.
     */
    CLASSIC("Классическая", "Две строки текста и полоса здоровья из символов, как в оригинале."),

    /**
     * Dark glass plate with a rarity-tinted border, a compact level badge, a
     * smooth gradient health bar and a small pointer towards the mob.
     */
    MODERN("Современная", "Тёмная стеклянная плашка с бейджем уровня и плавной полосой здоровья."),

    /**
     * Ornate fantasy plate: bronze trim, gem badge and a segmented health bar.
     */
    RUNE("Руническая", "Плашка с золотой огранкой, самоцветом уровня и сегментированной полосой.");

    public final String displayName;
    public final String description;

    MobPlateStyle(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public MobPlateStyle next() {
        MobPlateStyle[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static MobPlateStyle byName(String name) {
        if (name != null) {
            for (MobPlateStyle style : values()) {
                if (style.name().equalsIgnoreCase(name)) {
                    return style;
                }
            }
        }
        return MODERN;
    }
}
