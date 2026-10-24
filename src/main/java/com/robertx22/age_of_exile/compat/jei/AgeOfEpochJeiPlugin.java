package com.robertx22.age_of_exile.compat.jei;

import com.robertx22.age_of_exile.compat.jei.category.CurrencyCategory;
import com.robertx22.age_of_exile.compat.jei.category.ProfessionCraftingCategory;
import com.robertx22.age_of_exile.compat.jei.category.RunewordCategory;
import com.robertx22.age_of_exile.compat.jei.category.SalvageCategory;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CraftToolItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

/**
 * Just Enough Items integration.
 *
 * Everything the mod crafts goes through custom systems (profession grids, runeword
 * station, salvage, "drag currency onto gear"), none of which are vanilla recipes, so
 * JEI knows nothing about them. This plugin exposes all of them as browseable pages.
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
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(
            new ProfessionCraftingCategory(guiHelper),
            new RunewordCategory(guiHelper),
            new SalvageCategory(guiHelper),
            new CurrencyCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        safely(() -> registration.addRecipes(ProfessionCraftingCategory.TYPE, JeiRecipes.professionCrafts()));
        safely(() -> registration.addRecipes(RunewordCategory.TYPE, JeiRecipes.runewords()));
        safely(() -> registration.addRecipes(SalvageCategory.TYPE, JeiRecipes.salvages()));
        safely(() -> registration.addRecipes(CurrencyCategory.TYPE, JeiRecipes.currencies()));
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
