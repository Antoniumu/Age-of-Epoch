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
        base.put("cheap_perc", cheapPerc);
        base.put("rich_perc", richPerc);
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
        base.check("rich_stronger", richPerc > cheapPerc);
        base.check("below_normal", normalPerc > 0 && richPerc < normalPerc * 0.8D);
        base.check("plain_name", cheapData != null && plainNameMatches(cheap, cheapData));
        out.add(base.line());
        counters[0] += base.checks;
        counters[1] += base.failed;

        // ---- 2. Душа накладывается поверх: редкость/имя от души, база от предмета ----
        String typeBefore = cheapData == null ? "" : cheapData.gear_type;
        List<Integer> percBefore = cheapData == null ? List.of() : List.copyOf(cheapData.baseStats.perc);

        StatSoulData soul = new StatSoulData();
        soul.slot = "sword";
        soul.rar = "rare";
        soul.tier = Math.max(0, com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils
            .levelToTier(Math.max(1, player.experienceLevel)));
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

        Report soulReport = new Report("soulless_soul");
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
        out.add(soulReport.line());
        counters[0] += soulReport.checks;
        counters[1] += soulReport.failed;

        // ---- 3. «Уники по умолчанию» и не-гир не трогаем ----
        ItemStack trident = new ItemStack(Items.TRIDENT);
        boolean tridentTouched = SoullessGear.ensure(trident, player);
        ItemStack apple = new ItemStack(Items.APPLE);
        boolean appleTouched = SoullessGear.ensure(apple, player);

        Report guards = new Report("soulless_guards");
        guards.check("trident_untouched", !tridentTouched && !Gear.has(trident));
        guards.check("non_gear_untouched", !appleTouched && !Gear.has(apple));
        guards.check("weapon_like_sword", SoullessGear.isWeaponLike(new ItemStack(Items.IRON_SWORD)));
        guards.check("weapon_like_apple_false", !SoullessGear.isWeaponLike(apple));
        out.add(guards.line());
        counters[0] += guards.checks;
        counters[1] += guards.failed;

        // ---- 4. Демо-предметы в хотбар: их снимает сценарий ----
        try {
            give(player, SLOT_BASE_CHEAP, cheap.copy());
            give(player, SLOT_BASE_RICH, rich.copy());
            give(player, SLOT_SOULED, souled.copy());
            give(player, SLOT_SOUL, soulStack.copy());
        } catch (Exception ignored) {
        }

        Report demo = new Report("soulless_demo");
        demo.put("slots", SLOT_BASE_CHEAP + "," + SLOT_BASE_RICH + "," + SLOT_SOULED + "," + SLOT_SOUL);
        demo.check("hotbar_filled", !player.getInventory()
            .getItem(SLOT_SOULED)
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
            List<Component> name = data.GetDisplayName(stack);
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
