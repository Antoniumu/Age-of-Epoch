package com.robertx22.age_of_exile.saveclasses.stat_soul;

import com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.BaseStatsData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.GearAffixesData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.ImplicitStatsData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.UpgradeData;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.VanillaMaterial;
import com.robertx22.forge_compat.common.ArmorItem;
import com.robertx22.library_of_exile.registry.Database;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * «Бесдушная основа» предмета — универсальная часть системы гира.
 *
 * <p>Правило системы: <b>любой</b> предмет снаряжения, который существует в мире
 * (дроп, крафт, сундук, выдача командой, модовый меч) обязан иметь бесдушную основу.
 * Без основы предмет не работает: не наносит урон и не даёт статов. Основу выдаёт
 * сервер — при подборе ({@code PlayerInventoryMixin}) и периодической проверкой
 * инвентаря ({@code GearSoulOnInvTick}), а также на выходе генераторов лута.
 *
 * <p>Бесдушная основа — это слабый предмет: 12–45 % базовых статов своего типа,
 * без аффиксов, сокетов и униката, всегда {@code common}. Чем дороже предмет,
 * тем выше его основа, но даже самая дорогая основа слабее того же предмета,
 * на который наложена хотя бы минимальная душа ({@link #applySoul}).
 *
 * <p>Исключение — предметы, у которых «души не существует» и вся их сила в самом
 * предмете: булава, трезубец и элитры ({@link #WHOLE_ITEM_BASES}). Их основа —
 * это весь предмет, поэтому она сильнее обычных основ ({@link #WHOLE_ITEM_BASE_PERCENT}),
 * но всё равно слабее предмета с душой.
 */
public class SoullessGear {

    /**
     * Доля гир-дропа (в процентах), которая падает готовым бесдушным предметом,
     * а не душой. Души продолжают падать как раньше — это основной источник силы.
     */
    public static final int ITEM_DROP_SHARE = 40;

    /** Минимальная сила основы: дешёвый предмет (дерево/кожа). */
    public static final float MIN_BASE_PERCENT = 12F;

    /** Максимальная сила обычной основы: самый дорогой предмет (незерит). */
    public static final float MAX_BASE_PERCENT = 45F;

    /**
     * Сила «цельной» основы — предмета, для которого души не существует
     * (булава, трезубец, элитры). Выше любой обычной основы, но ниже предмета
     * с душой (100 % базовых статов + редкость/аффиксы).
     */
    public static final float WHOLE_ITEM_BASE_PERCENT = 70F;

    /**
     * Предметы, у которых нет души: их бесдушная основа — это весь предмет.
     * Им основа тоже выдаётся (иначе они были бы «пустышками»), но по повышенной
     * силе {@link #WHOLE_ITEM_BASE_PERCENT}.
     */
    public static final List<String> WHOLE_ITEM_BASES = Arrays.asList(
        "minecraft:mace",
        "minecraft:trident",
        "minecraft:elytra"
    );

    /** Старое имя списка, оставлено для совместимости. */
    public static final List<String> DEFAULT_UNIQUES = WHOLE_ITEM_BASES;

    /**
     * Слот AoE, в котором считается «цельная» основа. Булава и трезубец бьют
     * как оружие ближнего боя, элитры занимают слот нагрудника.
     */
    private static final Map<String, String> WHOLE_ITEM_SLOTS = Map.of(
        "minecraft:mace", GearSlots.SWORD,
        "minecraft:trident", GearSlots.SWORD,
        "minecraft:elytra", GearSlots.CHEST
    );

    // ------------------------------------------------------------------
    // проверки
    // ------------------------------------------------------------------

    public static boolean isWholeItemBase(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return isWholeItemBase(stack.getItem());
    }

    public static boolean isWholeItemBase(Item item) {
        return WHOLE_ITEM_BASES.contains(itemId(item));
    }

    /** Старое имя, оставлено для совместимости. */
    public static boolean isDefaultUnique(ItemStack stack) {
        return isWholeItemBase(stack);
    }

    public static boolean isDefaultUnique(Item item) {
        return isWholeItemBase(item);
    }

    public static boolean isSoulless(ItemStack stack) {
        GearItemData data = Gear.Load(stack);
        return data != null && data.isSoulless();
    }

    /**
     * Есть ли у предмета бесдушная основа (или душа) — то есть работает ли он.
     * «Пустышка» без данных не работает: см. {@code LivingHurtUtils}.
     */
    public static boolean hasBase(ItemStack stack) {
        GearItemData data = Gear.Load(stack);
        return data != null && data.isValidItem();
    }

    /**
     * Предмет вообще может носить душу/иметь бесдушную основу?
     * Годится для любых ванильных и модовых мечей, топоров, луков, арбалетов, щитов,
     * брони, колец, ожерелий и посохов мода, а также для оружия без слота AoE
     * (копья, кинжалы, молоты, жезлы) — ему подбирается ближайший слот.
     */
    public static boolean looksLikeGear(ItemStack stack) {
        return slotIdFor(stack) != null;
    }

    /**
     * Похоже ли на оружие (в т.ч. модовое, которого нет в слотах AoE).
     * Нужно, чтобы предметы без бесдушной основы оставались «безделушками»
     * с нулевым уроном, но еда/палки/факелы в руке не превращались в оружие с нулём.
     */
    public static boolean isWeaponLike(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            if (stack.getItem() instanceof com.robertx22.age_of_exile.vanilla_mc.items.gearitems.bases.SingleTargetWeapon) {
                return true;
            }
            String path = itemPath(stack);
            return path.endsWith("_sword") || path.endsWith("_axe") || path.endsWith("_spear")
                || path.endsWith("_dagger") || path.endsWith("_hammer") || path.endsWith("_mace")
                || path.endsWith("_wand") || path.endsWith("_staff") || path.endsWith("_scepter")
                || path.contains("trident") || path.equals("mace");
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // какой слот AoE соответствует предмету
    // ------------------------------------------------------------------

    /**
     * Слот AoE для предмета. Сначала пробуем штатное определение слота, затем
     * эвристику по классу/id — благодаря ей основу получают и модовые вещи,
     * которых нет в реестре слотов (копья, кинжалы, молоты, жезлы, «свой» посох).
     *
     * @return id слота или null, если предмет не снаряжение
     */
    public static String slotIdFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        try {
            // Броня для животных (лошадь, волк, лама) — не снаряжение игрока,
            // хотя и висит в теге нагрудной брони.
            if (isAnimalArmor(itemPath(stack))) {
                return null;
            }

            String whole = wholeItemSlotId(stack);
            if (whole != null) {
                return whole;
            }

            // Посох и жезл — один класс предметов (StaffWeapon), поэтому штатный
            // поиск слота может вернуть «scepter» для посоха. Различаем по id,
            // иначе посох получал бы основу жезла и не принимал душу посоха.
            String path = itemPath(stack);
            if (path.contains("scepter")) {
                return GearSlots.SCEPTER;
            }
            if (path.contains("staff")) {
                return GearSlots.STAFF;
            }

            GearSlot slot = GearSlot.getSlotOf(stack.getItem());
            if (slot != null && !slot.GUID().isEmpty()) {
                return slot.GUID();
            }

            return heuristicSlotId(stack);
        } catch (Exception e) {
            return null;
        }
    }

    private static String wholeItemSlotId(ItemStack stack) {
        return WHOLE_ITEM_SLOTS.get(itemId(stack.getItem()));
    }

    private static String heuristicSlotId(ItemStack stack) {
        try {
            Item item = stack.getItem();
            String path = itemPath(stack);
            ItemStack def = item.getDefaultInstance();

            // арбалет раньше лука: "crossbow" содержит "bow"
            if (item instanceof CrossbowItem || path.equals("crossbow") || path.endsWith("_crossbow")) {
                return GearSlots.CROSBOW;
            }
            if (item instanceof BowItem || path.equals("bow") || path.endsWith("_bow")) {
                return GearSlots.BOW;
            }
            if (path.endsWith("_staff") || path.contains("staff")) {
                return GearSlots.STAFF;
            }
            if (path.endsWith("_scepter") || path.endsWith("_wand") || path.contains("scepter") || path.contains("wand")) {
                return GearSlots.SCEPTER;
            }
            if (item instanceof ShieldItem || path.equals("shield") || path.endsWith("_shield")) {
                return GearSlots.SHIELD;
            }

            if (isArmorLike(item, def, path)) {
                if (path.endsWith("_helmet") || def.is(ItemTags.HEAD_ARMOR)) {
                    return GearSlots.HELMET;
                }
                if (path.endsWith("_chestplate") || def.is(ItemTags.CHEST_ARMOR)) {
                    return GearSlots.CHEST;
                }
                if (path.endsWith("_leggings") || def.is(ItemTags.LEG_ARMOR)) {
                    return GearSlots.PANTS;
                }
                if (path.endsWith("_boots") || def.is(ItemTags.FOOT_ARMOR)) {
                    return GearSlots.BOOTS;
                }
            }

            // "_ring"/"ring_", а не contains("ring"): иначе «string» стал бы кольцом.
            if (path.equals("ring") || path.endsWith("_ring") || path.contains("ring_")) {
                return GearSlots.RING;
            }
            if (path.contains("necklace") || path.contains("amulet")) {
                return GearSlots.NECKLACE;
            }

            // Копья, кинжалы, молоты, алебарды и прочее модовое оружие без слота AoE
            // считаем мечами: у них та же роль — оружие ближнего боя в основной руке.
            if (isWeaponLike(stack)) {
                return GearSlots.SWORD;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Броня/упряжь для животных: снаряжением игрока не считается. */
    private static boolean isAnimalArmor(String path) {
        return path.contains("horse_armor") || path.contains("wolf_armor")
            || path.contains("llama") || path.endsWith("_harness");
    }

    private static boolean isArmorLike(Item item, ItemStack def, String path) {
        return item instanceof ArmorItem
            || path.endsWith("_helmet") || path.endsWith("_chestplate")
            || path.endsWith("_leggings") || path.endsWith("_boots")
            || def.is(ItemTags.HEAD_ARMOR) || def.is(ItemTags.CHEST_ARMOR)
            || def.is(ItemTags.LEG_ARMOR) || def.is(ItemTags.FOOT_ARMOR);
    }

    // ------------------------------------------------------------------
    // «дороговизна» предмета
    // ------------------------------------------------------------------

    /**
     * Ранг материала: 1 — дерево/кожа, 2 — камень/цепной доспех/лук, 3 — железо/золото,
     * 4 — алмаз, 5 — незерит. Опирается на id предмета, поэтому работает и для ванильных,
     * и для модовых вещей (у посохов/колец материал прямо в id: iron_ring и т.д.).
     * Если материала в id нет (модовый сплав), ранг оценивается по прочности предмета —
     * так «дороговизна» работает и для вещей из других модов.
     */
    public static int materialRank(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 2;
        }
        try {
            String path = itemPath(stack);

            if (path.contains("netherite")) {
                return 5;
            }
            if (path.contains("diamond")) {
                return 4;
            }
            if (path.contains("iron") || path.contains("gold") || path.contains("chainmail")) {
                return 3;
            }
            if (path.contains("stone")) {
                return 2;
            }
            if (path.contains("wood") || path.contains("leather")) {
                return 1;
            }
            // Незнакомый материал: чем прочнее предмет, тем он обычно дороже.
            return rankFromDurability(stack);
        } catch (Exception e) {
            return 2;
        }
    }

    /** Оценка ранга материала по максимальной прочности (для модовых предметов). */
    private static int rankFromDurability(ItemStack stack) {
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return 2; // неразрушимый/не оружие — считаем средним
        }
        if (max < 150) {
            return 1;
        }
        if (max < 400) {
            return 2;
        }
        if (max < 1000) {
            return 3;
        }
        if (max < 2000) {
            return 4;
        }
        return 5;
    }

    /** 0..1 — «дороговизна» по рангу материала. */
    public static float costFactor(ItemStack stack) {
        return factorForRank(materialRank(stack));
    }

    public static float factorForRank(int rank) {
        return Mth.clamp((rank - 1) / 4F, 0F, 1F);
    }

    /** Ранг материала, который уместен для предмета такого уровня. */
    public static int rankForLevel(int lvl) {
        int max = 50;
        try {
            max = Math.max(1, GameBalanceConfig.get().MAX_LEVEL);
        } catch (Exception ignored) {
        }
        float perc = Mth.clamp((float) lvl / (float) max, 0F, 1F);

        if (perc <= 0.18F) {
            return 1;
        }
        if (perc <= 0.38F) {
            return 2;
        }
        if (perc <= 0.62F) {
            return 3;
        }
        if (perc <= 0.82F) {
            return 4;
        }
        return 5;
    }

    /**
     * Сколько процентов от базовых статов получает обычная бесдушная основа:
     * 12 % у дешёвых предметов и 45 % у самых дорогих. Специально невысоко —
     * сила приходит от души.
     */
    public static int basePercent(float factor) {
        return Math.round(MIN_BASE_PERCENT + (MAX_BASE_PERCENT - MIN_BASE_PERCENT)
            * Mth.clamp(factor, 0F, 1F));
    }

    /** Сила основы конкретного предмета в процентах — для тултипов и проверок. */
    public static int basePercentFor(ItemStack stack) {
        if (isWholeItemBase(stack)) {
            return Math.round(WHOLE_ITEM_BASE_PERCENT);
        }
        return basePercent(costFactor(stack));
    }

    // ------------------------------------------------------------------
    // генерация бесдушной основы
    // ------------------------------------------------------------------

    public static BaseGearType pickType(String slotId, int lvl) {
        try {
            BaseGearType type = ExileDB.GearTypes()
                .getFilterWrapped(x -> x.gear_slot.equals(slotId) && x.getLevelRange()
                    .isLevelInRange(lvl))
                .errorIfNothingLeft(false)
                .random();

            if (type == null) {
                type = ExileDB.GearTypes()
                    .getFilterWrapped(x -> x.gear_slot.equals(slotId))
                    .errorIfNothingLeft(false)
                    .random();
            }
            return type;
        } catch (Exception e) {
            return null;
        }
    }

    public static GearItemData create(BaseGearType type, int lvl, float factor) {
        return create(type, lvl, factor, false);
    }

    /**
     * @param wholeItemBase предмет, у которого нет души (булава/трезубец/элитры):
     *                      его основа — весь предмет, поэтому она сильнее обычной
     */
    public static GearItemData create(BaseGearType type, int lvl, float factor, boolean wholeItemBase) {
        GearItemData data = new GearItemData();

        data.gear_type = type.GUID();
        data.lvl = Math.max(1, lvl);
        data.rarity = IRarity.COMMON_ID;

        data.up = new UpgradeData();
        if (data.getRarity() != null) {
            data.up.regenerate(data.getRarity());
        }

        // Роллы остаются полными (perc лишь выбирает значение внутри min..max),
        // а «сила основы» применяется множителем bq — иначе 12 % и 45 % давали бы
        // почти одинаковые числа: perc не масштабирует стат, а двигает его в диапазоне.
        data.baseStats = new BaseStatsData();
        data.baseStats.RerollFully(data);
        data.bq = (wholeItemBase ? WHOLE_ITEM_BASE_PERCENT : basePercent(factor)) / 100F;

        data.imp = new ImplicitStatsData();
        data.imp.RerollFully(data);

        // Ни аффиксов, ни сокетов, ни уника: всё это приносит только настоящая душа.
        data.affixes = new GearAffixesData();

        data.sl = true;
        data.wi = wholeItemBase;
        return data;
    }

    public static GearItemData createFor(ItemStack stack, int lvl) {
        String slotId = slotIdFor(stack);
        if (slotId == null) {
            return null;
        }
        BaseGearType type = pickType(slotId, lvl);
        if (type == null) {
            return null;
        }
        return create(type, lvl, costFactor(stack), isWholeItemBase(stack));
    }

    /**
     * «Сервер фиксирует предмет»: если у вещи ещё нет действующей основы (или данные
     * битые/от другой вещи) — генерируем основу по сложности предмета.
     *
     * @return true, если предмет изменён
     */
    public static boolean ensure(ItemStack stack, Player player) {
        if (player == null) {
            return false;
        }
        Level level = player.level();
        if (level == null || level.isClientSide()) {
            return false;
        }
        // Дешёвый выход для 99 % слотов: у вещи уже есть основа (или это не гир).
        // Иначе секундная проверка инвентаря каждый раз считала бы уровень игрока.
        if (!needsBase(stack)) {
            return false;
        }

        int lvl = 1;
        try {
            lvl = Mth.clamp(Load.Unit(player)
                .getLevel(), 1, GameBalanceConfig.get().MAX_LEVEL);
        } catch (Exception ignored) {
        }

        return ensure(stack, level, lvl);
    }

    /** Версия без игрока: для генераторов лута, где уровень известен из LootInfo. */
    public static boolean ensure(ItemStack stack, int lvl) {
        return ensure(stack, null, lvl);
    }

    /**
     * Ядро проверки. Работает и на сервере, и в генераторах лута.
     *
     * @param level уровень мира (нужен для проверки загрузки датапаков), может быть null
     * @param lvl   уровень предмета, если у вещи его ещё нет
     */
    public static boolean ensure(ItemStack stack, @Nullable Level level, int lvl) {
        try {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            if (level != null && level.isClientSide()) {
                return false;
            }

            // Души и незавершённый крафт — не снаряжение, их трогать нельзя.
            if (StackSaving.STAT_SOULS.has(stack) || StackSaving.CRAFT_PROCESS.has(stack)) {
                return false;
            }

            GearItemData existing = Gear.has(stack) ? Gear.Load(stack) : null;

            // Данные есть и тип зарегистрирован — у предмета уже есть действующая
            // основа (бесдушная) или душа. Второй раз ничего не выдаём.
            if (existing != null && existing.isValidItem()) {
                return false;
            }

            String slotId = slotIdFor(stack);
            if (slotId == null) {
                return false; // не снаряжение
            }

            if (!Database.areDatapacksLoaded(level)) {
                return false;
            }

            int useLvl = Math.max(1, lvl);
            if (existing != null && existing.lvl > 0) {
                // Битые данные всё равно помнят уровень — не понижаем предмет.
                useLvl = Math.max(useLvl, existing.lvl);
            }

            BaseGearType type = pickType(slotId, useLvl);
            if (type == null) {
                return false;
            }

            GearItemData data = create(type, useLvl, costFactor(stack), isWholeItemBase(stack));
            if (existing != null) {
                keepSurvivingFields(existing, data);
            }
            data.item_id = itemId(stack);
            Gear.Save(stack, data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Что сохраняем при «починке» битых данных: заточки, крафт, камни, флаги. */
    private static void keepSurvivingFields(GearItemData from, GearItemData to) {
        try {
            if (from.up != null && !from.up.ups.isEmpty()) {
                to.up = from.up;
            }
            if (from.cr != null) {
                to.cr = from.cr;
            }
            if (from.sockets != null && from.sockets.getSocketedGemsCount() > 0) {
                to.sockets = from.sockets;
            }
            to.can_sal = from.can_sal;
            to.c = from.c;
            to.setInstability(from.getInstability());
        } catch (Exception ignored) {
        }
    }

    /** Есть ли у предмета вообще шанс получить основу (для тултипа и проверок). */
    public static boolean needsBase(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (StackSaving.STAT_SOULS.has(stack) || StackSaving.CRAFT_PROCESS.has(stack)) {
            return false;
        }
        if (hasBase(stack)) {
            return false;
        }
        return slotIdFor(stack) != null;
    }

    // ------------------------------------------------------------------
    // дроп готовым предметом
    // ------------------------------------------------------------------

    public static ItemStack createLootItem(BaseGearType type, int lvl) {
        return createLootItem(type, lvl, rankForLevel(lvl));
    }

    /** Готовая бесдушная вещь нужного слота и материала (материал = ранг 1..5). */
    public static ItemStack createLootItem(BaseGearType type, int lvl, int rank) {
        try {
            if (type == null) {
                return ItemStack.EMPTY;
            }
            Item item = itemFor(type.gear_slot, rank);
            if (item == null || item == Items.AIR) {
                return ItemStack.EMPTY;
            }

            ItemStack stack = new ItemStack(item);
            GearItemData data = create(type, lvl, factorForRank(rank), isWholeItemBase(item));
            data.item_id = itemId(stack);
            Gear.Save(stack, data);
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static Item itemFor(String gearSlot, int rank) {
        try {
            Item weapon = weaponItem(gearSlot, rank);
            if (weapon != Items.AIR) {
                return weapon;
            }

            Item armor = materialItem(gearSlot, rank);
            if (armor != Items.AIR) {
                return armor;
            }

            if (gearSlot.equals(GearSlots.SHIELD)) {
                return Items.SHIELD;
            }

            if (gearSlot.equals(GearSlots.STAFF) || gearSlot.equals(GearSlots.SCEPTER)) {
                return SlashItems.GearItems.STAFFS.get(vanillaMaterial(rank))
                    .get();
            }

            VanillaMaterial jewelry = jewelryMaterial(rank);

            if (gearSlot.equals(GearSlots.RING)) {
                return SlashItems.GearItems.RINGS.get(jewelry)
                    .get();
            }
            if (gearSlot.equals(GearSlots.NECKLACE)) {
                return SlashItems.GearItems.NECKLACES.get(jewelry)
                    .get();
            }
        } catch (Exception e) {
            return Items.AIR;
        }
        return Items.AIR;
    }

    private static Item materialItem(String slotPath, int rank) {
        String prefix = switch (rank) {
            case 1 -> "leather";
            case 2 -> "chainmail";
            case 3 -> "iron";
            case 4 -> "diamond";
            default -> "netherite";
        };

        return switch (slotPath + "|" + prefix) {
            case "helmet|leather" -> Items.LEATHER_HELMET;
            case "helmet|chainmail" -> Items.CHAINMAIL_HELMET;
            case "helmet|iron" -> Items.IRON_HELMET;
            case "helmet|diamond" -> Items.DIAMOND_HELMET;
            case "helmet|netherite" -> Items.NETHERITE_HELMET;

            case "chest|leather" -> Items.LEATHER_CHESTPLATE;
            case "chest|chainmail" -> Items.CHAINMAIL_CHESTPLATE;
            case "chest|iron" -> Items.IRON_CHESTPLATE;
            case "chest|diamond" -> Items.DIAMOND_CHESTPLATE;
            case "chest|netherite" -> Items.NETHERITE_CHESTPLATE;

            case "pants|leather" -> Items.LEATHER_LEGGINGS;
            case "pants|chainmail" -> Items.CHAINMAIL_LEGGINGS;
            case "pants|iron" -> Items.IRON_LEGGINGS;
            case "pants|diamond" -> Items.DIAMOND_LEGGINGS;
            case "pants|netherite" -> Items.NETHERITE_LEGGINGS;

            case "boots|leather" -> Items.LEATHER_BOOTS;
            case "boots|chainmail" -> Items.CHAINMAIL_BOOTS;
            case "boots|iron" -> Items.IRON_BOOTS;
            case "boots|diamond" -> Items.DIAMOND_BOOTS;
            case "boots|netherite" -> Items.NETHERITE_BOOTS;

            default -> Items.AIR;
        };
    }

    private static Item weaponItem(String slotPath, int rank) {
        return switch (slotPath + "|" + rank) {
            case "sword|1" -> Items.WOODEN_SWORD;
            case "sword|2" -> Items.STONE_SWORD;
            case "sword|3" -> Items.IRON_SWORD;
            case "sword|4" -> Items.DIAMOND_SWORD;
            case "sword|5" -> Items.NETHERITE_SWORD;

            case "axe|1" -> Items.WOODEN_AXE;
            case "axe|2" -> Items.STONE_AXE;
            case "axe|3" -> Items.IRON_AXE;
            case "axe|4" -> Items.DIAMOND_AXE;
            case "axe|5" -> Items.NETHERITE_AXE;

            case "bow|1", "bow|2", "bow|3", "bow|4", "bow|5" -> Items.BOW;
            case "crossbow|1", "crossbow|2", "crossbow|3", "crossbow|4", "crossbow|5" -> Items.CROSSBOW;
            default -> Items.AIR;
        };
    }

    private static VanillaMaterial jewelryMaterial(int rank) {
        if (rank >= 5) {
            return VanillaMaterial.DIAMOND;
        }
        if (rank == 4) {
            return VanillaMaterial.GOLD;
        }
        return VanillaMaterial.IRON;
    }

    private static VanillaMaterial vanillaMaterial(int rank) {
        if (rank >= 4) {
            return VanillaMaterial.DIAMOND;
        }
        if (rank == 3) {
            return VanillaMaterial.IRON;
        }
        if (rank == 2) {
            return VanillaMaterial.GOLD;
        }
        return VanillaMaterial.WOOD;
    }

    // ------------------------------------------------------------------
    // наложение настоящей души
    // ------------------------------------------------------------------

    /**
     * Накладывает данные души на бесдушную основу: редкость, аффиксы, имя и сокеты
     * берутся у души, а тип предмета, роллы базовых статов, заточки, сокеты и
     * крафтовые статы остаются от самого предмета.
     *
     * <p>Душа «завершает» предмет: бесдушная слабость (12–45 %) снимается, и предмет
     * получает полные базовые статы. Поэтому любая основа слабее того же предмета
     * с душой — даже самой минимальной.
     */
    public static void applySoul(GearItemData soulData, GearItemData base) {

        if (soulData == null || base == null) {
            return;
        }

        // Предмет остаётся собой: алмазный меч не превращается в случайный меч из души.
        soulData.gear_type = base.gear_type;

        // Уровень предмета не может упасть ниже уровня основы: иначе дешёвая
        // низкотировая душа «омолаживала» бы готовый предмет и обнуляла его
        // базовые статы (в живом клиенте у меча 20 уровня после души первого
        // тира оставалось Physical Damage: 0).
        soulData.lvl = Math.max(base.lvl, soulData.lvl);

        if (base.baseStats != null) {
            soulData.baseStats = base.baseStats;
        }
        if (base.imp != null) {
            soulData.imp = base.imp;
        }
        if (base.cr != null) {
            soulData.cr = base.cr;
        }
        if (base.up != null) {
            soulData.up = base.up;
        }
        if (base.sockets != null) {
            soulData.sockets = base.sockets;
        }

        soulData.can_sal = base.can_sal;
        // Душа снимает слабость основы: предмет с душой сильнее любой бесдушной основы.
        soulData.bq = 1F;
        soulData.sl = false;
        soulData.wi = false;
        soulData.item_id = base.item_id;
    }

    // ------------------------------------------------------------------
    // утилиты
    // ------------------------------------------------------------------

    private static String itemPath(ItemStack stack) {
        try {
            return BuiltInRegistries.ITEM.getKey(stack.getItem())
                .getPath();
        } catch (Exception e) {
            return "";
        }
    }

    private static String itemId(Item item) {
        try {
            return BuiltInRegistries.ITEM.getKey(item)
                .toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** id предмета для сохранения в данных: чтобы Gear.Load не счёл данные «чужими». */
    public static String itemId(ItemStack stack) {
        return stack == null ? "" : itemId(stack.getItem());
    }

    /**
     * Оружие без действующей основы — «безделушка»: урона не наносит.
     * «Цельные» основы (булава/трезубец) не блокируются: их сила — в самом предмете,
     * и даже если сервер не успел записать данные, ванильный урон остаётся.
     */
    public static boolean isUnusableWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty() || hasBase(stack)) {
            return false;
        }
        if (isWholeItemBase(stack)) {
            return false;
        }
        if (StackSaving.STAT_SOULS.has(stack) || StackSaving.CRAFT_PROCESS.has(stack)) {
            return false;
        }
        GearSlot slot = GearSlot.getSlotOf(stack.getItem());
        if (slot != null && slot.fam == com.robertx22.age_of_exile.database.data.gear_types.bases.SlotFamily.Weapon) {
            return true;
        }
        return isWeaponLike(stack);
    }
}
