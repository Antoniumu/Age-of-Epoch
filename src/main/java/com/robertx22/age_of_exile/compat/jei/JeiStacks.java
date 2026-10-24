package com.robertx22.age_of_exile.compat.jei;

import com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.database.data.ingredient.SlashIngredient;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.data.runewords.RuneWord;
import com.robertx22.age_of_exile.database.data.unique_items.UniqueGear;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.loot.blueprints.GearBlueprint;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CraftToolItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.ProfessionItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.RuneItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.player_skills.ingredient.data.IngredientData;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.VanillaMaterial;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.RuneItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Builds the ItemStacks that JEI displays. Everything here is best-effort: if the
 * datapack registries are not ready (or a single entry is broken), the helpers
 * return an empty stack instead of throwing during a JEI reload.
 */
public final class JeiStacks {

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
}
