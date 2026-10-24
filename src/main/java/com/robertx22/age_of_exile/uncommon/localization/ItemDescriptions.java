package com.robertx22.age_of_exile.uncommon.localization;

import com.robertx22.age_of_exile.database.data.currency.base.CurrencyItem;
import com.robertx22.age_of_exile.player_skills.ingredient.items.BaseCraftedConsumableItem;
import com.robertx22.age_of_exile.player_skills.ingredient.items.CraftToolItem;
import com.robertx22.age_of_exile.player_skills.ingredient.items.IngredientItem;
import com.robertx22.age_of_exile.player_skills.items.SmeltedEssenceItem;
import com.robertx22.age_of_exile.player_skills.items.TieredItem;
import com.robertx22.age_of_exile.player_skills.items.alchemy.AlchemyPotionItem;
import com.robertx22.age_of_exile.player_skills.items.alchemy.CondensedEssenceItem;
import com.robertx22.age_of_exile.player_skills.items.backpacks.BackpackItem;
import com.robertx22.age_of_exile.player_skills.items.backpacks.mat_pouch.MaterialBagItem;
import com.robertx22.age_of_exile.player_skills.items.backpacks.upgrades.BackpackUpgradeItem;
import com.robertx22.age_of_exile.player_skills.items.mining.MiningStoneItem;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulItem;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.vanilla_mc.items.CommonGearProducerItem;
import com.robertx22.age_of_exile.vanilla_mc.items.RarityEssenceItem;
import com.robertx22.age_of_exile.vanilla_mc.items.RarityUpgradeStone;
import com.robertx22.age_of_exile.vanilla_mc.items.SalvageHammerItem;
import com.robertx22.age_of_exile.vanilla_mc.items.SocketExtractorItem;
import com.robertx22.age_of_exile.vanilla_mc.items.crates.gem_crate.LootCrateItem;
import com.robertx22.age_of_exile.vanilla_mc.items.crates.rarity_gear.RarityGearCrateItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.bases.BaseArmorItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.bases.BaseBaublesItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.bases.SingleTargetWeapon;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.GemItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.RuneItem;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.FunnyDeathPotion;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.LootTableItem;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.SalvagedDustItem;
import com.robertx22.forge_compat.client.Screen;
import com.robertx22.infinite_dungeons.item.DungeonCurrencyItem;
import com.robertx22.infinite_dungeons.item.DungeonKeyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Хранилище "человеческих" описаний предметов мода.
 * <p>
 * Сами тексты лежат в языковых файлах, код только решает, какой ключ показать:
 * сначала ищется описание конкретного предмета ({@code mmorpg.item.desc.<path предмета>}),
 * затем описание семейства предметов ({@code mmorpg.item.desc.family.<family>}).
 * <p>
 * Показывается по Alt (как и описания характеристик), в обычном тултипе висит только
 * короткая серая подсказка.
 */
public final class ItemDescriptions {

    public static final String PREFIX = "mmorpg.item.desc.";
    private static final String FAMILY_PREFIX = PREFIX + "family.";
    private static final String MOD_NAMESPACE = "mmorpg";

    private ItemDescriptions() {
    }

