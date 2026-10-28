package com.robertx22.age_of_exile.uncommon.testing;

import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.stat_soul.SoullessGear;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Проверка «бесдушных основ» в живом мире.
 *
 * <p>Как и боевой зонд, это не юнит-тест: путь «предмет попал в игру → получил
 * основу → на него наложили душу» проходит через {@code GearSlot} (реестр слотов),
 * {@code ExileDB.GearTypes()} (датапак), {@code BaseStatsData} и NBT-сохранение
 * {@code Gear.Save/Gear.Load}. Вне запущенного клиента с загруженными датапаками
 * его не проверить.
 *
 * <p>Результат — строки {@code AOETEST soulless ...} и {@code AOETEST
 * soulless_soul ...} в логе (их разбирает CI-сценарий), а ещё предметы в хотбаре:
 * сценарий открывает инвентарь и снимает тултипы, чтобы «бесдушная основа» была
 * видна глазами.
 */
public final class SoullessProbe {

    private SoullessProbe() {
    }

    /** Слоты хотбара, куда зонд кладёт демонстрационные предметы для скриншотов. */
    public static final int SLOT_BASE_CHEAP = 0;
    public static final int SLOT_BASE_RICH = 1;
    public static final int SLOT_SOULED = 2;
    public static final int SLOT_SOUL = 3;
    public static final int SLOT_WHOLE = 4;

    /**
     * Прогоняет все проверки за один серверный тик.
     *
     * @return строки отчёта (для {@code CombatProbe}, который пишет их в лог)
     */
    public static void run(ServerPlayer player, List<String> out, int[] counters) {
        try {
            oneRun(player, out, counters);
        } catch (Throwable error) {
            out.add(CombatProbe.TAG + " soulless fatal=" + error);
            counters[0]++;
            counters[1]++;
        }
    }

