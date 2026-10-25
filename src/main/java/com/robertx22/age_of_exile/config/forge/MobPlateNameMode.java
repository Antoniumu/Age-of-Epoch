package com.robertx22.age_of_exile.config.forge;

/**
 * How the {@code MODERN} / {@code RUNE} panels behave on entities that already
 * have a vanilla Minecraft name tag (players, and mobs renamed with a name tag).
 *
 * <p>Vanilla attaches a name tag at {@code bbHeight + 0.5}. The panels used to
 * grow down to {@code bbHeight + 0.04}, so their opaque body sat right on top of
 * that text. The classic style never had this problem: it is two thin text lines
 * stacked above the head.</p>
 *
 * <p>Selectable from the client config ({@code named_mode} in
 * {@code config/age_of_epoch/mob_plates.properties}).</p>
 */
public enum MobPlateNameMode {

    /**
     * Named entities get their own compact banner (level badge + HP bar + HP
     * numbers, but no duplicated name) lifted above the vanilla name tag. Mobs
     * without a vanilla name keep the usual full panel right above the head.
     */
    COMPACT("Компактная", "Над ванильным ником — узкая плашка без имени: уровень, полоса и числа HP."),

    /**
     * Same lift, but the panel keeps its own name row. The vanilla nickname is
     * still fully visible underneath, at the cost of seeing the name twice.
     */
    FULL("Полная", "Над ванильным ником — обычная плашка со своим именем, уровнем и HP."),

    /**
     * Old behaviour: ignore the vanilla name tag and draw the full panel right
     * above the head (it can overlap the nickname again).
     */
    IGNORE("Не учитывать", "Как раньше: плашка у головы, ванильный ник может перекрываться.");

    public final String displayName;
    public final String description;

    MobPlateNameMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean liftsAboveNameTag() {
        return this != IGNORE;
    }

    public MobPlateNameMode next() {
        MobPlateNameMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static MobPlateNameMode byName(String name) {
        if (name != null) {
            for (MobPlateNameMode mode : values()) {
                if (mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
        }
        return COMPACT;
    }
}
