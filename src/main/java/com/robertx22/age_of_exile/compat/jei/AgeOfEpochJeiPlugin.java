package com.robertx22.age_of_exile.compat.jei;

import com.robertx22.age_of_exile.compat.jei.category.CurrencyCategory;
import com.robertx22.age_of_exile.compat.jei.category.DisplayCategory;
import com.robertx22.age_of_exile.compat.jei.category.ProfessionCraftingCategory;
import com.robertx22.age_of_exile.compat.jei.category.RunewordCategory;
import com.robertx22.age_of_exile.compat.jei.category.SalvageCategory;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CraftToolItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.GemItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.player_skills.crafting_inv.ProfCraftScreen;
import com.robertx22.age_of_exile.vanilla_mc.blocks.runeword_station.RuneWordStationGui;
import com.robertx22.infinite_dungeons.main.DungeonItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Just Enough Items integration.
 *
 * Everything the mod crafts goes through custom systems (profession grids, runeword
 * station, salvage, souls, gems, crates and "drag currency onto gear"), none of which
 * are vanilla recipes, so JEI knows nothing about them. This plugin exposes all of
 * them as browseable pages and teaches JEI how the mod's data-driven item variants
 * differ from each other.
 *
 * Registered through the {@code jei_mod_plugin} entrypoint in fabric.mod.json. JEI is
 * an optional dependency: it is only needed at compile time, and building with
 * {@code -PnoJei} removes this package from the jar and disables that entrypoint.
 */
@JeiPlugin
public class AgeOfEpochJeiPlugin implements IModPlugin {