    private static void oneRun(ServerPlayer player, List<String> out, int[] counters) {

        // ---- 1. Бесдушная основа появляется сама и она слабая ----
        ItemStack cheap = new ItemStack(Items.WOODEN_SWORD);
        ItemStack rich = new ItemStack(Items.NETHERITE_SWORD);

        int cheapRank = SoullessGear.materialRank(cheap);
        int richRank = SoullessGear.materialRank(rich);

        boolean cheapMade = SoullessGear.ensure(cheap, player);
        boolean richMade = SoullessGear.ensure(rich, player);

        GearItemData cheapData = Gear.Load(cheap);
        GearItemData richData = Gear.Load(rich);

        int cheapPerc = sumPerc(cheapData);
        int richPerc = sumPerc(richData);

        Report base = new Report("soulless");
        base.put("cheap_rank", cheapRank);
        base.put("rich_rank", richRank);
        base.put("cheap_bq", cheapData == null ? -1F : cheapData.bq);
        base.put("rich_bq", richData == null ? -1F : richData.bq);
        base.put("cheap_pct", SoullessGear.basePercent(SoullessGear.costFactor(cheap)));
        base.put("rich_pct", SoullessGear.basePercent(SoullessGear.costFactor(rich)));
        base.check("cheap_created", cheapMade && cheapData != null && cheapData.isSoulless());
        base.check("rich_created", richMade && richData != null && richData.isSoulless());
        base.check("rank_order", cheapRank < richRank);
        base.check("cheap_is_common", cheapData != null && "common".equals(cheapData.rarity));
        base.check("no_affixes", cheapData != null && cheapData.affixes.getNumberOfAffixes() == 0);
        // Референс: та же база, но с полными (не урезанными) статами —
        // так проверяется само «ослабление» основы, а не только формула.
        int normalPerc = normalBasePerc(richData);
        base.put("normal_perc", normalPerc);
        base.check("quality_set", cheapData != null && richData != null
            && Math.abs(cheapData.bq - 0.12F) < 0.01F && Math.abs(richData.bq - 0.45F) < 0.01F);
        base.check("rolls_not_crippled", cheapPerc > 0 && richPerc > 0
            && cheapPerc > normalPerc * 0.3D);
        // Роллы больше не урезаются — «слабость» основы задаёт множитель bq,
        // поэтому проверяем именно его: дешёвая основа 12 %, дорогая 45 %.
        base.check("plain_name", cheapData != null && plainNameMatches(cheap, cheapData));
        out.add(base.line());
        counters[0] += base.checks;
        counters[1] += base.failed;

        // ---- 1b. Разница видна не только в процентах, но и в числах ----
        // На 1 уровне базовые числа предмета крошечные (2–4 урона), поэтому
        // 12 % и 45 % округляются в одни и те же «1». Сравниваем на реальном
        // уровне игры: статы пересчитываются через ExactStatData, как в бою.
        int lvlCompare = 20;
        GearItemData cheapMid = SoullessGear.create(cheapData.GetBaseGearType(), lvlCompare,
            SoullessGear.costFactor(cheap));
        GearItemData richMid = SoullessGear.create(richData.GetBaseGearType(), lvlCompare,
            SoullessGear.costFactor(rich));

        float cheapValue = totalBaseValue(cheapMid);
        float richValue = totalBaseValue(richMid);

        // Эталон: тот же дорогой предмет, но без ослабления (bq = 1).
        GearItemData normalMid = SoullessGear.create(richData.GetBaseGearType(), lvlCompare,
            SoullessGear.costFactor(rich));
        normalMid.bq = 1F;
        float normalValue = totalBaseValue(normalMid);

        Report scaling = new Report("soulless_scaling");
        scaling.put("lvl", lvlCompare);
        scaling.put("cheap_value", String.format(java.util.Locale.ROOT, "%.1f", cheapValue));
        scaling.put("rich_value", String.format(java.util.Locale.ROOT, "%.1f", richValue));
        scaling.put("ratio", String.format(java.util.Locale.ROOT, "%.2f",
            richValue / Math.max(0.01F, cheapValue)));
        scaling.put("normal_value", String.format(java.util.Locale.ROOT, "%.1f", normalValue));
        scaling.check("below_normal", normalValue > 0 && richValue < normalValue * 0.6F);
        scaling.check("rich_higher_in_numbers", richValue > cheapValue);
        // 45 % против 12 % — разница должна быть заметно больше, чем «на волосок».
        scaling.check("difference_visible", richValue > cheapValue * 1.5F);
        out.add(scaling.line());
        counters[0] += scaling.checks;
        counters[1] += scaling.failed;

        // ---- 2. Душа накладывается поверх: редкость/имя от души, база от предмета ----
        String typeBefore = cheapData == null ? "" : cheapData.gear_type;
        List<Integer> percBefore = cheapData == null ? List.of() : List.copyOf(cheapData.baseStats.perc);

        StatSoulData soul = new StatSoulData();
        soul.slot = "sword";
        soul.rar = "rare";
        soul.tier = com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils.levelToTier(20);
        ItemStack soulStack = soul.toStack();

        ItemStack souled = cheap.copy();
        StatSoulData loaded = com.robertx22.age_of_exile.uncommon.datasaving.StackSaving.STAT_SOULS.loadFrom(soulStack);
        boolean inserted = false;
        if (loaded != null && loaded.canInsertIntoStack(souled)) {
            loaded.insertAsUnidentifiedOn(souled, player);
            inserted = true;
        }

        GearItemData souledData = Gear.Load(souled);
        String typeAfter = souledData == null ? "" : souledData.gear_type;
        String rarityAfter = souledData == null ? "" : souledData.rarity;
        boolean percKept = souledData != null && souledData.baseStats != null
            && souledData.baseStats.perc.equals(percBefore);
        boolean soulConsumed = !com.robertx22.age_of_exile.uncommon.datasaving.StackSaving.STAT_SOULS.has(souled);

        // Регресс: душа низкого тира не должна «омолаживать» предмет и обнулять его статы.
        GearItemData levelBase = SoullessGear.create(cheapData.GetBaseGearType(), 20,
            SoullessGear.costFactor(cheap));
        StatSoulData lowSoul = new StatSoulData();
        lowSoul.slot = "sword";
        lowSoul.rar = "common";
        lowSoul.tier = 0;
        GearItemData lowSoulData = lowSoul.createGearData(null, player);
        SoullessGear.applySoul(lowSoulData, levelBase);
        int lvlAfterLowSoul = lowSoulData.lvl;
        float valueAfterLowSoul = totalBaseValue(lowSoulData);

        Report soulReport = new Report("soulless_soul");
        soulReport.put("lvl_after_low_soul", lvlAfterLowSoul);
        soulReport.put("value_after_low_soul", String.format(java.util.Locale.ROOT, "%.1f", valueAfterLowSoul));
        soulReport.check("low_soul_keeps_level", lvlAfterLowSoul >= 20);
        soulReport.check("low_soul_keeps_stats", valueAfterLowSoul > 0F);
        soulReport.put("rarity_after", rarityAfter);
        soulReport.put("affixes_after", souledData == null ? -1 : souledData.affixes.getNumberOfAffixes());
        soulReport.put("type_kept", typeBefore.equals(typeAfter));
        soulReport.check("inserted", inserted);
        soulReport.check("no_longer_soulless", souledData != null && !souledData.isSoulless());
        soulReport.check("rarity_from_soul", "rare".equals(rarityAfter));
        soulReport.check("got_affixes", souledData != null && souledData.affixes.getNumberOfAffixes() > 0);
        soulReport.check("base_stats_kept", percKept);
        soulReport.check("same_item_type", typeBefore.equals(typeAfter));
        soulReport.check("soul_tag_removed", soulConsumed);
        soulReport.check("name_changed", souledData != null && !plainNameMatches(souled, souledData));
        // Душа завершает предмет: слабость основы (12–45 %) снимается.
        soulReport.check("soul_restores_full_base", souledData != null
            && Math.abs(souledData.bq - 1F) < 0.001F);
        out.add(soulReport.line());
        counters[0] += soulReport.checks;
        counters[1] += soulReport.failed;

        // ---- 3. «Цельные» основы: булава/трезубец/элитры — весь предмет и есть основа ----
        ItemStack trident = new ItemStack(Items.TRIDENT);
        boolean tridentMade = SoullessGear.ensure(trident, player);
        GearItemData tridentData = Gear.Load(trident);

        ItemStack elytra = new ItemStack(Items.ELYTRA);
        boolean elytraMade = SoullessGear.ensure(elytra, player);
        GearItemData elytraData = Gear.Load(elytra);

        float bestNormalBq = Math.max(cheapData == null ? 0F : cheapData.bq,
            richData == null ? 0F : richData.bq);

        Report whole = new Report("soulless_whole");
        whole.put("trident_slot", SoullessGear.slotIdFor(trident));
        whole.put("elytra_slot", SoullessGear.slotIdFor(elytra));
        whole.put("trident_bq", tridentData == null ? -1F : tridentData.bq);
        whole.put("elytra_bq", elytraData == null ? -1F : elytraData.bq);
        whole.put("best_normal_bq", bestNormalBq);
        whole.check("trident_created", tridentMade && tridentData != null && tridentData.isSoulless());
        whole.check("trident_whole_item", tridentData != null && tridentData.isWholeItemBase());
        whole.check("trident_stronger_than_normal", tridentData != null && tridentData.bq > bestNormalBq);
        whole.check("trident_weaker_than_soul", tridentData != null && tridentData.bq < 1F);
        whole.check("elytra_created", elytraMade && elytraData != null && elytraData.isSoulless());
        whole.check("elytra_whole_item", elytraData != null && elytraData.isWholeItemBase());
        whole.check("elytra_stronger_than_normal", elytraData != null && elytraData.bq > bestNormalBq);
        whole.check("whole_is_weaker_than_soul", elytraData != null && elytraData.bq < 1F);
        out.add(whole.line());
        counters[0] += whole.checks;
        counters[1] += whole.failed;

        // ---- 3b. «Пустышка» без основы не работает; не-гир не трогаем ----
        ItemStack apple = new ItemStack(Items.APPLE);
        boolean appleTouched = SoullessGear.ensure(apple, player);

        ItemStack bareWeapon = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack basedWeapon = new ItemStack(Items.DIAMOND_SWORD);
        SoullessGear.ensure(basedWeapon, player);

        Report guards = new Report("soulless_guards");
        guards.check("non_gear_untouched", !appleTouched && !Gear.has(apple));
        guards.check("bare_weapon_needs_base", SoullessGear.needsBase(bareWeapon));
        guards.check("bare_weapon_unusable", SoullessGear.isUnusableWeapon(bareWeapon));
        guards.check("based_weapon_usable", !SoullessGear.isUnusableWeapon(basedWeapon)
            && SoullessGear.hasBase(basedWeapon));
        guards.check("whole_item_fallback_usable", !SoullessGear.isUnusableWeapon(new ItemStack(Items.TRIDENT)));
        guards.check("weapon_like_sword", SoullessGear.isWeaponLike(new ItemStack(Items.IRON_SWORD)));
        guards.check("weapon_like_apple_false", !SoullessGear.isWeaponLike(apple));
        guards.check("slot_for_vanilla_sword", "sword".equals(SoullessGear.slotIdFor(new ItemStack(Items.IRON_SWORD))));
        guards.check("slot_for_apple_null", SoullessGear.slotIdFor(apple) == null);

        // «Мёртвый» предмет (данные есть, базового типа нет) обязан быть бесполезен:
        // никаких статов и никакого урона. Это страховка правила «ничего без основы».
        ItemStack deadStack = new ItemStack(Items.IRON_SWORD);
        GearItemData deadData = new GearItemData();
        deadData.gear_type = "does_not_exist";
        deadData.lvl = 10;
        deadData.sl = true;
        Gear.Save(deadStack, deadData);
        GearItemData deadLoaded = Gear.Load(deadStack);
        com.robertx22.age_of_exile.capability.entity.EntityData playerData =
            com.robertx22.age_of_exile.uncommon.datasaving.Load.Unit(player);

        guards.check("dead_item_detected", deadLoaded != null && deadLoaded.isDeadItem());
        guards.check("dead_item_no_stats", deadLoaded != null && deadLoaded.GetAllStats()
            .isEmpty());
        guards.check("dead_item_unusable", playerData != null && !playerData.canUseWeapon(deadLoaded));
        guards.check("soulless_base_usable", playerData != null && cheapData != null
            && playerData.canUseWeapon(cheapData));
        guards.check("whole_base_usable", playerData != null && tridentData != null
            && playerData.canUseWeapon(tridentData));
        out.add(guards.line());
        counters[0] += guards.checks;
        counters[1] += guards.failed;

        // ---- 3c. Повторная выдача не дублируется, битые данные чинятся ----
        float tridentBefore = tridentData == null ? -1F : tridentData.bq;
        boolean secondEnsure = SoullessGear.ensure(trident, player);
        GearItemData tridentAfter = Gear.Load(trident);

        ItemStack broken = new ItemStack(Items.IRON_SWORD);
        GearItemData brokenData = new GearItemData();
        brokenData.gear_type = "does_not_exist";
        brokenData.lvl = 20;
        Gear.Save(broken, brokenData);
        boolean repaired = SoullessGear.ensure(broken, player);
        GearItemData fixed = repaired ? Gear.Load(broken) : null;

        Report repeat = new Report("soulless_repair");
        repeat.check("ensure_idempotent", !secondEnsure);
        repeat.check("data_kept", tridentAfter != null
            && Math.abs(tridentAfter.bq - tridentBefore) < 0.001F);
        repeat.check("broken_repaired", repaired && fixed != null && fixed.hasBase() && fixed.isSoulless());
        repeat.check("repaired_keeps_level", fixed != null && fixed.lvl >= 20);
        out.add(repeat.line());
        counters[0] += repeat.checks;
        counters[1] += repeat.failed;

        // ---- 3d. Стартовый мешок: ровно один готовый посох (а не «посох + душа») ----
        // Баг был в том, что мешок выдавал отдельный «голый» посох и отдельно роллил
        // душу/предмет, и из-за доли дропа готовыми предметами новичок получал два посоха.
        ItemStack starter = com.robertx22.age_of_exile.vanilla_mc.items.misc.ItemNewbieGearBag
            .createStarterGear(com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots.STAFF);
        GearItemData starterData = starter.isEmpty() ? null : Gear.Load(starter);
        boolean starterIsSoul = starterData != null
            && com.robertx22.age_of_exile.uncommon.datasaving.StackSaving.STAT_SOULS.has(starter);

        Report bag = new Report("soulless_starter");
        bag.put("slot", starter.isEmpty() ? "" : SoullessGear.slotIdFor(starter));
        bag.put("items", starter.getCount());
        bag.check("starter_not_empty", !starter.isEmpty());
        // Предмет — именно посох (а не случайный меч из души).
        bag.check("starter_is_staff", "staff".equals(SoullessGear.slotIdFor(starter)));
        // Душа уже вложена в предмет, отдельной душей мешок ничего не выдаёт.
        bag.check("starter_soul_inside", starterData != null && !starterData.isSoulless());
        bag.check("starter_not_soul_item", !starterIsSoul);
        bag.check("starter_has_base", starterData != null && starterData.hasBase());
        bag.check("starter_full_stats", starterData != null && Math.abs(starterData.bq - 1F) < 0.001F);
        bag.check("starter_unsalvageable", starterData != null && !starterData.can_sal);
        out.add(bag.line());
        counters[0] += bag.checks;
        counters[1] += bag.failed;

        // ---- 4. Демо-предметы в хотбар: их снимает сценарий ----
        // Берём предметы 20 уровня: на 1 уровне базовые числа округляются до «1»
        // и разницу между 12 % и 45 % в тултипе не видно.
        try {
            ItemStack demoCheap = new ItemStack(Items.WOODEN_SWORD);
            com.robertx22.age_of_exile.uncommon.datasaving.Gear.Save(demoCheap, cheapMid);
            ItemStack demoRich = new ItemStack(Items.NETHERITE_SWORD);
            com.robertx22.age_of_exile.uncommon.datasaving.Gear.Save(demoRich, richMid);

            ItemStack demoSouled = demoCheap.copy();
            if (loaded != null && loaded.canInsertIntoStack(demoSouled)) {
                loaded.insertAsUnidentifiedOn(demoSouled, player);
            }

            give(player, SLOT_BASE_CHEAP, demoCheap);
            give(player, SLOT_BASE_RICH, demoRich);
            give(player, SLOT_SOULED, demoSouled);
            give(player, SLOT_SOUL, soulStack.copy());
            give(player, SLOT_WHOLE, trident.copy());
        } catch (Exception ignored) {
        }

        Report demo = new Report("soulless_demo");
        demo.put("slots", SLOT_BASE_CHEAP + "," + SLOT_BASE_RICH + "," + SLOT_SOULED + ","
            + SLOT_SOUL + "," + SLOT_WHOLE);
        demo.check("hotbar_filled", !player.getInventory()
            .getItem(SLOT_SOULED)
            .isEmpty());
        demo.check("whole_item_shown", !player.getInventory()
            .getItem(SLOT_WHOLE)
            .isEmpty());
        out.add(demo.line());
        counters[0] += demo.checks;
        counters[1] += demo.failed;
    }

