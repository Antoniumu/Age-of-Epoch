package com.robertx22.age_of_exile.compat.jei;

import com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.age_of_exile.compat.jei.category.CurrencyRecipe;
import com.robertx22.age_of_exile.compat.jei.category.ProfessionCraftRecipe;
import com.robertx22.age_of_exile.compat.jei.category.RunewordRecipe;
import com.robertx22.age_of_exile.compat.jei.category.SalvageRecipe;
import com.robertx22.age_of_exile.database.data.currency.base.BaseLocRequirement;
import com.robertx22.age_of_exile.database.data.currency.base.CurrencyItem;
import com.robertx22.age_of_exile.database.data.ingredient.SlashIngredient;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.data.runewords.RuneWord;
import com.robertx22.age_of_exile.database.data.unique_items.UniqueGear;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.GemItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.player_skills.ingredient.items.CraftToolItem;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.GemItem;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.RuneItem;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects everything the mod knows about its own crafting systems and turns it into
 * JEI pages. Every builder swallows exceptions and simply returns fewer pages, so one
 * broken datapack entry can never break the JEI reload.
 */
public final class JeiRecipes {

    private JeiRecipes() {
    }

    // ------------------------------------------------------------------
    // Profession crafting: ingredient + gear -> enchanted gear / consumable
    // ------------------------------------------------------------------

