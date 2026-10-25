package com.robertx22.age_of_exile.compat.jei;

import com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.age_of_exile.database.data.currency.base.CurrencyItem;
import com.robertx22.age_of_exile.database.data.currency.base.ICurrencyItemEffect;
import com.robertx22.age_of_exile.database.data.currency.loc_reqs.LocReqContext;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.database.data.gems.Gem;
import com.robertx22.age_of_exile.database.data.ingredient.SlashIngredient;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.data.runewords.RuneWord;
import com.robertx22.age_of_exile.database.data.unique_items.UniqueGear;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.loot.blueprints.GearBlueprint;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.BackpackItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CraftToolItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.GemItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.ProfessionItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.RuneItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.player_skills.ingredient.data.IngredientData;
import com.robertx22.age_of_exile.player_skills.items.backpacks.upgrades.BackpackUpgradeItem;
import com.robertx22.age_of_exile.player_skills.items.backpacks.upgrades.BagUpgradesData;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.CraftedStatsData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.GearSocketsData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.SocketData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.UpgradeData;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.enumclasses.LootType;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.age_of_exile.vanilla_mc.items.crates.gem_crate.LootCrateData;
import com.robertx22.age_of_exile.vanilla_mc.items.crates.rarity_gear.RarityGearCrateItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.VanillaMaterial;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.GemItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.RuneItem;
import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.database.db_types.RewardList;
import com.robertx22.infinite_dungeons.item.RewardCrateItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Builds the ItemStacks that JEI displays. Everything here is best-effort: if the
 * datapack registries are not ready (or a single entry is broken), the helpers
 * return an empty stack instead of throwing during a JEI reload.
 */
public final class JeiStacks {

    private static final Map<String, List<Item>> GEAR_BASE_CACHE = new HashMap<>();
    private static final Map<String, List<ItemStack>> GEAR_SAMPLE_CACHE = new HashMap<>();
    private static final Map<String, List<ItemStack>> ACCEPTED_GEAR_CACHE = new HashMap<>();

    private JeiStacks() {
    }

