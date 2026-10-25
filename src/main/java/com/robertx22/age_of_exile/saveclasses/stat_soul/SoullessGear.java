package com.robertx22.age_of_exile.saveclasses.stat_soul;

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
import com.robertx22.library_of_exile.registry.Database;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;

/**
 * «Бесдушная основа» предмета.
 *
 * <p>Раньше любой гир существовал только душой: чтобы получить предмет, нужно было найти
 * душу и найти/скрафтить пустую основу, а потом перетащить душу на основу. Теперь наоборот:
 * любой найденный, выпавший или скрафченный предмет сразу получает слабый набор базовых
 * статов — «бесдушную основу». Такой предмет можно носить, но он намеренно слабый
 * (12–45 % от базовых статов типа предмета, без аффиксов и без редкости — только common).
 *
 * <p>Настоящая душа накладывается поверх основы: она приносит редкость, аффиксы, имя
 * (в т.ч. имя униката) и сокеты, но базовые статы остаются теми, что были у предмета.
 * Поэтому приоритет баланса остаётся за душами: даже дорогая основа без души — это
 * «заготовка», а не готовый предмет.
 *
 * <p>Чем дороже предмет в крафте (дерево → камень → железо → алмаз → незерит,
 * для крафта профессиями — количество и тир ингредиентов), тем выше его бесдушные статы,
 * но даже у самого дорогого предмета основа не превышает {@link #basePercent(float)}.
 */
public class SoullessGear {

    /**
     * Доля гир-дропа (в процентах), которая падает готовым бесдушным предметом,
     * а не душой. Души продолжают падать как раньше — это основной источник силы.
     */
    public static final int ITEM_DROP_SHARE = 40;

    /**
     * Предметы, для которых душ не существует и не будет: «уники по умолчанию».
     * Им не генерируется бесдушная основа, и их ванильный урон/защита не обнуляется.
     */
    public static final List<String> DEFAULT_UNIQUES = Arrays.asList(
        "minecraft:mace",
        "minecraft:trident",
        "minecraft:elytra"
    );

    // ------------------------------------------------------------------
    // проверки
    // ------------------------------------------------------------------

    public static boolean isDefaultUnique(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return isDefaultUnique(stack.getItem());
    }

