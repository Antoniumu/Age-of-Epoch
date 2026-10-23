package com.robertx22.age_of_exile.mmorpg.registers.common.items;

import com.robertx22.age_of_exile.database.data.currency.*;
import com.robertx22.age_of_exile.database.data.currency.base.CurrencyItem;
import com.robertx22.age_of_exile.database.data.currency.safes.EpicSafeScroll;
import com.robertx22.age_of_exile.database.data.currency.safes.LegendarySafeScroll;
import com.robertx22.age_of_exile.database.data.currency.upgrades.PlusOneUpgradeItem;
import com.robertx22.age_of_exile.database.data.currency.upgrades.PlusThreeUpgradeItem;
import com.robertx22.age_of_exile.database.data.currency.upgrades.PlusTwoUpgradeItem;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.forge_compat.registries.ForgeRegistries;

import java.util.List;
import java.util.stream.Collectors;

public class CurrencyItems {

    public static void init() {

    }

    public static List<CurrencyItem> getAllCurrenciesFromRegistry() {
        return ForgeRegistries.ITEMS.stream()
            .filter(x -> x instanceof CurrencyItem)
            .map(x -> (CurrencyItem) x)
            .collect(Collectors.toList());
    }

    public static RegObj<CurrencyItem> ORB_OF_TRANSMUTATION = Def.item("currency/orb_of_transmutation", () -> new OrbOfTransmutationItem());
    public static RegObj<CurrencyItem> CLEAR_CORRUPTION = Def.item("currency/clear_corruption", () -> new PurifyItem());
    public static RegObj<CurrencyItem> CRYSTAL_OF_PURIFICATION = Def.item("currency/crystal_of_purification", () -> new CrystalOfPurificationItem());
    public static RegObj<CurrencyItem> ORB_OF_DISORDER = Def.item("currency/randomize_affixes", () -> new OrbOfDisorder());
    public static RegObj<CurrencyItem> ORB_OF_TURBULENCE = Def.item("currency/reroll_affix_numbers", () -> new OrbOfTurbulence());
    public static RegObj<CurrencyItem> STONE_OF_HOPE = Def.item("currency/stone_of_hope", () -> new StoneOfHopeItem());
    public static RegObj<CurrencyItem> LEAF_OF_CHANGE = Def.item("currency/reroll_primary_stats_numbers", () -> new LeafOfChangeItem());
    public static RegObj<CurrencyItem> ORB_OF_BLESSING = Def.item("currency/number_reroll", () -> new OrbOfBlessingItem());
    public static RegObj<CurrencyItem> ORB_OF_UNIQUE_BLESSING = Def.item("currency/reroll_unique_numbers", () -> new OrbOfUniqueBlessingItem());
    public static RegObj<CurrencyItem> CLEAR_INSTABILITY = Def.item("currency/clear_instability", () -> new ClearInstabilityItem());
    public static RegObj<CurrencyItem> ORB_OF_CORRUPTION = Def.item("currency/orb_of_corruption", () -> new OrbOfCorruption());
    public static RegObj<CurrencyItem> ORB_OF_INFINITY = Def.item("currency/orb_of_infinity", () -> new OrbOfInfinityItem());

    public static RegObj<CurrencyItem> PLUS_ONE_UPGRADE = Def.item("currency/plus_one_upgrade", () -> new PlusOneUpgradeItem());
    public static RegObj<CurrencyItem> PLUS_TWO_UPGRADE = Def.item("currency/plus_two_upgrade", () -> new PlusTwoUpgradeItem());
    public static RegObj<CurrencyItem> PLUS_THREE_UPGRADE = Def.item("currency/plus_three_upgrade", () -> new PlusThreeUpgradeItem());

    public static RegObj<CurrencyItem> EPIC_PROT_SCROLL = Def.item("currency/epic_safe_scroll", () -> new EpicSafeScroll());
    public static RegObj<CurrencyItem> LEGENDARY_PROT_SCROLL = Def.item("currency/legendary_safe_scroll", () -> new LegendarySafeScroll());

}
