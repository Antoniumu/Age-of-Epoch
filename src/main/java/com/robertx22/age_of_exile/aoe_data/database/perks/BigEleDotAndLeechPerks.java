package com.robertx22.age_of_exile.aoe_data.database.perks;

import com.robertx22.age_of_exile.aoe_data.database.stats.Stats;
import com.robertx22.age_of_exile.aoe_data.database.stats.base.LeechInfo;
import com.robertx22.age_of_exile.database.OptScaleExactStat;
import com.robertx22.age_of_exile.database.data.stats.types.generated.ElementalPenetration;
import com.robertx22.age_of_exile.saveclasses.unit.ResourceType;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import com.robertx22.age_of_exile.uncommon.enumclasses.ModType;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

public class BigEleDotAndLeechPerks implements ExileRegistryInit {

    static void breath(String locname, Elements ele) {

        PerkBuilder.bigStat("big_breath_of_" + ele.guidName, locname,
            new OptScaleExactStat(5, Stats.ELEMENTAL_DAMAGE.get(ele), ModType.FLAT),
            new OptScaleExactStat(15, Stats.ELE_DOT_DAMAGE.get(ele), ModType.FLAT)
        );
    }

    static void heart(String locname, Elements ele) {
        PerkBuilder.bigStat("big_heart_of_" + ele.guidName, locname,
            new OptScaleExactStat(10, Stats.ELEMENTAL_SPELL_DAMAGE.get(ele), ModType.FLAT),
            new OptScaleExactStat(2, new ElementalPenetration(ele), ModType.FLAT),
            new OptScaleExactStat(2, Stats.ELEMENT_LEECH_RESOURCE.get(new LeechInfo(ele, ResourceType.health)), ModType.FLAT)
        );
    }

    static void dominator(String locname, Elements ele) {
        PerkBuilder.bigStat(ele.guidName + "_dom", locname,
            new OptScaleExactStat(5, new ElementalPenetration(ele), ModType.FLAT),
            new OptScaleExactStat(10, Stats.CRIT_DAMAGE.get(), ModType.FLAT),
            new OptScaleExactStat(2, Stats.ELEMENT_LEECH_RESOURCE.get(new LeechInfo(ele, ResourceType.health)))
        );

    }

    @Override
    public void registerAll() {
        // Раньше этот метод был пустым: девять талантов (дыхание / сердце /
        // повелитель для трёх стихий) описывались хелперами выше, но никогда
        // не регистрировались, то есть в древе их не существовало.

        breath("Дыхание пламени", Elements.Fire);
        breath("Дыхание прилива", Elements.Water);
        breath("Дыхание природы", Elements.Earth);

        heart("Сердце пламени", Elements.Fire);
        heart("Сердце прилива", Elements.Water);
        heart("Сердце природы", Elements.Earth);

        dominator("Повелитель огня", Elements.Fire);
        dominator("Повелитель воды", Elements.Water);
        dominator("Повелитель земли", Elements.Earth);
    }
}
