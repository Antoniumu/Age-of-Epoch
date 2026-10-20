package com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers;

// same type modifiers are upgraded to higher rank ones and replaced ( so you don't put both iron AND dimaond armor on mobs)
public class ModifierType {

    private static String CHAOTIC_TAG = "chaotic";

    public static ModifierType gearedMobs(int rank) {
        return new ModifierType("geared_mobs", rank).setCanOnlyHaveOne();
    }

    public static ModifierType tntDeathMobs(int rank) {
        return new ModifierType(CHAOTIC_TAG, rank).setCanOnlyHaveOne();
    }

    public static ModifierType fasterMobSpawning() {
        return new ModifierType(CHAOTIC_TAG, 3).setCanOnlyHaveOne();
    }

    public static ModifierType meleeSkeletons(int rank) {
        return new ModifierType("melee_skeletons", rank).setCanOnlyHaveOne();
    }

    public static ModifierType enchantedMobGear(int rank) {
        return new ModifierType("enchanted_mob_gear", rank).setCanOnlyHaveOne();
    }

    public static ModifierType mobTickPotion(int rank) {
        return new ModifierType("mob_tick_potion", rank).setCanOnlyHaveOne();
    }

    private String type;
    public int rank;
    boolean canOnlyHaveOne = false;

    private ModifierType setCanOnlyHaveOne() {
        this.canOnlyHaveOne = true;
        return this;
    }

    public boolean isSameType(ModifierType other) {
        return !this.type.isEmpty() && !other.type.isEmpty() && other.type.equals(type);
    }

    public boolean canOnlyHaveOne(ModifierType other) {
        return canOnlyHaveOne && !this.type.isEmpty() && !other.type.isEmpty() && other.type.equals(type);
    }

    private ModifierType(String type, int rank) {
        this.type = type;
        this.rank = rank;
    }
}