    /**
     * Ключ описания конкретного предмета, например mmorpg.item.desc.currency.orb_of_transmutation
     */
    public static String itemKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || id.getPath().isEmpty()) {
            return null;
        }
        String path = id.getPath().replace('/', '.');
        // предметы самого мода — mmorpg.item.desc.<путь>,
        // предметы встроенных сабмодов — mmorpg.item.desc.<неймспейс>.<путь>,
        // чтобы пути вроде reward_crate не пересекались
        if (MOD_NAMESPACE.equals(id.getNamespace())) {
            return PREFIX + path;
        }
        return PREFIX + id.getNamespace() + "." + path;
    }

    /**
     * Ключ описания семейства предметов (самоцветы, руны, ингредиенты и т.д.), либо null.
     */
    public static String familyKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();

        if (item instanceof GemItem) {
            return FAMILY_PREFIX + "gem";
        }
        if (item instanceof RuneItem) {
            return FAMILY_PREFIX + "rune";
        }
        if (item instanceof IngredientItem || StackSaving.INGREDIENTS.has(stack)) {
            return FAMILY_PREFIX + "ingredient";
        }
        if (item instanceof SmeltedEssenceItem) {
            return FAMILY_PREFIX + "smelted_essence";
        }
        if (item instanceof CondensedEssenceItem) {
            return FAMILY_PREFIX + "condensed_essence";
        }
        if (item instanceof MiningStoneItem) {
            return FAMILY_PREFIX + "mining_stone";
        }
        if (item instanceof SalvagedDustItem) {
            return FAMILY_PREFIX + "salvage_dust";
        }
        if (item instanceof CraftToolItem) {
            return FAMILY_PREFIX + "craft_tool";
        }
        if (item instanceof BackpackUpgradeItem) {
            return FAMILY_PREFIX + "backpack_upgrade";
        }
        if (item instanceof MaterialBagItem) {
            return FAMILY_PREFIX + "material_pouch";
        }
        if (item instanceof BackpackItem) {
            return FAMILY_PREFIX + "backpack";
        }
        if (item instanceof StatSoulItem) {
            return FAMILY_PREFIX + "stat_soul";
        }
        if (item instanceof BaseCraftedConsumableItem) {
            return FAMILY_PREFIX + "crafted_consumable";
        }
        if (item instanceof AlchemyPotionItem) {
            return FAMILY_PREFIX + "alchemy_potion";
        }
        if (item instanceof CurrencyItem || item instanceof RarityUpgradeStone) {
            return FAMILY_PREFIX + "currency";
        }
        if (item instanceof LootCrateItem) {
            return FAMILY_PREFIX + "loot_crate";
        }
        if (item instanceof DungeonKeyItem) {
            return FAMILY_PREFIX + "dungeon_key";
        }
        if (item instanceof DungeonCurrencyItem) {
            return FAMILY_PREFIX + "dungeon_coin";
        }
        if (item instanceof TieredItem) {
            return FAMILY_PREFIX + "tiered";
        }
        if (item instanceof BlockItem) {
            return FAMILY_PREFIX + "block";
        }
        if (item instanceof SingleTargetWeapon || item instanceof BaseArmorItem || item instanceof BaseBaublesItem) {
            return FAMILY_PREFIX + "gear";
        }
        return null;
    }

    /**
     * Ключ, который реально существует в языковых файлах, либо null.
     */
    public static String resolveKey(ItemStack stack) {
        String key = itemKey(stack);
        if (key != null && exists(key)) {
            return key;
        }
        key = familyKey(stack);
        if (key != null && exists(key)) {
            return key;
        }
        return null;
    }

    /**
     * I18n.get возвращает сам ключ, если перевода нет — этого достаточно, чтобы
     * не зависеть от наличия I18n.exists в конкретной версии маппингов.
     */
    private static boolean exists(String key) {
        return !I18n.get(key)
            .equals(key);
    }

    public static boolean hasDescription(ItemStack stack) {
        return resolveKey(stack) != null;
    }

    public static List<Component> lines(ItemStack stack) {
        String key = resolveKey(stack);
        if (key == null) {
            return Collections.emptyList();
        }
        return parse(I18n.get(key));
    }

    /**
     * Разбивает многострочный текст из языкового файла на отдельные строки тултипа.
     */
    private static List<Component> parse(String raw) {
        List<Component> list = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return list;
        }
        for (String line : raw.split("\n")) {
            String cleaned = line.replace("\r", "")
                .trim();
            if (!cleaned.isEmpty()) {
                list.add(Component.literal(cleaned)
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        return list;
    }

    /**
     * Предметы, у которых описание (или его основная часть) и так видно в тултипе —
     * им не нужна подсказка про Alt.
     */
    private static boolean alreadyExplained(ItemStack stack) {
        Item item = stack.getItem();

        return item instanceof CurrencyItem
            || item instanceof GemItem
            || item instanceof CraftToolItem
            || item instanceof SalvagedDustItem
            || item instanceof AlchemyPotionItem
            || item instanceof BaseCraftedConsumableItem
            || item instanceof StatSoulItem
            || item instanceof RarityEssenceItem
            || item instanceof RarityUpgradeStone
            || item instanceof BackpackUpgradeItem
            || item instanceof LootTableItem
            || item instanceof RarityGearCrateItem
            || item instanceof SocketExtractorItem
            || item instanceof SalvageHammerItem
            || item instanceof CommonGearProducerItem
            || item instanceof FunnyDeathPotion;
    }

    /**
     * Снаряжение с данными: описание по Alt показываем, но подсказку про Alt
     * не рисуем — тултип снаряжения и так длинный, а Alt там уже занят
     * описаниями характеристик (TooltipInfo.shouldShowDescriptions()).
     */
    private static boolean isGearStack(ItemStack stack) {
        try {
            return Gear.Load(stack) != null;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void appendToTooltip(ItemStack stack, List<Component> tooltip) {
        if (stack == null || stack.isEmpty() || tooltip == null) {
            return;
        }

        boolean alt = Screen.hasAltDown();
        boolean shift = Screen.hasShiftDown();

        if (alt && !shift) {
            List<Component> desc = lines(stack);
            if (desc.isEmpty()) {
                return;
            }
            if (!tooltip.isEmpty() && !isBlank(tooltip.get(tooltip.size() - 1))) {
                tooltip.add(Component.literal(""));
            }
            for (Component line : desc) {
                addUnique(tooltip, line);
            }
        } else if (!alt && !shift && !alreadyExplained(stack) && !isGearStack(stack)) {
            if (hasDescription(stack)) {
                addUnique(tooltip, Words.PressAltForItemDescription.locName()
                    .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static boolean isBlank(Component comp) {
        return comp == null || comp.getString()
            .trim()
            .isEmpty();
    }

    /**
     * Не добавляет строку, если точно такая же уже есть — так описания не дублируют
     * то, что предмет уже выводит сам.
     */
    private static void addUnique(List<Component> tooltip, Component line) {
        String text = line == null ? "" : line.getString();
        for (Component existing : tooltip) {
            if (existing != null && existing.getString()
                .equals(text)) {
                return;
            }
        }
        tooltip.add(line);
    }
}
