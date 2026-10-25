package com.robertx22.age_of_exile.compat.jei;

import com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.age_of_exile.compat.jei.category.CurrencyRecipe;
import com.robertx22.age_of_exile.compat.jei.category.DisplayRecipe;
import com.robertx22.age_of_exile.compat.jei.category.ProfessionCraftRecipe;
import com.robertx22.age_of_exile.compat.jei.category.RunewordRecipe;
import com.robertx22.age_of_exile.compat.jei.category.SalvageRecipe;
import com.robertx22.age_of_exile.database.data.currency.base.CurrencyItem;
import com.robertx22.age_of_exile.database.data.currency.loc_reqs.BaseLocRequirement;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.gems.Gem;
import com.robertx22.age_of_exile.database.data.ingredient.SlashIngredient;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.data.runes.Rune;
import com.robertx22.age_of_exile.database.data.runewords.RuneWord;
import com.robertx22.age_of_exile.database.data.unique_items.UniqueGear;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.BackpackItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CraftedConsumableItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.GemItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.player_skills.ingredient.items.CraftToolItem;
import com.robertx22.age_of_exile.player_skills.items.backpacks.upgrades.BackpackUpgradeItem;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.enumclasses.LootType;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.uncommon.localization.ItemDescriptions;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.age_of_exile.vanilla_mc.items.crates.gem_crate.LootCrateItem;
import com.robertx22.age_of_exile.vanilla_mc.items.favor.FullFavorItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.GemItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.RuneItem;
import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.database.db_types.RewardList;
import com.robertx22.infinite_dungeons.database.db_types.WeightedReward;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Collects everything the mod knows about its own crafting systems and turns it into
 * JEI pages. Every builder swallows exceptions and simply returns fewer pages, so one
 * broken datapack entry can never break the JEI reload.
 */
public final class JeiRecipes {

    private JeiRecipes() {
    }

    // ------------------------------------------------------------------
    // Small shared helpers
    // ------------------------------------------------------------------

    private static Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private static List<ItemStack> slot(List<ItemStack> stacks) {
        return JeiStacks.clean(stacks);
    }

    private static List<ItemStack> slot(ItemStack... stacks) {
        List<ItemStack> list = new ArrayList<>();
        if (stacks != null) {
            for (ItemStack stack : stacks) {
                if (stack != null && !stack.isEmpty()) {
                    list.add(stack);
                }
            }
        }
        return list;
    }

    private static List<List<ItemStack>> pools(List<ItemStack> stacks) {
        List<List<ItemStack>> list = new ArrayList<>();
        for (ItemStack stack : JeiStacks.clean(stacks)) {
            list.add(List.of(stack));
        }
        return list;
    }

    private static DisplayRecipe display(
        String path,
        List<List<ItemStack>> inputs,
        List<ItemStack> catalyst,
        List<List<ItemStack>> outputs,
        List<Component> lines
    ) {
        return new DisplayRecipe(id(path), inputs, catalyst, outputs, lines);
    }

    private static void addRequirements(List<Component> lines, List<BaseLocRequirement> requirements) {
        try {
            if (requirements == null || requirements.isEmpty()) {
                return;
            }
            lines.add(tr("mmorpg.jei.currency.requirements"));
            for (BaseLocRequirement req : requirements) {
                lines.add(Component.literal(" • ").append(req.getText()));
            }
        } catch (Exception ignored) {
            // Requirements are explanatory only; a page is still useful without them.
        }
    }

    private static List<Integer> allTiers() {
        try {
            return new ArrayList<>(new TreeSet<>(LevelUtils.getAllTiers()));
        } catch (Exception e) {
            return List.of(JeiStacks.sampleTier());
        }
    }

