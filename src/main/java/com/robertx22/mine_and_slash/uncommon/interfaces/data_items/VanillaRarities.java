package com.robertx22.mine_and_slash.uncommon.interfaces.data_items;

import net.minecraft.world.item.Rarity;
import java.util.HashMap;

public class VanillaRarities {
    public static Rarity COMMON_ITEM = Rarity.COMMON;
    public static Rarity UNCOMMON_ITEM = Rarity.UNCOMMON;
    public static Rarity RARE_ITEM = Rarity.RARE;
    public static Rarity EPIC_ITEM = Rarity.EPIC;
    public static Rarity LEGENDARY_ITEM = Rarity.EPIC;
    public static Rarity MYTHIC_ITEM = Rarity.EPIC;
    public static Rarity RUNED_ITEM = Rarity.RARE;
    public static Rarity UNIQUE_ITEM = Rarity.EPIC;

    public static HashMap<String, Rarity> MAP = new HashMap<>();
    static {
        MAP.put("common", COMMON_ITEM);
        MAP.put("uncommon", UNCOMMON_ITEM);
        MAP.put("rare", RARE_ITEM);
        MAP.put("epic", EPIC_ITEM);
        MAP.put("legendary", LEGENDARY_ITEM);
        MAP.put("mythic", MYTHIC_ITEM);
        MAP.put("runed", RUNED_ITEM);
        MAP.put("unique", UNIQUE_ITEM);
    }

    public static void init() {}
}