    private static void give(ServerPlayer player, int slot, ItemStack stack) {
        player.getInventory()
            .setItem(slot, stack);
        player.getInventory()
            .setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    /** Суммарное значение базовых статов после пересчёта по уровню предмета. */
    private static float totalBaseValue(GearItemData data) {
        try {
            if (data == null || data.baseStats == null) {
                return -1F;
            }
            float sum = 0F;
            for (com.robertx22.age_of_exile.saveclasses.ExactStatData stat : data.baseStats.GetAllStats(data)) {
                sum += Math.abs(stat.getAverageValue());
            }
            return sum;
        } catch (Exception e) {
            return -1F;
        }
    }

    private static int sumPerc(GearItemData data) {
        if (data == null || data.baseStats == null || data.baseStats.perc == null) {
            return -1;
        }
        return data.baseStats.perc.stream()
            .mapToInt(Integer::intValue)
            .sum();
    }

    /** Сумма роллов базовых статов без ослабления — эталон для сравнения. */
    private static int normalBasePerc(GearItemData reference) {
        try {
            if (reference == null) {
                return -1;
            }
            GearItemData normal = new GearItemData();
            normal.gear_type = reference.gear_type;
            normal.lvl = reference.lvl;
            normal.rarity = "common";
            normal.baseStats.RerollFully(normal);
            return sumPerc(normal);
        } catch (Exception e) {
            return -1;
        }
    }

    private static boolean plainNameMatches(ItemStack stack, GearItemData data) {
        try {
            List<net.minecraft.network.chat.MutableComponent> name = data.GetDisplayName(stack);
            if (name == null || name.isEmpty()) {
                return false;
            }
            String shown = name.stream()
                .map(Component::getString)
                .reduce("", String::concat)
                .trim();
            return shown.equals(stack.getHoverName()
                .getString()
                .trim());
        } catch (Exception e) {
            return false;
        }
    }

    private static final class Report {
        private final String kind;
        private final java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        private int checks;
        private int failed;

        Report(String kind) {
            this.kind = kind;
        }

        void put(String key, Object value) {
            parts.add(key + "=" + value);
        }

        void check(String name, boolean ok) {
            checks++;
            if (!ok) {
                failed++;
            }
            parts.add("check_" + name + "=" + ok);
        }

        String line() {
            return CombatProbe.TAG + " " + kind + " " + String.join(" ", parts);
        }
    }
}
