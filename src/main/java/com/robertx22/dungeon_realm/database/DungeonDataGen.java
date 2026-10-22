package com.robertx22.dungeon_realm.database;

import com.robertx22.dungeon_realm.main.DungeonEntries;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.library_of_exile.localization.ExileLangFile;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.recipe.RecipeGenerator;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.orbs_of_crafting.misc.ShapedRecipeUTIL;
import net.minecraft.ChatFormatting;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DungeonDataGen implements DataProvider {

    public DungeonDataGen() {

    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {

        RecipeGenerator.addRecipe(DungeonMain.MODID, () -> ShapedRecipeUTIL.of(DungeonEntries.HOME_TP_BACK.get(), 16)
                .define('Y', Items.IRON_INGOT)
                .define('X', Items.GOLD_INGOT)
                .pattern(" X ")
                .pattern("YYY")
                .pattern("YYY"));

        List<ITranslated> tra = new ArrayList<>();
        tra.addAll(Arrays.stream(DungeonWords.values()).toList());
        for (ITranslated t : tra) {
            t.createTranslationBuilder().build();
        }
        TranslationBuilder.of(DungeonMain.MODID).name(ExileTranslation.item(DungeonEntries.DUNGEON_MAP_ITEM.get(), ChatFormatting.DARK_PURPLE + "Dungeon Map")).build();
        TranslationBuilder.of(DungeonMain.MODID).name(ExileTranslation.item(DungeonEntries.MAP_DEVICE_ITEM.get(), "Map Device")).build();
        TranslationBuilder.of(DungeonMain.MODID).name(ExileTranslation.item(DungeonEntries.UBER_FRAGMENT.get(), "Uber Fragment")).build();
        TranslationBuilder.of(DungeonMain.MODID).name(ExileTranslation.item(DungeonEntries.HOME_TP_BACK.get(), "Home Pearl")).build();
        TranslationBuilder.of(DungeonMain.MODID).name(ExileTranslation.item(DungeonEntries.RELIC_ITEM.get(), ChatFormatting.GOLD + "Dungeon Relic")).build();
        TranslationBuilder.of(DungeonMain.MODID).name(ExileTranslation.item(DungeonEntries.RELIC_KEY.get(), "Relic Key")).build();

        TranslationBuilder.of(DungeonMain.MODID).name(ExileTranslation.block(DungeonEntries.MAP_DEVICE_BLOCK.get(), "Map Device")).build();

        ExileLangFile.createFile(DungeonMain.MODID, "");


        for (ExileRegistryType type : ExileRegistryType.getAllInRegisterOrder()) {
            type.getDatapackGenerator().run(pOutput);
        }

        RecipeGenerator.generateAll(CachedOutput.NO_CACHE, DungeonMain.MODID);

        return CompletableFuture.completedFuture(null);
    }


    @Override
    public String getName() {
        return "dungeon_data";
    }
}