    private static List<ItemStack> registeredStacks(List<? extends RegObj<? extends Item>> objects) {
        List<ItemStack> list = new ArrayList<>();
        try {
            for (RegObj<? extends Item> obj : objects) {
                Item item = obj.get();
                if (item != null && item != Items.AIR) {
                    list.add(new ItemStack(item));
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Profession crafting: ingredient + optional gear -> result
    // ------------------------------------------------------------------

    public static List<ProfessionCraftRecipe> professionCrafts() {
        List<ProfessionCraftRecipe> list = new ArrayList<>();
        int tier = JeiStacks.sampleTier();
        try {
            for (PlayerSkillEnum skill : PlayerSkillEnum.getAll()) {
                ItemStack tool = JeiStacks.craftTool(skill);
                if (tool.isEmpty()) {
                    continue;
                }

                List<ItemStack> gear = skill.isGearCraftingProf()
                    ? JeiStacks.gearBasesForProfession(skill)
                    : List.of();
                List<ItemStack> results = skill.isGearCraftingProf()
                    ? JeiStacks.professionGearSamples(skill, tier)
                    : consumableResults(skill);
                if (results.isEmpty() || (skill.isGearCraftingProf() && gear.isEmpty())) {
                    continue;
                }
                List<Component> lines = professionLines(skill);

                for (SlashIngredient ing : ExileDB.Ingredients().getList()) {
                    try {
                        if (!ing.isAllowedInProfession(skill.id)) {
                            continue;
                        }
                        List<ItemStack> variants = JeiStacks.ingredientVariants(ing);
                        if (variants.isEmpty()) {
                            continue;
                        }
                        list.add(new ProfessionCraftRecipe(
                            id("prof_craft/" + skill.id + "/" + ing.GUID()),
                            gear,
                            variants,
                            results,
                            tool,
                            lines
                        ));
                    } catch (Exception e) {
                        // skip this ingredient only
                    }
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    private static List<ItemStack> consumableResults(PlayerSkillEnum skill) {
        if (skill == PlayerSkillEnum.COOKING) {
            return registeredStacks(CraftedConsumableItems.FOODS);
        }
        if (skill == PlayerSkillEnum.ALCHEMY) {
            return registeredStacks(CraftedConsumableItems.POTIONS);
        }
        if (skill == PlayerSkillEnum.INSCRIBING) {
            return registeredStacks(CraftedConsumableItems.SCROLLS);
        }
        return List.of();
    }

    private static List<Component> professionLines(PlayerSkillEnum skill) {
        List<Component> lines = new ArrayList<>();
        lines.add(tr("mmorpg.jei.prof.tool", skill.word.locName()));
        lines.add(tr("mmorpg.jei.prof.grid"));
        lines.add(tr("mmorpg.jei.prof.success"));
        lines.add(tr("mmorpg.jei.prof.multiplier"));
        lines.add(tr("mmorpg.jei.prof.skill_multiplier", formatMultiplier(skill.craftedStatMulti)));
        if (skill.isGearCraftingProf()) {
            lines.add(tr("mmorpg.jei.prof.result_gear"));
        } else {
            lines.add(tr("mmorpg.jei.prof.result_consumable"));
            int uses = craftingUses(skill);
            if (uses > 0) {
                lines.add(tr("mmorpg.jei.prof.uses", uses));
            }
        }
        return lines;
    }

    private static String formatMultiplier(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001F) {
            return "×" + Math.round(value);
        }
        return "×" + value;
    }

    private static int craftingUses(PlayerSkillEnum skill) {
        if (skill == PlayerSkillEnum.COOKING) {
            return 3;
        }
        if (skill == PlayerSkillEnum.ALCHEMY) {
            return 6;
        }
        if (skill == PlayerSkillEnum.INSCRIBING) {
            return 3;
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Runewords
    // ------------------------------------------------------------------

    public static List<RunewordRecipe> runewords() {
        List<RunewordRecipe> list = new ArrayList<>();
        try {
            for (RuneWord word : ExileDB.RuneWords().getList()) {
                try {
                    List<ItemStack> runes = runeStacks(word);
                    if (runes.isEmpty() || runes.size() != word.runes.size()) {
                        continue;
                    }

                    Set<Item> seen = new LinkedHashSet<>();
                    List<ItemStack> gear = new ArrayList<>();
                    for (String slot : word.slots) {
                        for (Item item : JeiStacks.gearBaseItems(slot)) {
                            if (seen.add(item)) {
                                gear.add(new ItemStack(item));
                            }
                        }
                    }
                    List<ItemStack> results = JeiStacks.runewordResults(word);
                    if (gear.isEmpty() || results.isEmpty()) {
                        continue;
                    }

                    List<Component> lines = new ArrayList<>();
                    UniqueGear unique = ExileDB.UniqueGears().get(word.uniq_id);
                    if (unique != null) {
                        lines.add(tr("mmorpg.jei.runeword.result", unique.locName()));
                    }
                    lines.add(tr("mmorpg.jei.runeword.runes", String.join(" + ", word.runes)));
                    lines.add(tr("mmorpg.jei.runeword.slots", slotNames(word.slots)));
                    lines.add(tr("mmorpg.jei.runeword.station"));

                    list.add(new RunewordRecipe(
                        id("runeword/" + word.GUID()),
                        runes,
                        gear,
                        results,
                        lines
                    ));
                } catch (Exception e) {
                    // skip this runeword only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    private static String slotNames(List<String> slots) {
        List<String> names = new ArrayList<>();
        for (String slot : slots) {
            String name = slot;
            try {
                GearSlot gearSlot = ExileDB.GearSlots().get(slot);
                if (gearSlot != null) {
                    name = gearSlot.locName().getString();
                }
            } catch (Exception ignored) {
            }
            names.add(name);
        }
        return String.join(", ", names);
    }

    private static List<ItemStack> runeStacks(RuneWord word) {
        List<ItemStack> runes = new ArrayList<>();
        for (String runeId : word.runes) {
            RuneItem.RuneType type = runeType(runeId);
            if (type == null) {
                return new ArrayList<>();
            }
            ItemStack stack = JeiStacks.rune(type);
            if (stack.isEmpty()) {
                return new ArrayList<>();
            }
            runes.add(stack);
        }
        return runes;
    }

    private static RuneItem.RuneType runeType(String id) {
        for (RuneItem.RuneType type : RuneItem.RuneType.values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Salvaging gear, souls and rarity essence
    // ------------------------------------------------------------------

    public static List<SalvageRecipe> salvages() {
        List<SalvageRecipe> list = new ArrayList<>();
        int tier = JeiStacks.sampleTier();
        try {
            for (GearRarity rarity : JeiStacks.normalRarities()) {
                try {
                    List<ItemStack> gear = JeiStacks.allGearSamples(rarity, tier);
                    List<List<ItemStack>> outputs = salvageOutputs(rarity);
                    if (gear.isEmpty() || outputs.isEmpty()) {
                        continue;
                    }
                    List<Component> lines = salvageLines(rarity);
                    lines.add(0, tr("mmorpg.jei.salvage.gear"));
                    list.add(new SalvageRecipe(id("salvage/gear/" + rarity.GUID()), gear, outputs, lines));
                } catch (Exception e) {
                    // skip this rarity only
                }

                try {
                    List<ItemStack> souls = JeiStacks.soulsForRarity(rarity, tier, true);
                    List<List<ItemStack>> outputs = salvageOutputs(rarity);
                    if (souls.isEmpty() || outputs.isEmpty()) {
                        continue;
                    }
                    List<Component> lines = salvageLines(rarity);
                    lines.add(0, tr("mmorpg.jei.salvage.soul"));
                    list.add(new SalvageRecipe(id("salvage/soul/" + rarity.GUID()), souls, outputs, lines));
                } catch (Exception e) {
                    // skip this rarity only
                }
            }

            List<ItemStack> uniqueGear = JeiStacks.allUniqueSamples(tier);
            List<List<ItemStack>> uniqueOutputs = uniqueSalvageOutputs();
            if (!uniqueGear.isEmpty() && !uniqueOutputs.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.salvage.unique_gear"));
                lines.add(tr("mmorpg.jei.salvage.hammer"));
                lines.add(tr("mmorpg.jei.salvage.depends_on_tier"));
                lines.add(tr("mmorpg.jei.salvage.only_salvageable"));
                list.add(new SalvageRecipe(id("salvage/unique_gear"), uniqueGear, uniqueOutputs, lines));
            }

            List<ItemStack> uniqueSouls = new ArrayList<>();
            for (UniqueGear unique : ExileDB.UniqueGears().getList()) {
                uniqueSouls.add(JeiStacks.uniqueSoul(unique, tier, false));
                uniqueSouls.add(JeiStacks.uniqueSoul(unique, tier, true));
            }
            uniqueSouls = JeiStacks.clean(uniqueSouls);
            if (!uniqueSouls.isEmpty() && !uniqueOutputs.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.salvage.unique_soul"));
                lines.add(tr("mmorpg.jei.salvage.hammer"));
                lines.add(tr("mmorpg.jei.salvage.depends_on_tier"));
                lines.add(tr("mmorpg.jei.salvage.only_salvageable"));
                list.add(new SalvageRecipe(id("salvage/unique_soul"), uniqueSouls, uniqueOutputs, lines));
            }

            for (GearRarity rarity : ExileDB.GearRarities().getList()) {
                try {
                    if (rarity == null || rarity.rar_ess_per_sal <= 0) {
                        continue;
                    }
                    ItemStack essence = rarity.getRarityEssenceStack();
                    essence.setCount(1);
                    int count = Math.max(1, (rarity.dust_per_sal.min + rarity.dust_per_sal.max) / 2);
                    ItemStack source = new ItemStack(SlashItems.SOURCE_OF_STRENGTH.get(), count);
                    List<Component> lines = new ArrayList<>();
                    lines.add(tr("mmorpg.jei.salvage.essence"));
                    lines.add(tr("mmorpg.jei.salvage.amount", rarity.dust_per_sal.min, rarity.dust_per_sal.max));
                    list.add(new SalvageRecipe(
                        id("salvage/essence/" + rarity.GUID()),
                        slot(essence),
                        List.of(slot(source)),
                        lines
                    ));
                } catch (Exception e) {
                    // skip this rarity only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    private static List<Component> salvageLines(GearRarity rarity) {
        List<Component> lines = new ArrayList<>();
        lines.add(tr("mmorpg.jei.salvage.hammer"));
        lines.add(tr("mmorpg.jei.salvage.amount", rarity.dust_per_sal.min, rarity.dust_per_sal.max));
        lines.add(tr("mmorpg.jei.salvage.depends_on_tier"));
        lines.add(tr("mmorpg.jei.salvage.only_salvageable"));
        return lines;
    }

    private static List<ItemStack> dustVariants(int count) {
        List<ItemStack> list = new ArrayList<>();
        for (int tier : allTiers()) {
            ItemStack dust = JeiStacks.dust(tier, count);
            if (!dust.isEmpty()) {
                list.add(dust);
            }
        }
        return list;
    }

    private static List<List<ItemStack>> salvageOutputs(GearRarity rarity) {
        List<List<ItemStack>> outputs = new ArrayList<>();
        int representative = Math.max(1, (rarity.dust_per_sal.min + rarity.dust_per_sal.max) / 2);
        List<ItemStack> dusts = dustVariants(representative);
        if (!dusts.isEmpty()) {
            outputs.add(dusts);
        }
        if (rarity.rar_ess_per_sal > 0) {
            ItemStack essence = rarity.getRarityEssenceStack();
            essence.setCount(rarity.rar_ess_per_sal);
            outputs.add(slot(essence));
        }
        return outputs;
    }

    private static List<List<ItemStack>> uniqueSalvageOutputs() {
        List<List<ItemStack>> outputs = new ArrayList<>();
        List<ItemStack> dusts = dustVariants(1);
        if (!dusts.isEmpty()) {
            outputs.add(dusts);
        }
        List<ItemStack> essences = new ArrayList<>();
        try {
            for (GearRarity rarity : ExileDB.GearRarities().getList()) {
                if (rarity != null && rarity.is_unique_item && rarity.rar_ess_per_sal > 0) {
                    ItemStack essence = rarity.getRarityEssenceStack();
                    essence.setCount(rarity.rar_ess_per_sal);
                    essences.add(essence);
                }
            }
        } catch (Exception ignored) {
        }
        essences = JeiStacks.clean(essences);
        if (!essences.isEmpty()) {
            outputs.add(essences);
        }
        return outputs;
    }

    // ------------------------------------------------------------------
    // Currency orbs ("drag onto gear")
    // ------------------------------------------------------------------

    public static List<CurrencyRecipe> currencies() {
        List<CurrencyRecipe> list = new ArrayList<>();
        try {
            for (CurrencyItem currency : ExileDB.CurrencyItems().getList()) {
                try {
                    List<ItemStack> gear = JeiStacks.currencyGearSamples(currency);
                    if (gear.isEmpty()) {
                        continue;
                    }
                    List<Component> lines = new ArrayList<>();
                    lines.add(currency.locDesc());
                    lines.add(tr("mmorpg.jei.currency.use"));
                    addRequirements(lines, currency.requirements());
                    if (currency.getInstability() > 0) {
                        lines.add(tr("mmorpg.jei.currency.instability", (int) currency.getInstability()));
                    }
                    if (currency.getBreakChance() > 0) {
                        lines.add(tr("mmorpg.jei.currency.break_chance", (int) currency.getBreakChance()));
                    }

                    list.add(new CurrencyRecipe(
                        id("currency/" + currency.GUID()),
                        gear,
                        new ItemStack(currency),
                        JeiStacks.copies(gear),
                        lines
                    ));
                } catch (Exception e) {
                    // skip this currency only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Stat souls: soul + empty physical base -> generated gear
    // ------------------------------------------------------------------

    public static List<DisplayRecipe> statSouls() {
        List<DisplayRecipe> list = new ArrayList<>();
        int tier = JeiStacks.sampleTier();
        try {
            for (GearSlot slotObj : ExileDB.GearSlots().getList()) {
                try {
                    String slot = slotObj.GUID();
                    List<ItemStack> souls = JeiStacks.soulsForSlot(slot, tier, true);
                    List<ItemStack> bases = JeiStacks.gearBases(slot);
                    List<ItemStack> results = new ArrayList<>();
                    for (GearRarity rarity : JeiStacks.normalRarities()) {
                        results.addAll(JeiStacks.gearSamples(slot, rarity, tier));
                    }
                    if (souls.isEmpty() || bases.isEmpty() || results.isEmpty()) {
                        continue;
                    }
                    List<Component> lines = new ArrayList<>();
                    lines.add(tr("mmorpg.jei.soul.apply"));
                    lines.add(tr("mmorpg.jei.soul.slot", slotObj.locName()));
                    lines.add(tr("mmorpg.jei.soul.variants"));
                    lines.add(tr("mmorpg.jei.soul.result"));
                    list.add(display(
                        "stat_soul/" + slot,
                        List.of(slot(souls), slot(bases)),
                        List.of(),
                        List.of(slot(results)),
                        lines
                    ));
                } catch (Exception e) {
                    // skip this slot only
                }
            }

            List<ItemStack> anySouls = JeiStacks.soulsForSlot("", tier, true);
            List<ItemStack> allBases = JeiStacks.allGearBases();
            List<ItemStack> allResults = JeiStacks.allNormalGearSamples(tier);
            if (!anySouls.isEmpty() && !allBases.isEmpty() && !allResults.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.soul.apply"));
                lines.add(tr("mmorpg.jei.soul.any_slot"));
                lines.add(tr("mmorpg.jei.soul.variants"));
                lines.add(tr("mmorpg.jei.soul.result"));
                list.add(display(
                    "stat_soul/any_slot",
                    List.of(slot(anySouls), slot(allBases)),
                    List.of(),
                    List.of(slot(allResults)),
                    lines
                ));
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Identify souls and the common soul producer
    // ------------------------------------------------------------------

    public static List<DisplayRecipe> soulIdentify() {
        List<DisplayRecipe> list = new ArrayList<>();
        int tier = JeiStacks.sampleTier();
        try {
            List<ItemStack> unidentified = JeiStacks.allSouls(tier, false);
            List<ItemStack> identified = JeiStacks.allSouls(tier, true);
            ItemStack tome = new ItemStack(SlashItems.IDENTIFY_TOME.get());
            if (!unidentified.isEmpty() && !identified.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.identify.use"));
                lines.add(tr("mmorpg.jei.identify.result"));
                lines.add(tr("mmorpg.jei.identify.still_soul"));
                list.add(display(
                    "soul_identify/identify_tome",
                    List.of(slot(unidentified), slot(tome)),
                    List.of(),
                    List.of(slot(identified)),
                    lines
                ));
            }

            List<ItemStack> produced = new ArrayList<>();
            for (GearRarity rarity : ExileDB.GearRarities().getList()) {
                if (rarity != null && rarity.item_tier == 0) {
                    produced.add(JeiStacks.soul("", rarity, tier, null, false, false));
                }
            }
            produced = JeiStacks.clean(produced);
            ItemStack producer = new ItemStack(SlashItems.COMMON_SOUL_PRODUCE.get());
            if (!produced.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.producer.use"));
                lines.add(tr("mmorpg.jei.producer.tier"));
                lines.add(tr("mmorpg.jei.producer.no_salvage"));
                list.add(display(
                    "soul_identify/common_producer",
                    List.of(slot(producer)),
                    List.of(),
                    List.of(slot(produced)),
                    lines
                ));
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Gems: socket, rank up and extract
    // ------------------------------------------------------------------

    public static List<DisplayRecipe> gems() {
        List<DisplayRecipe> list = new ArrayList<>();
        try {
            ItemStack extractor = new ItemStack(SlashItems.SOCKET_EXTRACTOR.get());
            for (RegObj<GemItem> obj : GemItems.ALL) {
                try {
                    GemItem gemItem = obj.get();
                    if (gemItem == null) {
                        continue;
                    }
                    Gem gem = gemItem.getGem();
                    if (!validGem(gem)) {
                        continue;
                    }

                    List<ItemStack> gear = JeiStacks.gemSocketGearSamples(gemItem);
                    List<ItemStack> socketed = JeiStacks.withSocket(gear, gem);
                    if (!gear.isEmpty() && !socketed.isEmpty()) {
                        List<Component> lines = new ArrayList<>();
                        lines.add(tr("mmorpg.jei.gem.socket"));
                        lines.add(tr("mmorpg.jei.gem.no_duplicate"));
                        addRequirements(lines, gemItem.requirements());
                        list.add(display(
                            "gem/socket/" + gemItem.GUID(),
                            List.of(slot(gear), slot(new ItemStack(gemItem))),
                            List.of(),
                            List.of(slot(socketed)),
                            lines
                        ));

                        List<Component> extractLines = new ArrayList<>();
                        extractLines.add(tr("mmorpg.jei.gem.extract"));
                        extractLines.add(tr("mmorpg.jei.gem.extract_first"));
                        list.add(display(
                            "gem/extract/" + gemItem.GUID(),
                            List.of(slot(socketed)),
                            JeiStacks.one(extractor.copy()),
                            List.of(slot(JeiStacks.copies(gear)), slot(new ItemStack(gemItem))),
                            extractLines
                        ));
                    }

                    if (gem.hasHigherTierGem()) {
                        Gem higher = gem.getHigherTierGem();
                        ItemStack output = JeiStacks.gem(higher);
                        if (!output.isEmpty()) {
                            int chance = gem.perc_upgrade_chance > 0 ? gem.perc_upgrade_chance : gemItem.gemRank.upgradeChance;
                            List<Component> lines = new ArrayList<>();
                            lines.add(tr("mmorpg.jei.gem.upgrade"));
                            lines.add(tr("mmorpg.jei.gem.upgrade_chance", chance));
                            lines.add(tr("mmorpg.jei.gem.upgrade_fail"));
                            list.add(display(
                                "gem/upgrade/" + gemItem.GUID(),
                                List.of(slot(new ItemStack(gemItem, 3))),
                                List.of(),
                                List.of(slot(output)),
                                lines
                            ));
                        }
                    }
                } catch (Exception e) {
                    // skip this gem only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    private static boolean validGem(Gem gem) {
        try {
            return gem != null && gem.identifier != null && !gem.identifier.isEmpty()
                && ExileDB.Gems().isRegistered(gem.identifier);
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Rarity essence -> stone -> higher-rarity gear
    // ------------------------------------------------------------------

    public static List<DisplayRecipe> rarityUpgrades() {
        List<DisplayRecipe> list = new ArrayList<>();
        int tier = JeiStacks.sampleTier();
        try {
            for (GearRarity rarity : ExileDB.GearRarities().getList()) {
                try {
                    if (rarity == null) {
                        continue;
                    }
                    if (rarity.rar_ess_per_sal > 0) {
                        ItemStack essence = rarity.getRarityEssenceStack();
                        essence.setCount(9);
                        ItemStack stone = rarity.getRarityUpgradeStack();
                        List<Component> lines = new ArrayList<>();
                        lines.add(tr("mmorpg.jei.rarity.combine"));
                        lines.add(tr("mmorpg.jei.rarity.combine_use"));
                        list.add(display(
                            "rarity/combine/" + rarity.GUID(),
                            List.of(slot(essence)),
                            List.of(),
                            List.of(slot(stone)),
                            lines
                        ));
                    }

                    if (!rarity.canUpgradeRarityIfUpgradeLevelIsHighEnough()) {
                        continue;
                    }
                    GearRarity higher = rarity.getHigherRarity();
                    if (higher == null) {
                        continue;
                    }
                    int threshold = rarity.upgrade_lvl_to_increase_rar;
                    List<ItemStack> before = JeiStacks.withUpgradeLevel(JeiStacks.allGearSamples(rarity, tier), threshold);
                    List<ItemStack> after = JeiStacks.withUpgradeLevel(JeiStacks.allGearSamples(higher, tier), threshold);
                    if (before.isEmpty() || after.isEmpty()) {
                        continue;
                    }
                    List<Component> lines = new ArrayList<>();
                    lines.add(tr("mmorpg.jei.rarity.threshold", threshold));
                    lines.add(tr("mmorpg.jei.rarity.preserve"));
                    int extraAffixes = higher.affixes - rarity.affixes;
                    if (extraAffixes > 0) {
                        lines.add(tr("mmorpg.jei.rarity.affixes", extraAffixes));
                    }
                    list.add(display(
                        "rarity/upgrade/" + rarity.GUID(),
                        List.of(slot(before), slot(rarity.getRarityUpgradeStack())),
                        List.of(),
                        List.of(slot(after)),
                        lines
                    ));
                } catch (Exception e) {
                    // skip this rarity only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Repair with salvaged dust
    // ------------------------------------------------------------------

    public static List<DisplayRecipe> repairs() {
        List<DisplayRecipe> list = new ArrayList<>();
        int tier = JeiStacks.sampleTier();
        try {
            List<ItemStack> dataGear = new ArrayList<>(JeiStacks.allNormalGearSamples(tier));
            dataGear.addAll(JeiStacks.allUniqueSamples(tier));
            List<ItemStack> damagedInputs = new ArrayList<>(JeiStacks.damagedCopies(JeiStacks.allGearBases()));
            damagedInputs.addAll(JeiStacks.damagedCopies(dataGear));
            List<ItemStack> repairedOutputs = JeiStacks.repairedCopies(dataGear);
            if (damagedInputs.isEmpty() || repairedOutputs.isEmpty()) {
                return list;
            }

            for (SkillItemTier skillTier : SkillItemTier.values()) {
                try {
                    ItemStack dust = JeiStacks.dust(skillTier.tier, 1);
                    if (dust.isEmpty()) {
                        continue;
                    }
                    List<Component> lines = new ArrayList<>();
                    lines.add(tr("mmorpg.jei.repair.use"));
                    lines.add(tr("mmorpg.jei.repair.amount", skillTier.repairDurab));
                    lines.add(tr("mmorpg.jei.repair.penalty"));
                    list.add(display(
                        "repair/dust_" + skillTier.tier,
                        List.of(slot(damagedInputs), slot(dust)),
                        List.of(),
                        List.of(slot(repairedOutputs)),
                        lines
                    ));
                } catch (Exception e) {
                    // skip this dust tier only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Backpack upgrades (items themselves are ordinary JSON recipes)
    // ------------------------------------------------------------------

    public static List<DisplayRecipe> backpackUpgrades() {
        List<DisplayRecipe> list = new ArrayList<>();
        try {
            for (RegObj<BackpackUpgradeItem> obj : BackpackItems.ALL) {
                try {
                    BackpackUpgradeItem upgrade = obj.get();
                    if (upgrade == null) {
                        continue;
                    }
                    BackpackUpgradeItem previous = previousUpgrade(upgrade);
                    ItemStack before = previous == null
                        ? JeiStacks.backpack()
                        : JeiStacks.backpackWith(previous);
                    ItemStack after = JeiStacks.backpackAfterUpgrade(before, upgrade);
                    if (before.isEmpty() || after.isEmpty()) {
                        continue;
                    }
                    List<Component> lines = new ArrayList<>();
                    lines.add(tr("mmorpg.jei.backpack.apply"));
                    lines.add(tr("mmorpg.jei.backpack.rules"));
                    lines.add(tr("mmorpg.jei.backpack.limit", 10));
                    list.add(display(
                        "backpack/" + upgrade.GUID(),
                        List.of(slot(before), slot(new ItemStack(upgrade))),
                        List.of(),
                        List.of(slot(after)),
                        lines
                    ));
                } catch (Exception e) {
                    // skip this upgrade only
                }
            }
        } catch (Exception e) {
            // item registry is not ready yet
        }
        return list;
    }

    private static BackpackUpgradeItem previousUpgrade(BackpackUpgradeItem current) {
        try {
            if (current == null || current.upgrade == null || !current.upgrade.canOverrideSelf()
                || current.tier == null || current.tier.tier <= 0) {
                return null;
            }
            for (RegObj<BackpackUpgradeItem> obj : BackpackItems.ALL) {
                BackpackUpgradeItem candidate = obj.get();
                if (candidate != null && candidate.upgrade == current.upgrade
                    && candidate.tier != null && candidate.tier.tier == current.tier.tier - 1) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Crates: the output grid is a random pool, not "all at once"
    // ------------------------------------------------------------------

    public static List<DisplayRecipe> crates() {
        List<DisplayRecipe> list = new ArrayList<>();
        int tier = JeiStacks.sampleTier();
        try {
            for (LootType type : LootCrateItem.LOOT_TYPES) {
                for (int crateTier : allTiers()) {
                    try {
                        List<ItemStack> outputs = lootCrateOutputs(type, crateTier);
                        ItemStack crate = JeiStacks.lootCrate(type, crateTier);
                        if (crate.isEmpty() || outputs.isEmpty()) {
                            continue;
                        }
                        List<Component> lines = new ArrayList<>();
                        lines.add(tr("mmorpg.jei.crate.open"));
                        lines.add(tr("mmorpg.jei.crate.random_one"));
                        lines.add(tr("mmorpg.jei.crate.pool", type.word.locName()));
                        lines.add(tr("mmorpg.jei.crate.tier", crateTier));
                        list.add(display(
                            "crate/loot/" + type.GUID() + "_" + crateTier,
                            List.of(slot(crate)),
                            List.of(),
                            pools(outputs),
                            lines
                        ));
                    } catch (Exception e) {
                        // skip this crate variant only
                    }
                }
            }

            for (GearRarity rarity : JeiStacks.normalRarities()) {
                try {
                    ItemStack crate = JeiStacks.rarityCrate(rarity);
                    List<ItemStack> souls = JeiStacks.soulsForRarity(rarity, tier, false);
                    if (crate.isEmpty() || souls.isEmpty()) {
                        continue;
                    }
                    List<Component> lines = new ArrayList<>();
                    lines.add(tr("mmorpg.jei.crate.open"));
                    lines.add(tr("mmorpg.jei.crate.random_one"));
                    lines.add(tr("mmorpg.jei.rarity_crate.player_tier"));
                    lines.add(tr("mmorpg.jei.rarity_crate.random_slot"));
                    list.add(display(
                        "crate/rarity/" + rarity.GUID(),
                        List.of(slot(crate)),
                        List.of(),
                        List.of(slot(souls)),
                        lines
                    ));
                } catch (Exception e) {
                    // skip this rarity only
                }
            }

            for (RewardList rewardList : DungeonsDB.RewardLists().getList()) {
                try {
                    ItemStack crate = JeiStacks.rewardCrate(rewardList);
                    if (crate.isEmpty()) {
                        continue;
                    }
                    List<ItemStack> outputs = new ArrayList<>();
                    int totalWeight = 0;
                    ItemStack jackpotStack = ItemStack.EMPTY;
                    int jackpotWeight = 0;

                    if (rewardList.rewards != null) {
                        for (WeightedReward reward : rewardList.rewards) {
                            try {
                                if (reward == null || reward.reward == null || reward.reward.type == null) {
                                    continue;
                                }
                                ItemStack stack = reward.reward.type.getStack(reward.reward);
                                if (stack != null && !stack.isEmpty()) {
                                    outputs.add(stack);
                                }
                                if (reward.weight > 0) {
                                    totalWeight += reward.weight;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    try {
                        WeightedReward jackpot = rewardList.jackpot;
                        if (jackpot != null && jackpot.reward != null && jackpot.reward.type != null) {
                            jackpotStack = jackpot.reward.type.getStack(jackpot.reward);
                            if (jackpotStack != null && !jackpotStack.isEmpty()) {
                                outputs.add(jackpotStack.copy());
                            }
                            jackpotWeight = Math.max(0, jackpot.weight);
                            totalWeight += jackpotWeight;
                        }
                    } catch (Exception ignored) {
                    }
                    outputs = JeiStacks.clean(outputs);
                    if (outputs.isEmpty()) {
                        continue;
                    }

                    List<Component> lines = new ArrayList<>();
                    lines.add(tr("mmorpg.jei.crate.open"));
                    lines.add(tr("mmorpg.jei.crate.weighted"));
                    lines.add(tr("mmorpg.jei.crate.reward_list", rewardList.getTranslatable()));
                    if (jackpotStack != null && !jackpotStack.isEmpty() && totalWeight > 0 && jackpotWeight > 0) {
                        long percent = Math.round(100.0 * jackpotWeight / totalWeight);
                        lines.add(tr("mmorpg.jei.crate.jackpot", jackpotStack.getHoverName(), percent));
                    }
                    list.add(display(
                        "crate/reward/" + rewardList.GUID(),
                        List.of(slot(crate)),
                        List.of(),
                        pools(outputs),
                        lines
                    ));
                } catch (Exception e) {
                    // skip this reward list only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    private static List<ItemStack> lootCrateOutputs(LootType type, int crateTier) {
        List<ItemStack> outputs = new ArrayList<>();
        if (type == LootType.Gem) {
            for (Gem gem : ExileDB.Gems().getList()) {
                if (gem != null && gem.tier == crateTier) {
                    outputs.add(JeiStacks.gem(gem));
                }
            }
        } else if (type == LootType.Rune) {
            for (Rune rune : ExileDB.Runes().getList()) {
                if (rune != null && crateTier >= rune.tier) {
                    outputs.add(JeiStacks.stack(rune.getItem()));
                }
            }
        } else if (type == LootType.Currency) {
            for (CurrencyItem currency : ExileDB.CurrencyItems().getList()) {
                if (currency != null && crateTier >= currency.getTier()) {
                    outputs.add(new ItemStack(currency));
                }
            }
            if (outputs.isEmpty()) {
                // Mirrors the real crate: if no currency passes its tier filter,
                // it falls back to the full currency registry.
                for (CurrencyItem currency : ExileDB.CurrencyItems().getList()) {
                    if (currency != null) {
                        outputs.add(new ItemStack(currency));
                    }
                }
            }
        }
        return JeiStacks.clean(outputs);
    }

    // ------------------------------------------------------------------
    // Unique gear: a registry-driven catalogue
    // ------------------------------------------------------------------

    public static List<DisplayRecipe> uniqueGear() {
        List<DisplayRecipe> list = new ArrayList<>();
        int tier = JeiStacks.sampleTier();
        try {
            for (UniqueGear unique : ExileDB.UniqueGears().getList()) {
                try {
                    GearSlot slotObj = unique.getSlot();
                    if (slotObj == null) {
                        continue;
                    }
                    List<ItemStack> souls = JeiStacks.clean(List.of(
                        JeiStacks.uniqueSoul(unique, tier, false),
                        JeiStacks.uniqueSoul(unique, tier, true)
                    ));
                    List<ItemStack> bases = JeiStacks.gearBases(slotObj.GUID());
                    List<ItemStack> results = JeiStacks.uniqueSamples(unique, tier);
                    if (souls.isEmpty() || bases.isEmpty() || results.isEmpty()) {
                        continue;
                    }

                    List<Component> lines = new ArrayList<>();
                    lines.add(tr("mmorpg.jei.unique.result", unique.locName()));
                    GearRarity rarity = unique.getUniqueRarity();
                    if (rarity != null) {
                        lines.add(tr("mmorpg.jei.unique.slot", slotObj.locName(), rarity.locName()));
                    }
                    lines.add(tr("mmorpg.jei.unique.apply"));
                    try {
                        lines.add(tr("mmorpg.jei.unique.stats", unique.uniqueStats().size()));
                    } catch (Exception ignored) {
                    }

                    list.add(display(
                        "unique/" + unique.GUID(),
                        List.of(slot(souls), slot(bases)),
                        List.of(),
                        List.of(slot(results)),
                        lines
                    ));
                } catch (Exception e) {
                    // skip this unique only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Right-click item actions that are real transformations
    // ------------------------------------------------------------------

    public static List<DisplayRecipe> itemActions() {
        List<DisplayRecipe> list = new ArrayList<>();
        try {
            ItemStack emptyFavor = new ItemStack(SlashItems.EMPTY_FAVOR.get());
            ItemStack fullFavor = new ItemStack(SlashItems.FULL_FAVOR.get());

            List<Component> fillLines = new ArrayList<>();
            fillLines.add(tr("mmorpg.jei.favor.fill_level", 20));
            fillLines.add(tr("mmorpg.jei.favor.fill_cost", FullFavorItem.FAVOR_CAPACITY));
            fillLines.add(tr("mmorpg.jei.favor.right_click"));
            list.add(display(
                "item_action/fill_favor",
                List.of(slot(emptyFavor)),
                List.of(),
                List.of(slot(fullFavor.copy())),
                fillLines
            ));

            List<Component> useLines = new ArrayList<>();
            useLines.add(tr("mmorpg.jei.favor.use", FullFavorItem.FAVOR_CAPACITY));
            useLines.add(tr("mmorpg.jei.favor.right_click"));
            list.add(display(
                "item_action/use_favor",
                List.of(slot(fullFavor)),
                List.of(),
                List.of(),
                useLines
            ));

            GearRarity common = ExileDB.GearRarities().get(IRarity.COMMON_ID);
            ItemStack staff = JeiStacks.gearForSlot(GearSlots.STAFF);
            ItemStack soul = common == null ? ItemStack.EMPTY
                : JeiStacks.soul(GearSlots.STAFF, common, 0, null, false, false);
            ItemStack bag = new ItemStack(SlashItems.NEWBIE_GEAR_BAG.get());
            if (!staff.isEmpty() && !soul.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.newbie.open"));
                lines.add(tr("mmorpg.jei.newbie.contents"));
                list.add(display(
                    "item_action/newbie_bag",
                    List.of(slot(bag)),
                    List.of(),
                    List.of(slot(staff), slot(soul)),
                    lines
                ));
            }
        } catch (Exception e) {
            // item registry is not ready yet
        }
        return list;
    }

    // ------------------------------------------------------------------
    // World sources: source icons are crafting stations, drops are pools
    // ------------------------------------------------------------------

    public static List<DisplayRecipe> worldSources() {
        List<DisplayRecipe> list = new ArrayList<>();
        int tier = JeiStacks.sampleTier();
        try {
            List<ItemStack> sources = sourceIcons();

            List<ItemStack> souls = JeiStacks.allSouls(tier, false);
            if (!souls.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.world.gear_souls"));
                lines.add(tr("mmorpg.jei.world.sources"));
                lines.add(tr("mmorpg.jei.world.tier_rarity"));
                list.add(display("world/gear_souls", List.of(), sources, List.of(slot(souls)), lines));
            }

            List<ItemStack> currencies = new ArrayList<>();
            for (CurrencyItem currency : ExileDB.CurrencyItems().getList()) {
                currencies.add(new ItemStack(currency));
            }
            currencies = JeiStacks.clean(currencies);
            if (!currencies.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.world.currency"));
                lines.add(tr("mmorpg.jei.world.sources"));
                lines.add(tr("mmorpg.jei.world.currency_conditions"));
                list.add(display("world/currency", List.of(), sources, pools(currencies), lines));
            }

            List<ItemStack> gemCrates = new ArrayList<>();
            List<ItemStack> runeCrates = new ArrayList<>();
            for (int crateTier : allTiers()) {
                gemCrates.add(JeiStacks.lootCrate(LootType.Gem, crateTier));
                runeCrates.add(JeiStacks.lootCrate(LootType.Rune, crateTier));
            }
            gemCrates = JeiStacks.clean(gemCrates);
            runeCrates = JeiStacks.clean(runeCrates);
            if (!gemCrates.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.world.gem_crates"));
                lines.add(tr("mmorpg.jei.world.sources"));
                lines.add(tr("mmorpg.jei.world.open_crate"));
                list.add(display("world/gem_crates", List.of(), sources, List.of(slot(gemCrates)), lines));
            }
            if (!runeCrates.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.world.rune_crates"));
                lines.add(tr("mmorpg.jei.world.sources"));
                lines.add(tr("mmorpg.jei.world.open_crate"));
                list.add(display("world/rune_crates", List.of(), sources, List.of(slot(runeCrates)), lines));
            }

            List<List<ItemStack>> ingredientPools = new ArrayList<>();
            for (SlashIngredient ingredient : ExileDB.Ingredients().getList()) {
                List<ItemStack> variants = JeiStacks.ingredientVariants(ingredient);
                if (!variants.isEmpty()) {
                    ingredientPools.add(slot(variants));
                }
            }
            if (!ingredientPools.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.world.ingredients"));
                lines.add(tr("mmorpg.jei.world.sources"));
                lines.add(tr("mmorpg.jei.world.ingredient_count"));
                list.add(display("world/ingredients", List.of(), sources, ingredientPools, lines));
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
        return list;
    }

    private static List<ItemStack> sourceIcons() {
        return JeiStacks.clean(List.of(
            new ItemStack(Items.ZOMBIE_HEAD),
            new ItemStack(Items.SKELETON_SKULL),
            new ItemStack(Items.CREEPER_HEAD),
            new ItemStack(Items.CHEST),
            new ItemStack(Items.SPAWNER)
        ));
    }

    // ------------------------------------------------------------------
    // Information pages (press U on an item)
    // ------------------------------------------------------------------

    public static void registerInfo(IRecipeRegistration registration) {
        universalInfo(registration);
        ingredientsInfo(registration);
        currenciesInfo(registration);
        runesInfo(registration);
        toolsInfo(registration);
        stationsInfo(registration);
    }

    /**
     * Every mod item that already has a hand-written description gets that same
     * text as a JEI information page. Extra data variants are included so looking
     * up a real soul/crate/ingredient stack can find its family description too.
     */
    private static void universalInfo(IRecipeRegistration registration) {
        try {
            List<ItemStack> candidates = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                try {
                    var key = BuiltInRegistries.ITEM.getKey(item);
                    if (key == null) {
                        continue;
                    }
                    if (SlashRef.MODID.equals(key.getNamespace()) || "infinite_dungeons".equals(key.getNamespace())) {
                        candidates.add(new ItemStack(item));
                    }
                } catch (Exception ignored) {
                }
            }
            candidates.addAll(JeiStacks.extraIngredients());

            Set<String> seen = new HashSet<>();
            candidates.sort(Comparator.comparing((ItemStack stack) -> {
                var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                return key == null ? "" : key.toString();
            }));
            for (ItemStack stack : JeiStacks.clean(candidates)) {
                try {
                    List<Component> lines = ItemDescriptions.lines(stack);
                    if (lines == null || lines.isEmpty()) {
                        continue;
                    }
                    var itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    String key = (itemKey == null ? "" : itemKey.toString())
                        + "#" + String.valueOf(ItemDescriptions.resolveKey(stack))
                        + "#" + JeiSubtypes.ingredientKey(stack);
                    if (!seen.add(key)) {
                        continue;
                    }
                    registration.addItemStackInfo(stack.copy(), lines.toArray(new Component[0]));
                } catch (Exception ignored) {
                    // skip this stack only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
    }

    private static void ingredientsInfo(IRecipeRegistration registration) {
        try {
            for (SlashIngredient ing : ExileDB.Ingredients().getList()) {
                try {
                    ItemStack stack = JeiStacks.ingredient(ing);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    List<Component> lines = new ArrayList<>();
                    lines.add(tr("mmorpg.jei.info.ingredient"));

                    List<String> professions = new ArrayList<>();
                    for (PlayerSkillEnum skill : PlayerSkillEnum.getAll()) {
                        if (ing.isAllowedInProfession(skill.id)) {
                            professions.add(skill.word.locName().getString());
                        }
                    }
                    if (!professions.isEmpty()) {
                        lines.add(tr("mmorpg.jei.info.ingredient_professions", String.join(", ", professions)));
                    }
                    registration.addItemStackInfo(stack, lines.toArray(new Component[0]));
                } catch (Exception e) {
                    // skip this ingredient only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
    }

    private static void currenciesInfo(IRecipeRegistration registration) {
        try {
            for (CurrencyItem currency : ExileDB.CurrencyItems().getList()) {
                try {
                    List<Component> lines = new ArrayList<>();
                    lines.add(currency.locDesc());
                    lines.add(tr("mmorpg.jei.currency.use"));
                    registration.addItemStackInfo(new ItemStack(currency), lines.toArray(new Component[0]));
                } catch (Exception e) {
                    // skip this currency only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
    }

    private static void runesInfo(IRecipeRegistration registration) {
        try {
            for (RuneItem.RuneType type : RuneItem.RuneType.values()) {
                try {
                    ItemStack stack = JeiStacks.rune(type);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    List<Component> lines = new ArrayList<>();
                    lines.add(tr("mmorpg.jei.info.rune"));

                    List<String> words = new ArrayList<>();
                    for (RuneWord word : ExileDB.RuneWords().getList()) {
                        if (word.runes.contains(type.id)) {
                            String name = word.GUID();
                            try {
                                UniqueGear uniq = ExileDB.UniqueGears().get(word.uniq_id);
                                if (uniq != null) {
                                    name = uniq.locName().getString();
                                }
                            } catch (Exception ignored) {
                            }
                            words.add(name);
                        }
                    }
                    if (!words.isEmpty()) {
                        lines.add(tr("mmorpg.jei.info.rune_words", String.join(", ", words)));
                    } else {
                        lines.add(tr("mmorpg.jei.info.rune_unused"));
                    }
                    registration.addItemStackInfo(stack, lines.toArray(new Component[0]));
                } catch (Exception e) {
                    // skip this rune only
                }
            }
        } catch (Exception e) {
            // registries are not ready yet
        }
    }

    private static void toolsInfo(IRecipeRegistration registration) {
        try {
            for (PlayerSkillEnum skill : PlayerSkillEnum.getAll()) {
                ItemStack tool = JeiStacks.craftTool(skill);
                if (tool.isEmpty() || !(tool.getItem() instanceof CraftToolItem)) {
                    continue;
                }
                List<Component> lines = new ArrayList<>();
                lines.add(tr("mmorpg.jei.info.tool", skill.word.locName()));
                lines.add(skill.desc.locName());
                lines.add(tr("mmorpg.jei.info.tool_grid"));
                if (skill.isGearCraftingProf()) {
                    lines.add(tr("mmorpg.jei.info.tool_gear"));
                }
                registration.addItemStackInfo(tool, lines.toArray(new Component[0]));
            }

            registration.addItemStackInfo(new ItemStack(SlashItems.SALVAGE_HAMMER.get()),
                tr("mmorpg.jei.info.salvage_hammer"));
            registration.addItemStackInfo(new ItemStack(SlashItems.SOCKET_EXTRACTOR.get()),
                tr("mmorpg.jei.info.socket_extractor"));
        } catch (Exception e) {
            // ignore
        }
    }

    private static void stationsInfo(IRecipeRegistration registration) {
        try {
            registration.addItemStackInfo(new ItemStack(SlashItems.RUNEWORD_STATION.get()),
                tr("mmorpg.jei.info.runeword_station"),
                tr("mmorpg.jei.info.runeword_station_2"));
        } catch (Exception e) {
            // ignore
        }
    }

    // ------------------------------------------------------------------

    private static Identifier id(String path) {
        String safe = path.toLowerCase().replaceAll("[^a-z0-9_/.\\-]", "_");
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "/jei/" + safe);
    }
}