    /**
     * Ingredient stack with real ingredient data attached, so the tooltip shows the
     * stat ranges and the professions it can be used in (the same way the item looks
     * in game when it drops).
     */
    public static ItemStack ingredient(SlashIngredient ing) {
        try {
            Item item = ing.getItem();
            if (item == null || item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = new ItemStack(item);
            StackSaving.INGREDIENTS.saveTo(stack, IngredientData.of(ing, sampleTier()));
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * Middle of the tier range, so displayed stat ranges are neither 1st level nor endgame.
     */
    public static int sampleTier() {
        try {
            int max = LevelUtils.getMaxTier();
            return Math.max(0, Math.min(2, max));
        } catch (Exception e) {
            return 0;
        }
    }

    public static ItemStack craftTool(PlayerSkillEnum skill) {
        try {
            RegObj<? extends Item> tool = null;
            if (skill == PlayerSkillEnum.COOKING) {
                tool = CraftToolItems.COOKING;
            } else if (skill == PlayerSkillEnum.ALCHEMY) {
                tool = CraftToolItems.ALCHEMY;
            } else if (skill == PlayerSkillEnum.INSCRIBING) {
                tool = CraftToolItems.INSCRIBING;
            } else if (skill == PlayerSkillEnum.ARMOR_CRAFTING) {
                tool = CraftToolItems.ARMOR;
            } else if (skill == PlayerSkillEnum.WEAPON_CRAFTING) {
                tool = CraftToolItems.WEAPON;
            } else if (skill == PlayerSkillEnum.JEWEL_CRAFTING) {
                tool = CraftToolItems.JEWEL;
            }
            if (tool == null) {
                return ItemStack.EMPTY;
            }
            Item item = tool.get();
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * Example gear piece a gear-crafting profession works on.
     */
    public static ItemStack craftGearSample(PlayerSkillEnum skill) {
        try {
            if (skill == PlayerSkillEnum.ARMOR_CRAFTING) {
                return new ItemStack(Items.IRON_CHESTPLATE);
            }
            if (skill == PlayerSkillEnum.WEAPON_CRAFTING) {
                return new ItemStack(Items.IRON_SWORD);
            }
            if (skill == PlayerSkillEnum.JEWEL_CRAFTING) {
                return gearForSlot(GearSlots.RING);
            }
        } catch (Exception e) {
            // ignore
        }
        return ItemStack.EMPTY;
    }

    /**
     * Representative item for an Age of Exile gear slot. Armor and weapons are plain
     * vanilla items in this port, jewelry and staves are registered by the mod.
     */
    public static ItemStack gearForSlot(String slot) {
        try {
            if (slot == null) {
                return ItemStack.EMPTY;
            }
            if (slot.equals(GearSlots.RING)) {
                return new ItemStack(SlashItems.GearItems.RINGS.get(VanillaMaterial.IRON).get());
            }
            if (slot.equals(GearSlots.NECKLACE)) {
                return new ItemStack(SlashItems.GearItems.NECKLACES.get(VanillaMaterial.IRON).get());
            }
            if (slot.equals(GearSlots.STAFF) || slot.equals(GearSlots.SCEPTER)) {
                return new ItemStack(SlashItems.GearItems.STAFFS.get(VanillaMaterial.IRON).get());
            }
            if (slot.equals(GearSlots.SWORD)) {
                return new ItemStack(Items.IRON_SWORD);
            }
            if (slot.equals(GearSlots.AXE)) {
                return new ItemStack(Items.IRON_AXE);
            }
            if (slot.equals(GearSlots.BOW)) {
                return new ItemStack(Items.BOW);
            }
            if (slot.equals(GearSlots.CROSBOW)) {
                return new ItemStack(Items.CROSSBOW);
            }
            if (slot.equals(GearSlots.HELMET)) {
                return new ItemStack(Items.IRON_HELMET);
            }
            if (slot.equals(GearSlots.CHEST)) {
                return new ItemStack(Items.IRON_CHESTPLATE);
            }
            if (slot.equals(GearSlots.PANTS)) {
                return new ItemStack(Items.IRON_LEGGINGS);
            }
            if (slot.equals(GearSlots.BOOTS)) {
                return new ItemStack(Items.IRON_BOOTS);
            }
            if (slot.equals(GearSlots.SHIELD)) {
                return new ItemStack(Items.SHIELD);
            }
        } catch (Exception e) {
            // ignore
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack rune(RuneItem.RuneType type) {
        try {
            RegObj<RuneItem> obj = RuneItems.MAP.get(type.id);
            if (obj == null) {
                return ItemStack.EMPTY;
            }
            RuneItem item = obj.get();
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * The unique item a runeword turns the base gear into. Building real gear data is
     * heavier than a plain stack, so a failure falls back to the base item.
     */
    public static ItemStack runewordResult(RuneWord word) {
        ItemStack base = ItemStack.EMPTY;
        try {
            String slot = word.slots.isEmpty() ? "" : word.slots.get(0);
            base = gearForSlot(slot);
            if (base.isEmpty()) {
                return ItemStack.EMPTY;
            }

            UniqueGear uniq = ExileDB.UniqueGears().get(word.uniq_id);
            if (uniq == null) {
                return base;
            }

            GearBlueprint blueprint = new GearBlueprint(LevelUtils.tierToLevel(sampleTier()));
            blueprint.uniquePart.set(uniq);
            blueprint.rarity.set(uniq.getUniqueRarity());

            GearItemData data = blueprint.createData();
            data.saveToStack(base);
            return base;
        } catch (Exception e) {
            return base;
        }
    }

    /**
     * Example gear of a given rarity, used by the salvage and currency pages.
     */
    public static ItemStack raritySample(GearRarity rarity) {
        ItemStack base = gearForSlot(GearSlots.CHEST);
        try {
            if (base.isEmpty()) {
                return ItemStack.EMPTY;
            }
            BaseGearType gearType = null;
            for (BaseGearType type : ExileDB.GearTypes().getList()) {
                if (GearSlots.CHEST.equals(type.gear_slot)) {
                    gearType = type;
                    break;
                }
            }
            if (gearType == null) {
                return base;
            }

            GearBlueprint blueprint = new GearBlueprint(LevelUtils.tierToLevel(sampleTier()));
            blueprint.gearItemSlot.set(gearType);
            blueprint.rarity.set(rarity);

            GearItemData data = blueprint.createData();
            data.saveToStack(base);
            return base;
        } catch (Exception e) {
            return base;
        }
    }

    public static ItemStack dust(int tier, int count) {
        try {
            SkillItemTier skillTier = SkillItemTier.of(tier);
            if (skillTier == null) {
                return ItemStack.EMPTY;
            }
            RegObj<com.robertx22.age_of_exile.vanilla_mc.items.misc.SalvagedDustItem> obj =
                ProfessionItems.SALVAGED_ESSENCE_MAP.get(skillTier);
            if (obj == null) {
                return ItemStack.EMPTY;
            }
            Item item = obj.get();
            if (item == null) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = new ItemStack(item);
            stack.setCount(Math.max(1, count));
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    // ------------------------------------------------------------------
    // Generic stack helpers
    // ------------------------------------------------------------------

    public static ItemStack stack(Item item) {
        return stack(item, 1);
    }

    public static ItemStack stack(Item item, int count) {
        try {
            if (item == null || item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, Math.max(1, count));
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static List<ItemStack> clean(List<ItemStack> stacks) {
        List<ItemStack> list = new ArrayList<>();
        if (stacks == null) {
            return list;
        }
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                list.add(stack);
            }
        }
        return list;
    }

    public static List<ItemStack> copies(List<ItemStack> stacks) {
        List<ItemStack> list = new ArrayList<>();
        if (stacks == null) {
            return list;
        }
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                list.add(stack.copy());
            }
        }
        return list;
    }

    public static List<ItemStack> one(ItemStack stack) {
        List<ItemStack> list = new ArrayList<>();
        if (stack != null && !stack.isEmpty()) {
            list.add(stack);
        }
        return list;
    }

    public static ItemStack first(List<ItemStack> stacks) {
        if (stacks == null) {
            return ItemStack.EMPTY;
        }
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------
    // Profession ingredients
    // ------------------------------------------------------------------

    public static ItemStack ingredient(SlashIngredient ing, int tier) {
        try {
            if (ing == null) {
                return ItemStack.EMPTY;
            }
            Item item = ing.getItem();
            if (item == null || item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = new ItemStack(item);
            StackSaving.INGREDIENTS.saveTo(stack, IngredientData.of(ing, tier));
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * One stack per actual item tier, so pressing U/R on any dropped tier finds the
     * same profession pages.
     */
    public static List<ItemStack> ingredientVariants(SlashIngredient ing) {
        List<ItemStack> list = new ArrayList<>();
        try {
            for (int tier : new TreeSet<>(LevelUtils.getAllTiers())) {
                ItemStack stack = ingredient(ing, tier);
                if (!stack.isEmpty()) {
                    list.add(stack);
                }
            }
        } catch (Exception e) {
            // fall through to the sample below
        }
        if (list.isEmpty()) {
            ItemStack fallback = ingredient(ing, sampleTier());
            if (!fallback.isEmpty()) {
                list.add(fallback);
            }
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Physical gear bases and generated gear samples
    // ------------------------------------------------------------------

    private static boolean looksLikeGearBase(Item item) {
        try {
            ItemStack def = item.getDefaultInstance();
            // GearSlot also has path heuristics. Without this guard those would match
            // stackable non-gear (for example "string" contains "ring").
            return def.getMaxStackSize() <= 1 || def.getMaxDamage() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Every physical item the game itself accepts for a slot, not just the old
     * one-item exemplars. Datapack/other-mod items discovered by GearSlot therefore
     * appear too.
     */
    public static List<Item> gearBaseItems(String slotId) {
        try {
            if (slotId == null || slotId.isEmpty()) {
                return List.of();
            }
            List<Item> cached = GEAR_BASE_CACHE.get(slotId);
            if (cached != null) {
                return new ArrayList<>(cached);
            }

            GearSlot slot = ExileDB.GearSlots().get(slotId);
            List<Item> list = new ArrayList<>();
            if (slot != null) {
                for (Item item : BuiltInRegistries.ITEM) {
                    try {
                        if (item == null || item == Items.AIR || !looksLikeGearBase(item)) {
                            continue;
                        }
                        if (GearSlot.isItemOfThisSlot(slot, item)) {
                            list.add(item);
                        }
                    } catch (Exception e) {
                        // skip one broken item only
                    }
                }
            }
            list.sort(Comparator.comparing((Item item) -> {
                var key = BuiltInRegistries.ITEM.getKey(item);
                return key == null ? "" : key.toString();
            }));
            GEAR_BASE_CACHE.put(slotId, list);
            return new ArrayList<>(list);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<ItemStack> gearBases(String slotId) {
        List<ItemStack> list = new ArrayList<>();
        for (Item item : gearBaseItems(slotId)) {
            list.add(new ItemStack(item));
        }
        return list;
    }

    public static List<Item> allGearBaseItems() {
        LinkedHashSet<Item> set = new LinkedHashSet<>();
        try {
            for (GearSlot slot : ExileDB.GearSlots().getList()) {
                set.addAll(gearBaseItems(slot.GUID()));
            }
        } catch (Exception e) {
            // registries not ready
        }
        return new ArrayList<>(set);
    }

    public static List<ItemStack> allGearBases() {
        List<ItemStack> list = new ArrayList<>();
        for (Item item : allGearBaseItems()) {
            list.add(new ItemStack(item));
        }
        return list;
    }

    public static List<Item> gearBaseItemsForProfession(PlayerSkillEnum skill) {
        List<Item> list = new ArrayList<>();
        try {
            if (skill == null || !skill.isGearCraftingProf()) {
                return list;
            }
            for (Item item : BuiltInRegistries.ITEM) {
                try {
                    if (item == null || item == Items.AIR || !looksLikeGearBase(item)) {
                        continue;
                    }
                    if (skill.gearMatchesProfession(item)) {
                        list.add(item);
                    }
                } catch (Exception e) {
                    // skip one broken item only
                }
            }
            list.sort(Comparator.comparing((Item item) -> {
                var key = BuiltInRegistries.ITEM.getKey(item);
                return key == null ? "" : key.toString();
            }));
        } catch (Exception e) {
            // registries not ready
        }
        return list;
    }

    public static List<ItemStack> gearBasesForProfession(PlayerSkillEnum skill) {
        List<ItemStack> list = new ArrayList<>();
        for (Item item : gearBaseItemsForProfession(skill)) {
            list.add(new ItemStack(item));
        }
        return list;
    }

    private static BaseGearType firstGearType(String slotId) {
        try {
            if (slotId == null || slotId.isEmpty()) {
                return null;
            }
            for (BaseGearType type : ExileDB.GearTypes().getList()) {
                if (slotId.equals(type.gear_slot)) {
                    return type;
                }
            }
        } catch (Exception e) {
            // registries not ready
        }
        return null;
    }

    private static ItemStack gearWithData(ItemStack base, BaseGearType type, GearRarity rarity, UniqueGear unique, int tier) {
        if (base == null || base.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = base.copy();
        copy.setCount(1);
        if (type == null) {
            return copy;
        }
        try {
            GearBlueprint blueprint = new GearBlueprint(LevelUtils.tierToLevel(Math.max(0, tier)));
            blueprint.gearItemSlot.set(type);
            if (unique != null) {
                blueprint.uniquePart.set(unique);
                GearRarity uniqueRarity = unique.getUniqueRarity();
                if (uniqueRarity != null) {
                    blueprint.rarity.set(uniqueRarity);
                }
            } else if (rarity != null) {
                blueprint.rarity.set(rarity);
            }

            GearItemData data = blueprint.createData();
            if (data == null) {
                return copy;
            }
            // Gear.Load rejects/clears data whose saved item id differs from the
            // physical stack, so stamp the actual JEI base before saving.
            try {
                var key = BuiltInRegistries.ITEM.getKey(copy.getItem());
                if (key != null) {
                    data.item_id = key.toString();
                }
            } catch (Exception ignored) {
            }
            if (unique != null && (data.uniq_id == null || data.uniq_id.isEmpty())) {
                data.uniq_id = unique.GUID();
            }
            data.saveToStack(copy);
            return copy;
        } catch (Exception e) {
            return copy;
        }
    }

    public static ItemStack gearSample(ItemStack base, String slotId, GearRarity rarity, int tier) {
        return gearWithData(base, firstGearType(slotId), rarity, null, tier);
    }

    public static List<ItemStack> gearSamples(String slotId, GearRarity rarity) {
        return gearSamples(slotId, rarity, sampleTier());
    }

    public static List<ItemStack> gearSamples(String slotId, GearRarity rarity, int tier) {
        try {
            if (slotId == null || slotId.isEmpty() || rarity == null) {
                return List.of();
            }
            String key = "slot|" + slotId + "|" + rarity.GUID() + "|" + tier;
            List<ItemStack> cached = GEAR_SAMPLE_CACHE.computeIfAbsent(key, ignored -> {
                List<ItemStack> list = new ArrayList<>();
                BaseGearType type = firstGearType(slotId);
                for (Item item : gearBaseItems(slotId)) {
                    ItemStack sample = gearWithData(new ItemStack(item), type, rarity, null, tier);
                    if (!sample.isEmpty()) {
                        list.add(sample);
                    }
                }
                return list;
            });
            return copies(cached);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<ItemStack> allGearSamples(GearRarity rarity, int tier) {
        List<ItemStack> list = new ArrayList<>();
        try {
            for (GearSlot slot : ExileDB.GearSlots().getList()) {
                list.addAll(gearSamples(slot.GUID(), rarity, tier));
            }
        } catch (Exception e) {
            // registries not ready
        }
        return list;
    }

    public static List<GearRarity> normalRarities() {
        List<GearRarity> list = new ArrayList<>();
        try {
            for (GearRarity rarity : ExileDB.GearRarities().getList()) {
                if (rarity != null && !rarity.is_unique_item && rarity.weight > 0) {
                    list.add(rarity);
                }
            }
        } catch (Exception e) {
            // registries not ready
        }
        return list;
    }

    public static List<ItemStack> allNormalGearSamples(int tier) {
        List<ItemStack> list = new ArrayList<>();
        for (GearRarity rarity : normalRarities()) {
            list.addAll(allGearSamples(rarity, tier));
        }
        return list;
    }

    public static List<ItemStack> professionGearSamples(PlayerSkillEnum skill, GearRarity rarity, int tier) {
        try {
            if (skill == null || rarity == null) {
                return List.of();
            }
            String key = "prof|" + skill.id + "|" + rarity.GUID() + "|" + tier;
            List<ItemStack> cached = GEAR_SAMPLE_CACHE.computeIfAbsent(key, ignored -> {
                List<ItemStack> list = new ArrayList<>();
                for (Item item : gearBaseItemsForProfession(skill)) {
                    try {
                        GearSlot slot = GearSlot.getSlotOf(item);
                        if (slot == null) {
                            continue;
                        }
                        ItemStack sample = gearWithData(new ItemStack(item), firstGearType(slot.GUID()), rarity, null, tier);
                        if (!sample.isEmpty()) {
                            list.add(sample);
                        }
                    } catch (Exception e) {
                        // skip one base only
                    }
                }
                return list;
            });
            return copies(cached);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<ItemStack> professionGearSamples(PlayerSkillEnum skill, int tier) {
        List<ItemStack> list = new ArrayList<>();
        for (GearRarity rarity : normalRarities()) {
            list.addAll(professionGearSamples(skill, rarity, tier));
        }
        return list;
    }

    public static ItemStack uniqueGear(ItemStack base, UniqueGear unique, int tier) {
        if (unique == null) {
            return base == null ? ItemStack.EMPTY : base.copy();
        }
        BaseGearType type = null;
        try {
            type = unique.getBaseGear();
        } catch (Exception ignored) {
        }
        if (type == null) {
            try {
                GearSlot slot = unique.getSlot();
                if (slot != null) {
                    type = firstGearType(slot.GUID());
                }
            } catch (Exception ignored) {
            }
        }
        return gearWithData(base, type, unique.getUniqueRarity(), unique, tier);
    }

    public static List<ItemStack> uniqueSamples(UniqueGear unique, int tier) {
        List<ItemStack> list = new ArrayList<>();
        try {
            if (unique == null || unique.getSlot() == null) {
                return list;
            }
            for (Item item : gearBaseItems(unique.getSlot().GUID())) {
                ItemStack sample = uniqueGear(new ItemStack(item), unique, tier);
                if (!sample.isEmpty()) {
                    list.add(sample);
                }
            }
        } catch (Exception e) {
            // skip one broken unique only in callers' loops
        }
        return list;
    }

    public static List<ItemStack> allUniqueSamples(int tier) {
        try {
            String key = "all_unique|" + tier;
            List<ItemStack> cached = GEAR_SAMPLE_CACHE.computeIfAbsent(key, ignored -> {
                List<ItemStack> list = new ArrayList<>();
                try {
                    for (UniqueGear unique : ExileDB.UniqueGears().getList()) {
                        list.addAll(uniqueSamples(unique, tier));
                    }
                } catch (Exception e) {
                    // registries not ready
                }
                return list;
            });
            return copies(cached);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<ItemStack> runewordResults(RuneWord word) {
        List<ItemStack> list = new ArrayList<>();
        try {
            if (word == null) {
                return list;
            }
            UniqueGear unique = ExileDB.UniqueGears().get(word.uniq_id);
            LinkedHashSet<Item> bases = new LinkedHashSet<>();
            for (String slot : word.slots) {
                bases.addAll(gearBaseItems(slot));
            }
            for (Item item : bases) {
                ItemStack sample;
                if (unique != null) {
                    sample = uniqueGear(new ItemStack(item), unique, sampleTier());
                } else {
                    sample = new ItemStack(item);
                }
                if (!sample.isEmpty()) {
                    list.add(sample);
                }
            }
        } catch (Exception e) {
            // registries not ready
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Generated-gear state variants used by currency/gem/repair pages
    // ------------------------------------------------------------------

    private static ItemStack modifyGear(ItemStack input, Consumer<GearItemData> modifier) {
        if (input == null || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = input.copy();
        try {
            GearItemData data = Gear.Load(copy);
            if (data == null) {
                return copy;
            }
            modifier.accept(data);
            Gear.Save(copy, data);
        } catch (Exception e) {
        }
        return copy;
    }

    public static ItemStack withUpgradeLevel(ItemStack input, int level) {
        return modifyGear(input, data -> {
            if (data.up == null) {
                data.up = new UpgradeData();
            }
            if (data.up.ups == null) {
                data.up.ups = new ArrayList<>();
            }
            int wanted = Math.max(0, level);
            int slots = Math.max(data.up.ups.size(), (wanted + 2) / 3);
            while (data.up.ups.size() < slots) {
                data.up.ups.add(UpgradeData.SlotType.EMPTY);
            }
            int remaining = wanted;
            for (int i = 0; i < data.up.ups.size(); i++) {
                int value = Math.min(3, remaining);
                data.up.ups.set(i, switch (value) {
                    case 1 -> UpgradeData.SlotType.UP1;
                    case 2 -> UpgradeData.SlotType.UP2;
                    case 3 -> UpgradeData.SlotType.UP3;
                    default -> UpgradeData.SlotType.EMPTY;
                });
                remaining -= value;
            }
        });
    }

    public static List<ItemStack> withUpgradeLevel(List<ItemStack> inputs, int level) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack input : clean(inputs)) {
            ItemStack modified = withUpgradeLevel(input, level);
            if (!modified.isEmpty()) {
                list.add(modified);
            }
        }
        return list;
    }

    private static ItemStack withUpgradeState(ItemStack input, UpgradeData.SlotType type) {
        return modifyGear(input, data -> {
            if (data.up == null) {
                data.up = new UpgradeData();
            }
            if (data.up.ups == null) {
                data.up.ups = new ArrayList<>();
            }
            if (data.up.ups.isEmpty()) {
                data.up.ups.add(type);
            } else {
                data.up.ups.set(0, type);
            }
        });
    }

    public static ItemStack withSocket(ItemStack input, Gem gem) {
        if (gem == null || gem.identifier == null || gem.identifier.isEmpty()) {
            return input == null ? ItemStack.EMPTY : input.copy();
        }
        return modifyGear(input, data -> {
            if (data.sockets == null) {
                data.sockets = new GearSocketsData();
            }
            if (data.sockets.sockets == null) {
                data.sockets.sockets = new ArrayList<>();
            }
            SocketData socket = new SocketData();
            socket.gem = gem.identifier;
            data.sockets.sockets.add(socket);
        });
    }

    public static List<ItemStack> withSocket(List<ItemStack> inputs, Gem gem) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack input : clean(inputs)) {
            ItemStack modified = withSocket(input, gem);
            if (!modified.isEmpty()) {
                list.add(modified);
            }
        }
        return list;
    }

    public static ItemStack withCorrupted(ItemStack input) {
        return modifyGear(input, data -> data.c = true);
    }

    public static ItemStack withInstability(ItemStack input) {
        return modifyGear(input, data -> data.setInstability(10));
    }

    public static ItemStack withCraftedStats(ItemStack input) {
        return modifyGear(input, data -> {
            if (data.cr == null) {
                data.cr = new CraftedStatsData();
            }
        });
    }

    public static ItemStack damaged(ItemStack input) {
        if (input == null || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = input.copy();
        try {
            if (copy.getMaxDamage() > 0) {
                copy.setDamageValue(Math.max(1, copy.getMaxDamage() / 2));
            }
        } catch (Exception e) {
        }
        return copy;
    }

    public static ItemStack repaired(ItemStack input) {
        if (input == null || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = input.copy();
        try {
            copy.setDamageValue(0);
        } catch (Exception e) {
        }
        return copy;
    }

    public static List<ItemStack> damagedCopies(List<ItemStack> inputs) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack input : clean(inputs)) {
            ItemStack modified = damaged(input);
            if (!modified.isEmpty()) {
                list.add(modified);
            }
        }
        return list;
    }

    public static List<ItemStack> repairedCopies(List<ItemStack> inputs) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack input : clean(inputs)) {
            ItemStack modified = repaired(input);
            if (!modified.isEmpty()) {
                list.add(modified);
            }
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Stat souls
    // ------------------------------------------------------------------

    public static ItemStack soul(String slot, GearRarity rarity, int tier, UniqueGear unique, boolean identified) {
        return soul(slot, rarity, tier, unique, identified, true);
    }

    public static ItemStack soul(String slot, GearRarity rarity, int tier, UniqueGear unique, boolean identified, boolean canSalvage) {
        try {
            StatSoulData data = new StatSoulData();
            data.slot = slot == null ? "" : slot;
            data.tier = tier;
            data.can_sal = canSalvage;
            if (unique != null) {
                data.uniq = unique.GUID();
                GearRarity uniqueRarity = unique.getUniqueRarity();
                if (uniqueRarity != null) {
                    data.rar = uniqueRarity.GUID();
                }
                try {
                    if (unique.getSlot() != null) {
                        data.slot = unique.getSlot().GUID();
                    }
                } catch (Exception ignored) {
                }
            } else if (rarity != null) {
                data.rar = rarity.GUID();
            }
            if (identified) {
                try {
                    data.gear = data.createGearData(null);
                } catch (Exception e) {
                    data.gear = null;
                }
            }
            return data.toStack();
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack uniqueSoul(UniqueGear unique, int tier, boolean identified) {
        String slot = "";
        try {
            if (unique != null && unique.getSlot() != null) {
                slot = unique.getSlot().GUID();
            }
        } catch (Exception ignored) {
        }
        return soul(slot, null, tier, unique, identified, true);
    }

    /**
     * Compact soul page input: for one slot, cycle rarity and identified state.
     * Recipe subtype matching deliberately omits tier for souls, so one sample tier
     * can represent every dropped tier while the tooltip still says its tier.
     */
    public static List<ItemStack> soulsForSlot(String slot, int tier, boolean includeIdentified) {
        List<ItemStack> list = new ArrayList<>();
        for (GearRarity rarity : normalRarities()) {
            ItemStack plain = soul(slot, rarity, tier, null, false);
            if (!plain.isEmpty()) {
                list.add(plain);
            }
            if (includeIdentified) {
                ItemStack identified = soul(slot, rarity, tier, null, true);
                if (!identified.isEmpty()) {
                    list.add(identified);
                }
            }
        }
        return list;
    }

    public static List<ItemStack> soulsForRarity(GearRarity rarity, int tier, boolean includeIdentified) {
        List<ItemStack> list = new ArrayList<>();
        try {
            if (rarity == null) {
                return list;
            }
            List<String> slots = new ArrayList<>();
            slots.add("");
            for (GearSlot slot : ExileDB.GearSlots().getList()) {
                slots.add(slot.GUID());
            }
            for (String slot : slots) {
                ItemStack plain = soul(slot, rarity, tier, null, false);
                if (!plain.isEmpty()) {
                    list.add(plain);
                }
                if (includeIdentified) {
                    ItemStack identified = soul(slot, rarity, tier, null, true);
                    if (!identified.isEmpty()) {
                        list.add(identified);
                    }
                }
            }
        } catch (Exception e) {
            // registries not ready
        }
        return list;
    }

    public static List<ItemStack> allSouls(int tier, boolean identified) {
        List<ItemStack> list = new ArrayList<>();
        try {
            List<String> slots = new ArrayList<>();
            slots.add("");
            for (GearSlot slot : ExileDB.GearSlots().getList()) {
                slots.add(slot.GUID());
            }
            for (String slot : slots) {
                for (GearRarity rarity : normalRarities()) {
                    ItemStack stack = soul(slot, rarity, tier, null, identified);
                    if (!stack.isEmpty()) {
                        list.add(stack);
                    }
                }
            }
            for (UniqueGear unique : ExileDB.UniqueGears().getList()) {
                ItemStack stack = uniqueSoul(unique, tier, identified);
                if (!stack.isEmpty()) {
                    list.add(stack);
                }
            }
        } catch (Exception e) {
            // registries not ready
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Backpacks and crates
    // ------------------------------------------------------------------

    public static ItemStack backpack() {
        try {
            return new ItemStack(SlashItems.BACKPACK.get());
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack backpackWith(BackpackUpgradeItem... upgrades) {
        ItemStack stack = backpack();
        if (stack.isEmpty() || upgrades == null || upgrades.length == 0) {
            return stack;
        }
        try {
            BagUpgradesData data = new BagUpgradesData();
            if (data.upgrades == null) {
                data.upgrades = new ArrayList<>();
            }
            for (BackpackUpgradeItem upgrade : upgrades) {
                if (upgrade == null) {
                    continue;
                }
                var key = BuiltInRegistries.ITEM.getKey(upgrade);
                if (key != null) {
                    data.upgrades.add(key.toString());
                }
            }
            Collections.sort(data.upgrades);
            data.saveToStack(stack);
        } catch (Exception e) {
            // plain backpack is still a useful fallback
        }
        return stack;
    }

    public static ItemStack backpackAfterUpgrade(ItemStack before, BackpackUpgradeItem upgrade) {
        if (before == null || before.isEmpty() || upgrade == null) {
            return backpackWith(upgrade);
        }
        ItemStack copy = before.copy();
        try {
            BagUpgradesData data = BagUpgradesData.load(copy);
            data.upgrade(upgrade);
            data.saveToStack(copy);
        } catch (Exception e) {
            return backpackWith(upgrade);
        }
        return copy;
    }

    public static ItemStack gem(Gem gem) {
        try {
            if (gem == null) {
                return ItemStack.EMPTY;
            }
            return stack(gem.getItem());
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack gemItem(GemItem.GemType type, GemItem.GemRank rank) {
        try {
            Map<GemItem.GemRank, RegObj<GemItem>> byRank = GemItems.MAP.get(type);
            if (byRank == null) {
                return ItemStack.EMPTY;
            }
            RegObj<GemItem> obj = byRank.get(rank);
            if (obj == null) {
                return ItemStack.EMPTY;
            }
            GemItem item = obj.get();
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack lootCrate(LootType type, int tier) {
        try {
            LootCrateData data = new LootCrateData();
            data.type = type;
            data.tier = tier;
            return data.createStack();
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack rarityCrate(GearRarity rarity) {
        try {
            if (rarity == null) {
                return ItemStack.EMPTY;
            }
            return RarityGearCrateItem.ofRarity(rarity.GUID());
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack rewardCrate(RewardList list) {
        try {
            if (list == null) {
                return ItemStack.EMPTY;
            }
            return RewardCrateItem.create(list.GUID());
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    // ------------------------------------------------------------------
    // Currency/gem candidate validation
    // ------------------------------------------------------------------

    private static Gem firstRegisteredGem() {
        try {
            for (Gem gem : ExileDB.Gems().getList()) {
                if (gem != null && gem.identifier != null && !gem.identifier.isEmpty() && ExileDB.Gems().isRegistered(gem.identifier)) {
                    return gem;
                }
            }
        } catch (Exception e) {
            // registries not ready
        }
        return null;
    }

    private static boolean canModify(ICurrencyItemEffect effect, ItemStack gear, ItemStack effectStack) {
        try {
            return effect != null && effect.canItemBeModified(new LocReqContext(null, gear.copy(), effectStack.copy()));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Samples that pass the actual item effect's own checks. Falling back to all
     * normal gear keeps a page visible for unusual effects whose checks need a
     * player, but the normal path is intentionally evidence-based.
     */
    public static List<ItemStack> acceptedGearSamples(ICurrencyItemEffect effect, ItemStack effectStack) {
        try {
            if (effect == null || effectStack == null || effectStack.isEmpty()) {
                return allNormalGearSamples(sampleTier());
            }
            String itemKey;
            try {
                var key = BuiltInRegistries.ITEM.getKey(effectStack.getItem());
                itemKey = key == null ? effect.getClass().getName() : key.toString();
            } catch (Exception e) {
                itemKey = effect.getClass().getName();
            }
            String cacheKey = effect.getClass().getName() + "|" + itemKey;
            List<ItemStack> cached = ACCEPTED_GEAR_CACHE.get(cacheKey);
            if (cached != null) {
                return copies(cached);
            }

            List<ItemStack> candidates = new ArrayList<>(allNormalGearSamples(sampleTier()));
            candidates.addAll(allUniqueSamples(sampleTier()));
            Gem firstGem = firstRegisteredGem();
            List<ItemStack> accepted = new ArrayList<>();

            for (ItemStack candidate : candidates) {
                List<ItemStack> variants = new ArrayList<>();
                variants.add(candidate.copy());
                variants.add(withUpgradeState(candidate, UpgradeData.SlotType.UP1));
                variants.add(withUpgradeState(candidate, UpgradeData.SlotType.UP2));
                variants.add(withCorrupted(candidate));
                variants.add(withInstability(candidate));
                variants.add(withCraftedStats(candidate));
                if (firstGem != null) {
                    variants.add(withSocket(candidate, firstGem));
                }
                for (ItemStack variant : variants) {
                    if (!variant.isEmpty() && canModify(effect, variant, effectStack)) {
                        accepted.add(variant);
                        break;
                    }
                }
            }

            if (accepted.isEmpty()) {
                accepted = allNormalGearSamples(sampleTier());
            }
            ACCEPTED_GEAR_CACHE.put(cacheKey, copies(accepted));
            return copies(accepted);
        } catch (Exception e) {
            return allNormalGearSamples(sampleTier());
        }
    }

    public static List<ItemStack> currencyGearSamples(CurrencyItem currency) {
        if (currency == null) {
            return allNormalGearSamples(sampleTier());
        }
        return acceptedGearSamples(currency, new ItemStack(currency));
    }

    public static List<ItemStack> gemSocketGearSamples(GemItem gemItem) {
        if (gemItem == null) {
            return allNormalGearSamples(sampleTier());
        }
        return acceptedGearSamples(gemItem, new ItemStack(gemItem));
    }

    // ------------------------------------------------------------------
    // Extra JEI ingredient variants
    // ------------------------------------------------------------------

    private static void addExtra(Map<String, ItemStack> map, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        try {
            var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String id = key == null ? stack.getItem().getClass().getName() : key.toString();
            map.putIfAbsent(id + "#" + JeiSubtypes.ingredientKey(stack), stack.copy());
        } catch (Exception e) {
            // one broken variant must not hide the rest
        }
    }

    private static void addExtra(Map<String, ItemStack> map, List<ItemStack> stacks) {
        for (ItemStack stack : clean(stacks)) {
            addExtra(map, stack);
        }
    }

    /**
     * Data variants JEI would otherwise not know about because they are not plain
     * creative-tab entries: ingredient tiers, souls, rarity/crate data, upgraded
     * backpacks and representative generated gear.
     */
    public static List<ItemStack> extraIngredients() {
        Map<String, ItemStack> map = new LinkedHashMap<>();
        int sampleTier = sampleTier();
        List<Integer> tiers = new ArrayList<>();
        try {
            tiers.addAll(new TreeSet<>(LevelUtils.getAllTiers()));
        } catch (Exception e) {
            tiers.add(sampleTier);
        }

        try {
            for (SlashIngredient ing : ExileDB.Ingredients().getList()) {
                addExtra(map, ingredientVariants(ing));
            }
        } catch (Exception e) {
            // registries not ready
        }

        try {
            List<String> slots = new ArrayList<>();
            slots.add("");
            for (GearSlot slot : ExileDB.GearSlots().getList()) {
                slots.add(slot.GUID());
            }
            for (String slot : slots) {
                for (GearRarity rarity : normalRarities()) {
                    for (int tier : tiers) {
                        addExtra(map, soul(slot, rarity, tier, null, false));
                    }
                    addExtra(map, soul(slot, rarity, sampleTier, null, true));
                }
            }
            for (UniqueGear unique : ExileDB.UniqueGears().getList()) {
                addExtra(map, uniqueSoul(unique, sampleTier, false));
                addExtra(map, first(uniqueSamples(unique, sampleTier)));
            }
        } catch (Exception e) {
            // registries not ready
        }

        try {
            for (GearRarity rarity : ExileDB.GearRarities().getList()) {
                if (rarity == null) {
                    continue;
                }
                if (rarity.rar_ess_per_sal > 0) {
                    ItemStack essence = rarity.getRarityEssenceStack();
                    essence.setCount(rarity.rar_ess_per_sal);
                    addExtra(map, essence);
                }
                if (rarity.canUpgradeRarityIfUpgradeLevelIsHighEnough()) {
                    addExtra(map, rarity.getRarityUpgradeStack());
                }
            }
        } catch (Exception e) {
            // registries not ready
        }

        try {
            for (LootType type : List.of(LootType.Gem, LootType.Rune, LootType.Currency)) {
                for (int tier : tiers) {
                    addExtra(map, lootCrate(type, tier));
                }
            }
            for (GearRarity rarity : normalRarities()) {
                addExtra(map, rarityCrate(rarity));
            }
            for (RewardList rewardList : DungeonsDB.RewardLists().getList()) {
                addExtra(map, rewardCrate(rewardList));
            }
        } catch (Exception e) {
            // registries not ready
        }

        try {
            for (RegObj<BackpackUpgradeItem> obj : BackpackItems.ALL) {
                BackpackUpgradeItem upgrade = obj.get();
                if (upgrade != null) {
                    addExtra(map, backpackWith(upgrade));
                }
            }
        } catch (Exception e) {
            // item registry not ready
        }

        try {
            for (GearRarity rarity : normalRarities()) {
                for (GearSlot slot : ExileDB.GearSlots().getList()) {
                    addExtra(map, first(gearSamples(slot.GUID(), rarity, sampleTier)));
                }
            }
        } catch (Exception e) {
            // registries not ready
        }

        return new ArrayList<>(map.values());
    }
}