    public static boolean isDefaultUnique(Item item) {
        try {
            return DEFAULT_UNIQUES.contains(BuiltInRegistries.ITEM.getKey(item)
                .toString());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isSoulless(ItemStack stack) {
        GearItemData data = Gear.Load(stack);
        return data != null && data.isSoulless();
    }

    /**
     * Предмет вообще может носить душу/иметь бесдушную основу?
     * Годится для любых ванильных и модовых мечей, топоров, луков, арбалетов, щитов,
     * брони, колец, ожерелий и посохов мода.
     */
    public static boolean looksLikeGear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            return GearSlot.getSlotOf(stack.getItem()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Похоже ли на оружие (в т.ч. модовое, которого нет в слотах AoE).
     * Нужно, чтобы предметы без бесдушной основы и без души оставались «безделушками»
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
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem())
                .getPath();
            return path.endsWith("_sword") || path.endsWith("_axe") || path.endsWith("_spear")
                || path.endsWith("_dagger") || path.endsWith("_hammer") || path.endsWith("_mace")
                || path.contains("trident") || path.equals("mace");
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // «дороговизна» предмета
    // ------------------------------------------------------------------

    /**
     * Ранг материала: 1 — дерево/кожа, 2 — камень/цепной доспех/лук, 3 — железо/золото,
     * 4 — алмаз, 5 — незерит. Опирается на id предмета, поэтому работает и для ванильных,
     * и для модовых вещей (у посохов/колец материал прямо в id: iron_ring и т.д.).
     */
    public static int materialRank(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 2;
        }
        try {
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem())
                .getPath();

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
            return 2;
        } catch (Exception e) {
            return 2;
        }
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
     * Сколько процентов от базовых статов получает бесдушная основа: 12 % у дешёвых
     * предметов и 45 % у самых дорогих. Специально невысоко — сила приходит от души.
     */
    public static int basePercent(float factor) {
        return (int) Math.round(12 + 33 * Mth.clamp(factor, 0F, 1F));
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
        data.bq = basePercent(factor) / 100F;

        data.imp = new ImplicitStatsData();
        data.imp.RerollFully(data);

        // Ни аффиксов, ни сокетов, ни уника: всё это приносит только настоящая душа.
        data.affixes = new GearAffixesData();

        data.sl = true;
        return data;
    }

    public static GearItemData createFor(ItemStack stack, int lvl) {
        if (!looksLikeGear(stack)) {
            return null;
        }
        GearSlot slot = GearSlot.getSlotOf(stack.getItem());
        if (slot == null) {
            return null;
        }
        BaseGearType type = pickType(slot.GUID(), lvl);
        if (type == null) {
            return null;
        }
        return create(type, lvl, costFactor(stack));
    }

    /**
     * «Сервер фиксирует предмет»: если у вещи ещё нет ни души, ни бесдушной основы —
     * генерируем основу по сложности предмета. Возвращает true, если предмет изменён.
     */
    public static boolean ensure(ItemStack stack, Player player) {
        try {
            if (stack == null || stack.isEmpty() || player == null) {
                return false;
            }
            Level level = player.level();
            if (level == null || level.isClientSide()) {
                return false;
            }
            if (Gear.has(stack) || StackSaving.STAT_SOULS.has(stack)) {
                return false;
            }
            // Дешёвые проверки — до обращения к реестрам: ensure зовётся на каждый add.
            if (isDefaultUnique(stack) || !looksLikeGear(stack)) {
                return false;
            }
            if (!Database.areDatapacksLoaded(level)) {
                return false;
            }

            int lvl = 1;
            try {
                lvl = Mth.clamp(Load.Unit(player)
                    .getLevel(), 1, GameBalanceConfig.get().MAX_LEVEL);
            } catch (Exception ignored) {
            }

            GearItemData data = createFor(stack, lvl);
            if (data == null) {
                return false;
            }
            Gear.Save(stack, data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // дроп готовым предметом
    // ------------------------------------------------------------------

    public static ItemStack createLootItem(BaseGearType type, int lvl) {
        try {
            if (type == null) {
                return ItemStack.EMPTY;
            }
            int rank = rankForLevel(lvl);
            Item item = itemFor(type.gear_slot, rank);
            if (item == null || item == Items.AIR) {
                return ItemStack.EMPTY;
            }

            ItemStack stack = new ItemStack(item);
            GearItemData data = create(type, lvl, factorForRank(rank));
            Gear.Save(stack, data);
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
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

    private static Item itemFor(String gearSlot, int rank) {
        try {
            Item weapon = weaponItem(gearSlot, rank);
            if (weapon != Items.AIR) {
                return weapon;
            }

            Item armor = materialItem(gearSlot, rank);
            if (armor != Items.AIR) {
                return armor;
            }

            if (gearSlot.equals("shield")) {
                return Items.SHIELD;
            }

            if (gearSlot.equals("staff") || gearSlot.equals("scepter")) {
                return SlashItems.GearItems.STAFFS.get(vanillaMaterial(rank))
                    .get();
            }

            VanillaMaterial jewelry = jewelryMaterial(rank);

            if (gearSlot.equals("ring")) {
                return SlashItems.GearItems.RINGS.get(jewelry)
                    .get();
            }
            if (gearSlot.equals("necklace")) {
                return SlashItems.GearItems.NECKLACES.get(jewelry)
                    .get();
            }
        } catch (Exception e) {
            return Items.AIR;
        }
        return Items.AIR;
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
     * берутся у души, а базовые статы, заточки, сокеты и крафтовые статы остаются
     * от самого предмета.
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
        // сила основы (12–45 %) остаётся у предмета и после наложения души
        soulData.bq = base.bq;
        soulData.sl = false;
        soulData.item_id = base.item_id;
    }
}
