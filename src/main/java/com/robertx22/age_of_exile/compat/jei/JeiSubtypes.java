package com.robertx22.age_of_exile.compat.jei;

import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.database.data.ingredient.SlashIngredient;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CraftedConsumableItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.player_skills.ingredient.data.IngredientData;
import com.robertx22.age_of_exile.player_skills.ingredient.items.IngredientItem;
import com.robertx22.age_of_exile.player_skills.items.backpacks.BackpackItem;
import com.robertx22.age_of_exile.player_skills.items.backpacks.upgrades.BagUpgradesData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.SocketData;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import com.robertx22.age_of_exile.vanilla_mc.items.crates.gem_crate.LootCrateData;
import com.robertx22.age_of_exile.vanilla_mc.items.crates.rarity_gear.RarityGearCrateData;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.baubles.ItemNecklace;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.baubles.ItemRing;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.weapons.StaffWeapon;
import com.robertx22.infinite_dungeons.item.RewardCrateItem;
import com.robertx22.infinite_dungeons.main.DungeonItems;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Semantic JEI identities for this mod's data-heavy items.
 *
 * <p>JEI normally compares whole item components. That is useless for generated
 * gear and souls: no two player items have the same random stats, so neither U nor
 * R would ever find an explanatory page. The {@link UidContext#Ingredient} keys keep
 * catalogue variants distinct where a player would expect it (tier, installed
 * backpack upgrades, upgrade/corruption state), while {@link UidContext#Recipe}
 * keys deliberately describe the broader rule used by each page.</p>
 */
public final class JeiSubtypes {

    private JeiSubtypes() {
    }

    public static void register(ISubtypeRegistration registration) {
        Set<Item> done = new HashSet<>();

        safely(() -> register(done, registration, item(SlashItems.STAT_SOUL), JeiSubtypes::soul));
        safely(() -> register(done, registration, item(SlashItems.RARITY_ESSENCE), JeiSubtypes::rarityItem));
        safely(() -> register(done, registration, item(SlashItems.RARITY_UPGRADE), JeiSubtypes::rarityItem));
        safely(() -> register(done, registration, item(SlashItems.LOOT_CRATE), JeiSubtypes::lootCrate));
        safely(() -> register(done, registration, item(SlashItems.RARITY_GEAR_CRATE), JeiSubtypes::rarityCrate));
        safely(() -> register(done, registration, DungeonItems.REWARD_CRATE.get(), JeiSubtypes::rewardCrate));
        safely(() -> register(done, registration, item(SlashItems.BACKPACK), JeiSubtypes::backpack));

        safely(() -> {
            for (SlashIngredient ingredient : ExileDB.Ingredients().getList()) {
                try {
                    register(done, registration, ingredient.getItem(), JeiSubtypes::ingredient);
                } catch (Exception ignored) {
                    // one datapack ingredient only
                }
            }
        });

        safely(() -> {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof IngredientItem) {
                    register(done, registration, item, JeiSubtypes::ingredient);
                }
            }
        });

        safely(() -> {
            for (RegObj<? extends Item> obj : craftedConsumableObjects()) {
                register(done, registration, item(obj), JeiSubtypes::craftedConsumable);
            }
        });

        safely(() -> {
            for (Item item : BuiltInRegistries.ITEM) {
                try {
                    if (item == null || item == Items.AIR || done.contains(item) || !looksLikeGearBase(item)) {
                        continue;
                    }
                    if (GearSlot.getSlotOf(item) != null || fallbackGear(item)) {
                        register(done, registration, item, JeiSubtypes::gear);
                    }
                } catch (Exception ignored) {
                    // one item/mod must not break JEI startup
                }
            }
        });
    }

    private static void safely(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Item item(RegObj<? extends Item> obj) {
        try {
            return obj == null ? null : obj.get();
        } catch (Exception e) {
            return null;
        }
    }

    private static void register(Set<Item> done, ISubtypeRegistration registration, Item item, ISubtypeInterpreter<ItemStack> interpreter) {
        if (registration == null || item == null || item == Items.AIR || !done.add(item)) {
            return;
        }
        registration.registerSubtypeInterpreter(item, interpreter);
    }

    private static List<RegObj<? extends Item>> craftedConsumableObjects() {
        List<RegObj<? extends Item>> list = new ArrayList<>();
        list.addAll(CraftedConsumableItems.FOODS);
        list.addAll(CraftedConsumableItems.POTIONS);
        list.addAll(CraftedConsumableItems.SCROLLS);
        return list;
    }

    private static boolean isCraftedConsumable(Item item) {
        try {
            for (RegObj<? extends Item> obj : craftedConsumableObjects()) {
                if (item(obj) == item) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Stable key used only for de-duplicating extra ingredient variants.
     */
    public static String ingredientKey(ItemStack stack) {
        Object key = semantic(stack, UidContext.Ingredient);
        return key == null ? "plain" : key.toString();
    }

    private static Object semantic(ItemStack stack, UidContext context) {
        try {
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            Item item = stack.getItem();

            if (item == item(SlashItems.STAT_SOUL) || StackSaving.STAT_SOULS.has(stack)) {
                return soul(stack, context);
            }
            if (StackSaving.INGREDIENTS.has(stack)) {
                return ingredient(stack, context);
            }
            if (item == item(SlashItems.RARITY_ESSENCE) || item == item(SlashItems.RARITY_UPGRADE)) {
                return rarityItem(stack, context);
            }
            if (item == item(SlashItems.LOOT_CRATE) || StackSaving.GEM_CRATE.has(stack)) {
                return lootCrate(stack, context);
            }
            if (item == item(SlashItems.RARITY_GEAR_CRATE) || StackSaving.RARITY_GEAR_CRATE.has(stack)) {
                return rarityCrate(stack, context);
            }
            if (item == DungeonItems.REWARD_CRATE.get() || item instanceof RewardCrateItem) {
                return rewardCrate(stack, context);
            }
            if (item instanceof BackpackItem || item == item(SlashItems.BACKPACK)) {
                return backpack(stack, context);
            }
            if (isCraftedConsumable(item)) {
                return craftedConsumable(stack, context);
            }
            if (Gear.has(stack)) {
                return gear(stack, context);
            }
            if (looksLikeGearBase(item) && GearSlot.getSlotOf(item) != null) {
                return gear(stack, context);
            }
        } catch (Exception ignored) {
            // JEI falls back to its default identity for this one stack.
        }
        return null;
    }

    public static Object soul(ItemStack stack, UidContext context) {
        try {
            if (!StackSaving.STAT_SOULS.has(stack)) {
                return "soul|empty";
            }
            StatSoulData data = StackSaving.STAT_SOULS.loadFrom(stack);
            if (data == null) {
                return "soul|empty";
            }
            String slot = safe(data.slot);
            String rarity = safe(data.rar);
            String unique = safe(data.uniq);
            String identified = data.gear != null ? "1" : "0";
            if (context == UidContext.Recipe) {
                // Application/identification do not depend on the soul tier or on
                // whether this particular dropped soul may be salvaged.
                return "soul|s=" + slot + "|r=" + rarity + "|u=" + unique + "|id=" + identified;
            }
            return "soul|s=" + slot + "|r=" + rarity + "|u=" + unique + "|id=" + identified
                + "|t=" + data.tier + "|sal=" + (data.can_sal ? "1" : "0");
        } catch (Exception ignored) {
            return "soul|invalid";
        }
    }

    public static Object ingredient(ItemStack stack, UidContext context) {
        try {
            if (!StackSaving.INGREDIENTS.has(stack)) {
                return null;
            }
            IngredientData data = StackSaving.INGREDIENTS.loadFrom(stack);
            if (data == null) {
                return null;
            }
            // Stats genuinely scale by tier, so both contexts stay exact.
            return "ingredient|id=" + safe(data.id) + "|t=" + data.tier;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Object gear(ItemStack stack, UidContext context) {
        try {
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            if (!Gear.has(stack)) {
                GearSlot slot = GearSlot.getSlotOf(stack.getItem());
                return slot == null ? null : "base|" + safe(slot.GUID());
            }

            // Gear.Load can remove mismatched data from a stack; never let a JEI
            // focus lookup mutate the player's actual stack.
            GearItemData data = Gear.Load(stack.copy());
            if (data == null) {
                return "gear|invalid";
            }

            String slot = "";
            try {
                BaseGearType type = data.GetBaseGearType();
                if (type != null) {
                    slot = safe(type.gear_slot);
                }
            } catch (Exception ignored) {
            }
            if (slot.isEmpty()) {
                try {
                    GearSlot slotObj = GearSlot.getSlotOf(stack.getItem());
                    if (slotObj != null) {
                        slot = safe(slotObj.GUID());
                    }
                } catch (Exception ignored) {
                }
            }

            String unique = safe(data.uniq_id);
            if (unique.isEmpty() && data.uniqueStats != null) {
                try {
                    var uniqueGear = data.uniqueStats.getUnique(data);
                    if (uniqueGear != null) {
                        unique = safe(uniqueGear.GUID());
                    }
                } catch (Exception ignored) {
                }
            }

            List<String> gems = new ArrayList<>();
            try {
                if (data.sockets != null && data.sockets.sockets != null) {
                    for (SocketData socket : data.sockets.sockets) {
                        if (socket != null && socket.gem != null && !socket.gem.isEmpty()) {
                            gems.add(socket.gem);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            Collections.sort(gems);

            String recipe = "gear|s=" + slot + "|r=" + safe(data.rarity) + "|u=" + unique
                + "|g=" + String.join(",", gems);
            if (context == UidContext.Recipe) {
                return recipe;
            }

            int tier = -1;
            try {
                tier = data.getTier();
            } catch (Exception ignored) {
            }
            int upgradeLevel = 0;
            try {
                if (data.up != null) {
                    upgradeLevel = data.up.getUpgradeLevel();
                }
            } catch (Exception ignored) {
            }
            boolean emptySocket = false;
            try {
                emptySocket = data.getEmptySockets() > 0;
            } catch (Exception ignored) {
            }
            boolean unstable = false;
            try {
                unstable = data.getInstability() > 0;
            } catch (Exception ignored) {
            }

            return recipe + "|t=" + tier + "|up=" + upgradeLevel
                + "|c=" + (data.isCorrupted() ? "1" : "0")
                + "|sal=" + (data.can_sal ? "1" : "0")
                + "|crafted=" + (data.hasCraftedStats() ? "1" : "0")
                + "|empty_socket=" + (emptySocket ? "1" : "0")
                + "|unstable=" + (unstable ? "1" : "0");
        } catch (Exception ignored) {
            return "gear|invalid";
        }
    }

    public static Object lootCrate(ItemStack stack, UidContext context) {
        try {
            if (!StackSaving.GEM_CRATE.has(stack)) {
                return "loot_crate|empty";
            }
            LootCrateData data = StackSaving.GEM_CRATE.loadFrom(stack);
            if (data == null) {
                return "loot_crate|empty";
            }
            return "loot_crate|type=" + String.valueOf(data.type) + "|t=" + data.tier;
        } catch (Exception ignored) {
            return "loot_crate|invalid";
        }
    }

    public static Object rarityCrate(ItemStack stack, UidContext context) {
        try {
            if (!StackSaving.RARITY_GEAR_CRATE.has(stack)) {
                return "rarity_crate|empty";
            }
            RarityGearCrateData data = StackSaving.RARITY_GEAR_CRATE.loadFrom(stack);
            if (data == null) {
                return "rarity_crate|empty";
            }
            if (context == UidContext.Recipe) {
                // Most boxes roll at the opening player's tier; the result rule is
                // the rarity recorded in the box.
                return "rarity_crate|r=" + safe(data.rar);
            }
            return "rarity_crate|r=" + safe(data.rar) + "|t=" + String.valueOf(data.tier)
                + "|player=" + (data.set_to_player_lvl ? "1" : "0");
        } catch (Exception ignored) {
            return "rarity_crate|invalid";
        }
    }

    public static Object rewardCrate(ItemStack stack, UidContext context) {
        return "reward_crate|list=" + tagString(stack, RewardCrateItem.TAG);
    }

    public static Object rarityItem(ItemStack stack, UidContext context) {
        return "rarity|r=" + tagString(stack, "rar");
    }

    public static Object backpack(ItemStack stack, UidContext context) {
        if (context == UidContext.Recipe) {
            // Any installed-upgrade combination can receive another legal upgrade;
            // one exact subset page would make U on a real backpack useless.
            return "backpack";
        }
        try {
            if (!StackNBT.has(stack)) {
                return "backpack|up=";
            }
            BagUpgradesData data = BagUpgradesData.load(stack.copy());
            List<String> upgrades = new ArrayList<>();
            if (data != null && data.upgrades != null) {
                upgrades.addAll(data.upgrades);
            }
            Collections.sort(upgrades);
            return "backpack|up=" + String.join(",", upgrades);
        } catch (Exception ignored) {
            return "backpack|invalid";
        }
    }

    public static Object craftedConsumable(ItemStack stack, UidContext context) {
        // The item id already selects food/potion/scroll model. Dynamic crafted
        // stats and remaining uses should not hide the profession that made it.
        return "crafted_consumable";
    }

    private static String tagString(ItemStack stack, String key) {
        try {
            if (stack == null || !StackNBT.has(stack)) {
                return "";
            }
            CompoundTag tag = StackNBT.get(stack);
            return tag == null ? "" : safe(tag.getStringOr(key, ""));
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean looksLikeGearBase(Item item) {
        try {
            ItemStack def = item.getDefaultInstance();
            return def.getMaxStackSize() <= 1 || def.getMaxDamage() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Fallback used if subtype registration runs before the datapack gear-slot
     * registry is populated. It mirrors only the unambiguous vanilla/mod cases.
     */
    private static boolean fallbackGear(Item item) {
        try {
            if (item instanceof StaffWeapon || item instanceof ItemRing || item instanceof ItemNecklace) {
                return true;
            }
            ItemStack def = item.getDefaultInstance();
            if (def.is(ItemTags.HEAD_ARMOR) || def.is(ItemTags.CHEST_ARMOR)
                || def.is(ItemTags.LEG_ARMOR) || def.is(ItemTags.FOOT_ARMOR)
                || def.is(ItemTags.SWORDS) || def.is(ItemTags.AXES)) {
                return true;
            }
            var key = BuiltInRegistries.ITEM.getKey(item);
            if (key == null) {
                return false;
            }
            String path = key.getPath();
            return path.endsWith("_helmet") || path.endsWith("_chestplate")
                || path.endsWith("_leggings") || path.endsWith("_boots")
                || path.endsWith("_sword") || path.endsWith("_axe")
                || path.equals("bow") || path.endsWith("_bow")
                || path.equals("crossbow") || path.endsWith("_crossbow")
                || path.equals("shield") || path.endsWith("_shield");
        } catch (Exception ignored) {
            return false;
        }
    }
}