    public static List<ProfessionCraftRecipe> professionCrafts() {
        List<ProfessionCraftRecipe> list = new ArrayList<>();
        try {
            for (PlayerSkillEnum skill : PlayerSkillEnum.getAll()) {
                ItemStack tool = JeiStacks.craftTool(skill);
                if (tool.isEmpty()) {
                    continue; // not a crafting profession
                }
                ItemStack gear = JeiStacks.craftGearSample(skill);
                if (skill.isGearCraftingProf() && gear.isEmpty()) {
                    continue;
                }
                ItemStack result = craftResult(skill, gear);

                for (SlashIngredient ing : ExileDB.Ingredients().getList()) {
                    try {
                        if (!ing.isAllowedInProfession(skill.id)) {
                            continue;
                        }
                        ItemStack ingredient = JeiStacks.ingredient(ing);
                        if (ingredient.isEmpty()) {
                            continue;
                        }
                        list.add(new ProfessionCraftRecipe(
                            id("prof_craft/" + skill.id + "/" + ing.GUID()),
                            gear,
                            ingredient,
                            result,
                            tool,
                            lines(skill)
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

    private static ItemStack craftResult(PlayerSkillEnum skill, ItemStack gear) {
        try {
            if (skill.isGearCraftingProf()) {
                return gear.copy();
            }
            if (skill.getCraftResultItem() != null) {
                return new ItemStack(skill.getCraftResultItem());
            }
        } catch (Exception e) {
            // ignore
        }
        return ItemStack.EMPTY;
    }

    private static List<Component> lines(PlayerSkillEnum skill) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("mmorpg.jei.prof.grid"));
        lines.add(Component.translatable("mmorpg.jei.prof.success"));
        lines.add(Component.translatable("mmorpg.jei.prof.multiplier"));
        if (skill.isGearCraftingProf()) {
            lines.add(Component.translatable("mmorpg.jei.prof.result_gear"));
        } else {
            lines.add(Component.translatable("mmorpg.jei.prof.result_consumable"));
            int uses = craftingUses(skill);
            if (uses > 0) {
                lines.add(Component.translatable("mmorpg.jei.prof.uses", uses));
            }
        }
        return lines;
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
                    String slot = word.slots.isEmpty() ? "" : word.slots.get(0);
                    ItemStack gear = JeiStacks.gearForSlot(slot);
                    if (gear.isEmpty()) {
                        continue;
                    }
                    ItemStack out = JeiStacks.runewordResult(word);
                    if (out.isEmpty()) {
                        out = gear.copy();
                    }

                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable("mmorpg.jei.runeword.runes", String.join(" + ", word.runes)));
                    lines.add(Component.translatable("mmorpg.jei.runeword.slots", String.join(", ", word.slots)));
                    lines.add(Component.translatable("mmorpg.jei.runeword.station"));

                    list.add(new RunewordRecipe(
                        id("runeword/" + word.GUID()),
                        runes,
                        gear,
                        out,
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
    // Salvaging gear
    // ------------------------------------------------------------------

    public static List<SalvageRecipe> salvages() {
        List<SalvageRecipe> list = new ArrayList<>();
        try {
            int tier = JeiStacks.sampleTier();
            for (GearRarity rarity : ExileDB.GearRarities().getList()) {
                try {
                    if (rarity.is_unique_item || rarity.weight <= 0) {
                        continue;
                    }
                    ItemStack gear = JeiStacks.raritySample(rarity);
                    if (gear.isEmpty()) {
                        continue;
                    }

                    List<ItemStack> outputs = new ArrayList<>();
                    int dust = Math.max(1, (rarity.dust_per_sal.min + rarity.dust_per_sal.max) / 2);
                    ItemStack dustStack = JeiStacks.dust(tier, dust);
                    if (!dustStack.isEmpty()) {
                        outputs.add(dustStack);
                    }
                    if (rarity.rar_ess_per_sal > 0) {
                        ItemStack essence = rarity.getRarityEssenceStack();
                        essence.setCount(rarity.rar_ess_per_sal);
                        outputs.add(essence);
                    }
                    if (outputs.isEmpty()) {
                        continue;
                    }

                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable("mmorpg.jei.salvage.hammer"));
                    lines.add(Component.translatable("mmorpg.jei.salvage.amount",
                        rarity.dust_per_sal.min, rarity.dust_per_sal.max));

                    list.add(new SalvageRecipe(
                        id("salvage/" + rarity.GUID()),
                        gear,
                        outputs,
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
    // Currency and gems ("drag onto gear")
    // ------------------------------------------------------------------

    public static List<CurrencyRecipe> currencies() {
        List<CurrencyRecipe> list = new ArrayList<>();
        try {
            ItemStack gear = JeiStacks.gearForSlot(GearSlots.CHEST);
            if (gear.isEmpty()) {
                gear = new ItemStack(Items.IRON_CHESTPLATE);
            }

            for (RegObj<GemItem> obj : GemItems.ALL) {
                try {
                    GemItem gem = obj.get();
                    if (gem == null) {
                        continue;
                    }
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable("mmorpg.jei.gem.socket"));
                    lines.add(Component.translatable("mmorpg.jei.gem.upgrade"));
                    lines.add(Component.translatable("mmorpg.jei.currency.requirements"));
                    for (BaseLocRequirement req : gem.requirements()) {
                        lines.add(Component.literal(" * ").append(req.getText()));
                    }

                    list.add(new CurrencyRecipe(
                        id("gem/" + gem.GUID()),
                        gear.copy(),
                        new ItemStack(gem),
                        gear.copy(),
                        lines
                    ));
                } catch (Exception e) {
                    // skip this gem only
                }
            }

            for (CurrencyItem currency : ExileDB.CurrencyItems().getList()) {
                try {
                    List<Component> lines = new ArrayList<>();
                    lines.add(currency.locDesc());
                    lines.add(Component.translatable("mmorpg.jei.currency.use"));

                    List<BaseLocRequirement> reqs = currency.requirements();
                    if (reqs != null && !reqs.isEmpty()) {
                        lines.add(Component.translatable("mmorpg.jei.currency.requirements"));
                        for (BaseLocRequirement req : reqs) {
                            lines.add(Component.literal(" * ").append(req.getText()));
                        }
                    }
                    if (currency.getInstability() > 0) {
                        lines.add(Component.translatable("mmorpg.jei.currency.instability",
                            (int) currency.getInstability()));
                    }
                    if (currency.getBreakChance() > 0) {
                        lines.add(Component.translatable("mmorpg.jei.currency.break_chance",
                            (int) currency.getBreakChance()));
                    }

                    list.add(new CurrencyRecipe(
                        id("currency/" + currency.GUID()),
                        gear.copy(),
                        new ItemStack(currency),
                        gear.copy(),
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
    // Plain text info pages (press "U" on an item)
    // ------------------------------------------------------------------

    public static void registerInfo(IRecipeRegistration registration) {
        ingredientsInfo(registration);
        currenciesInfo(registration);
        runesInfo(registration);
        toolsInfo(registration);
        stationsInfo(registration);
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
                    lines.add(Component.translatable("mmorpg.jei.info.ingredient"));

                    List<String> professions = new ArrayList<>();
                    for (PlayerSkillEnum skill : PlayerSkillEnum.getAll()) {
                        if (ing.isAllowedInProfession(skill.id)) {
                            professions.add(skill.word.locName().getString());
                        }
                    }
                    if (!professions.isEmpty()) {
                        lines.add(Component.translatable("mmorpg.jei.info.ingredient_professions",
                            String.join(", ", professions)));
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
                    lines.add(Component.translatable("mmorpg.jei.currency.use"));
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
                    lines.add(Component.translatable("mmorpg.jei.info.rune"));

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
                        lines.add(Component.translatable("mmorpg.jei.info.rune_words", String.join(", ", words)));
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
                lines.add(Component.translatable("mmorpg.jei.info.tool", skill.word.locName()));
                lines.add(skill.desc.locName());
                lines.add(Component.translatable("mmorpg.jei.info.tool_grid"));
                if (skill.isGearCraftingProf()) {
                    lines.add(Component.translatable("mmorpg.jei.info.tool_gear"));
                }
                registration.addItemStackInfo(tool, lines.toArray(new Component[0]));
            }

            registration.addItemStackInfo(new ItemStack(SlashItems.SALVAGE_HAMMER.get()),
                Component.translatable("mmorpg.jei.info.salvage_hammer"));
            registration.addItemStackInfo(new ItemStack(SlashItems.SOCKET_EXTRACTOR.get()),
                Component.translatable("mmorpg.jei.info.socket_extractor"));
        } catch (Exception e) {
            // ignore
        }
    }

    private static void stationsInfo(IRecipeRegistration registration) {
        try {
            registration.addItemStackInfo(new ItemStack(SlashItems.RUNEWORD_STATION.get()),
                Component.translatable("mmorpg.jei.info.runeword_station"),
                Component.translatable("mmorpg.jei.info.runeword_station_2"));
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