    public static final Identifier UID = Identifier.fromNamespaceAndPath(SlashRef.MODID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        safely(() -> JeiSubtypes.register(registration));
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        safely(() -> {
            List<ItemStack> extras = JeiStacks.extraIngredients();
            if (extras != null && !extras.isEmpty()) {
                registration.addExtraItemStacks(extras);
            }
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(
            new ProfessionCraftingCategory(guiHelper),
            new RunewordCategory(guiHelper),
            new SalvageCategory(guiHelper),
            new CurrencyCategory(guiHelper),
            new DisplayCategory(
                guiHelper,
                DisplayCategory.STAT_SOUL,
                "mmorpg.jei.category.stat_soul",
                icon(new ItemStack(SlashItems.STAT_SOUL.get())),
                142,
                false
            ),
            new DisplayCategory(
                guiHelper,
                DisplayCategory.SOUL_IDENTIFY,
                "mmorpg.jei.category.soul_identify",
                icon(new ItemStack(SlashItems.IDENTIFY_TOME.get())),
                136,
                false
            ),
            new DisplayCategory(
                guiHelper,
                DisplayCategory.GEM_SOCKETING,
                "mmorpg.jei.category.gem_socketing",
                gemIcon(),
                150,
                false
            ),
            new DisplayCategory(
                guiHelper,
                DisplayCategory.RARITY_UPGRADE,
                "mmorpg.jei.category.rarity_upgrade",
                icon(new ItemStack(SlashItems.RARITY_UPGRADE.get())),
                142,
                false
            ),
            new DisplayCategory(
                guiHelper,
                DisplayCategory.REPAIR,
                "mmorpg.jei.category.repair",
                icon(JeiStacks.dust(JeiStacks.sampleTier(), 1)),
                136,
                false
            ),
            new DisplayCategory(
                guiHelper,
                DisplayCategory.BACKPACK_UPGRADE,
                "mmorpg.jei.category.backpack_upgrade",
                icon(new ItemStack(SlashItems.BACKPACK.get())),
                136,
                false
            ),
            new DisplayCategory(
                guiHelper,
                DisplayCategory.CRATE_OPENING,
                "mmorpg.jei.category.crate_opening",
                icon(new ItemStack(SlashItems.LOOT_CRATE.get())),
                150,
                true
            ),
            new DisplayCategory(
                guiHelper,
                DisplayCategory.UNIQUE_GEAR,
                "mmorpg.jei.category.unique_gear",
                icon(new ItemStack(SlashItems.STAT_SOUL.get())),
                142,
                false
            ),
            new DisplayCategory(
                guiHelper,
                DisplayCategory.ITEM_ACTIONS,
                "mmorpg.jei.category.item_actions",
                icon(new ItemStack(SlashItems.FULL_FAVOR.get())),
                136,
                false
            ),
            new DisplayCategory(
                guiHelper,
                DisplayCategory.WORLD_SOURCES,
                "mmorpg.jei.category.world_sources",
                icon(new ItemStack(Items.CHEST)),
                150,
                true
            )
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        safely(() -> registration.addRecipes(ProfessionCraftingCategory.TYPE, JeiRecipes.professionCrafts()));
        safely(() -> registration.addRecipes(RunewordCategory.TYPE, JeiRecipes.runewords()));
        safely(() -> registration.addRecipes(SalvageCategory.TYPE, JeiRecipes.salvages()));
        safely(() -> registration.addRecipes(CurrencyCategory.TYPE, JeiRecipes.currencies()));
        safely(() -> registration.addRecipes(DisplayCategory.STAT_SOUL, JeiRecipes.statSouls()));
        safely(() -> registration.addRecipes(DisplayCategory.SOUL_IDENTIFY, JeiRecipes.soulIdentify()));
        safely(() -> registration.addRecipes(DisplayCategory.GEM_SOCKETING, JeiRecipes.gems()));
        safely(() -> registration.addRecipes(DisplayCategory.RARITY_UPGRADE, JeiRecipes.rarityUpgrades()));
        safely(() -> registration.addRecipes(DisplayCategory.REPAIR, JeiRecipes.repairs()));
        safely(() -> registration.addRecipes(DisplayCategory.BACKPACK_UPGRADE, JeiRecipes.backpackUpgrades()));
        safely(() -> registration.addRecipes(DisplayCategory.CRATE_OPENING, JeiRecipes.crates()));
        safely(() -> registration.addRecipes(DisplayCategory.UNIQUE_GEAR, JeiRecipes.uniqueGear()));
        safely(() -> registration.addRecipes(DisplayCategory.ITEM_ACTIONS, JeiRecipes.itemActions()));
        safely(() -> registration.addRecipes(DisplayCategory.WORLD_SOURCES, JeiRecipes.worldSources()));
        safely(() -> JeiRecipes.registerInfo(registration));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        safely(() -> registration.addCraftingStation(
            ProfessionCraftingCategory.TYPE,
            CraftToolItems.ARMOR.get(),
            CraftToolItems.WEAPON.get(),
            CraftToolItems.JEWEL.get(),
            CraftToolItems.ALCHEMY.get(),
            CraftToolItems.INSCRIBING.get(),
            CraftToolItems.COOKING.get()
        ));

        safely(() -> registration.addCraftingStation(
            RunewordCategory.TYPE,
            SlashItems.RUNEWORD_STATION.get()
        ));

        safely(() -> registration.addCraftingStation(
            SalvageCategory.TYPE,
            SlashItems.SALVAGE_HAMMER.get()
        ));

        safely(() -> registration.addCraftingStation(
            DisplayCategory.STAT_SOUL,
            SlashItems.STAT_SOUL.get()
        ));

        safely(() -> registration.addCraftingStation(
            DisplayCategory.SOUL_IDENTIFY,
            SlashItems.IDENTIFY_TOME.get(),
            SlashItems.COMMON_SOUL_PRODUCE.get()
        ));

        safely(() -> registration.addCraftingStation(
            DisplayCategory.GEM_SOCKETING,
            SlashItems.SOCKET_EXTRACTOR.get()
        ));

        safely(() -> registration.addCraftingStation(
            DisplayCategory.RARITY_UPGRADE,
            SlashItems.RARITY_ESSENCE.get(),
            SlashItems.RARITY_UPGRADE.get()
        ));

        safely(() -> registration.addCraftingStation(
            DisplayCategory.BACKPACK_UPGRADE,
            SlashItems.BACKPACK.get()
        ));

        safely(() -> registration.addCraftingStation(
            DisplayCategory.CRATE_OPENING,
            SlashItems.LOOT_CRATE.get(),
            SlashItems.RARITY_GEAR_CRATE.get(),
            DungeonItems.REWARD_CRATE.get()
        ));

        safely(() -> registration.addCraftingStation(
            DisplayCategory.UNIQUE_GEAR,
            SlashItems.STAT_SOUL.get()
        ));

        safely(() -> registration.addCraftingStation(
            DisplayCategory.ITEM_ACTIONS,
            SlashItems.EMPTY_FAVOR.get(),
            SlashItems.FULL_FAVOR.get(),
            SlashItems.NEWBIE_GEAR_BAG.get()
        ));

        safely(() -> registration.addCraftingStation(
            DisplayCategory.WORLD_SOURCES,
            Items.CHEST,
            Items.SPAWNER,
            Items.ZOMBIE_HEAD,
            Items.SKELETON_SKULL,
            Items.CREEPER_HEAD
        ));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // The profession screen is 176x207; its result slot is at (133, 75).
        safely(() -> registration.addRecipeClickArea(
            ProfCraftScreen.class,
            132,
            74,
            20,
            20,
            ProfessionCraftingCategory.TYPE
        ));

        // The runeword station GUI is 276x195; its craft button is at (176, 50),
        // sized 22x20 relative to the GUI's top-left corner.
        safely(() -> registration.addRecipeClickArea(
            RuneWordStationGui.class,
            176,
            50,
            22,
            20,
            RunewordCategory.TYPE
        ));
    }

    private static ItemStack gemIcon() {
        try {
            if (!GemItems.ALL.isEmpty()) {
                return icon(new ItemStack(GemItems.ALL.get(0).get()));
            }
        } catch (Exception ignored) {
            // fall through to the safe generic icon
        }
        return icon(ItemStack.EMPTY);
    }

    private static ItemStack icon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new ItemStack(Items.CHEST);
        }
        return stack;
    }

    private static void safely(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            // A broken datapack entry must never take the whole JEI reload down.
            e.printStackTrace();
        }
    }
}
