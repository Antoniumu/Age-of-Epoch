package com.robertx22.age_of_exile.aoe_data.database.perks;

import com.robertx22.age_of_exile.aoe_data.database.stats.old.DatapackStats;
import com.robertx22.age_of_exile.database.OptScaleExactStat;
import com.robertx22.age_of_exile.database.data.perks.Perk;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.enumclasses.ModType;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.resources.Identifier;

public class StartPerks implements ExileRegistryInit {

    public static String MAGE = "mage";
    public static String WARRIOR = "warrior";
    public static String HUNTER = "hunter";
    public static String TEMPLAR = "templar";
    public static String DUELIST = "duelist";
    public static String BATTLE_MAGE = "battle_mage";
    public static String SCION = "scion";

    @Override
    public void registerAll() {

        of(MAGE, "Mage", "mage",
            new OptScaleExactStat(30, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(15, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(15, DatapackStats.DEX, ModType.FLAT)
        );
        of(WARRIOR, "Warrior", "warrior",
            new OptScaleExactStat(15, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(30, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(15, DatapackStats.DEX, ModType.FLAT)
        );
        of(HUNTER, "Hunter", "hunter",
            new OptScaleExactStat(15, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(15, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(30, DatapackStats.DEX, ModType.FLAT)
        );

        of(TEMPLAR, "Templar", "templar",
            new OptScaleExactStat(23, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(23, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(14, DatapackStats.DEX, ModType.FLAT)
        );

        of(DUELIST, "Duelist", "duelist",
            new OptScaleExactStat(14, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(23, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(23, DatapackStats.DEX, ModType.FLAT)
        );

        of(BATTLE_MAGE, "Battle Mage", "scion",
            new OptScaleExactStat(23, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(14, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(23, DatapackStats.DEX, ModType.FLAT)
        );

        of(SCION, "Scion", "scion",
            new OptScaleExactStat(20, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(20, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(20, DatapackStats.DEX, ModType.FLAT)
        );

        // Стартовые узлы древа талантов (data/mmorpg/talent_tree/talents.json)
        of("start_int", "Путь мага", "mage",
            new OptScaleExactStat(30, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(15, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(15, DatapackStats.DEX, ModType.FLAT)
        );
        of("start_str", "Путь воина", "warrior",
            new OptScaleExactStat(15, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(30, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(15, DatapackStats.DEX, ModType.FLAT)
        );
        of("start_dex", "Путь охотника", "hunter",
            new OptScaleExactStat(15, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(15, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(30, DatapackStats.DEX, ModType.FLAT)
        );
        of("start_strint", "Путь храмовника", "templar",
            new OptScaleExactStat(23, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(23, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(14, DatapackStats.DEX, ModType.FLAT)
        );
        of("start_strdex", "Путь дуэлянта", "duelist",
            new OptScaleExactStat(14, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(23, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(23, DatapackStats.DEX, ModType.FLAT)
        );
        of("start_intdex", "Путь боевого мага", "scion",
            new OptScaleExactStat(23, DatapackStats.INT, ModType.FLAT),
            new OptScaleExactStat(14, DatapackStats.STR, ModType.FLAT),
            new OptScaleExactStat(23, DatapackStats.DEX, ModType.FLAT)
        );
    }

    Perk of(String id, String locname, String iconName, OptScaleExactStat... stats) {
        Perk perk = PerkBuilder.bigStat(id, locname, stats);
        perk.is_entry = true;
        perk.type = Perk.PerkType.START;
        perk.one_of_a_kind = "start";
        perk.icon = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/talent_icons/" + iconName + ".png").toString();
        return perk;
    }
}
