package com.robertx22.the_harvest.database;

import com.robertx22.library_of_exile.localization.ExileLangFile;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.recipe.RecipeGenerator;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.the_harvest.main.HarvestEntries;
import com.robertx22.the_harvest.main.HarvestMain;
import com.robertx22.the_harvest.main.HarvestWords;
import net.minecraft.ChatFormatting;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HarvestDataGen implements DataProvider {

    public HarvestDataGen() {

    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        List<ITranslated> tra = new ArrayList<>();
        tra.addAll(Arrays.stream(HarvestWords.values()).toList());
        for (ITranslated t : tra) {
            t.createTranslationBuilder().build();
        }
        TranslationBuilder.of(HarvestMain.MODID).name(ExileTranslation.item(HarvestEntries.HARVEST_MAP_ITEM.get(), ChatFormatting.GREEN + "Harvest Map")).build();
        TranslationBuilder.of(HarvestMain.MODID).name(ExileTranslation.item(HarvestEntries.RELIC.get(), ChatFormatting.GREEN + "Dungeon Relic")).build();
        TranslationBuilder.of(HarvestMain.MODID).name(ExileTranslation.item(HarvestEntries.HARVEST_ITEM.get(), "Harvest")).build();
        TranslationBuilder.of(HarvestMain.MODID).name(ExileTranslation.block(HarvestEntries.HARVEST_BLOCK.get(), "Harvest")).build();
        TranslationBuilder.of(HarvestMain.MODID).name(ExileTranslation.block(HarvestEntries.SPAWNER_BLOCK.get(), "Harvest Spawner")).build();

        ExileLangFile.createFile(HarvestMain.MODID, "");


        for (ExileRegistryType type : ExileRegistryType.getAllInRegisterOrder()) {
            type.getDatapackGenerator().run(pOutput);
        }

        RecipeGenerator.generateAll(pOutput, HarvestMain.MODID);


        return CompletableFuture.completedFuture(null); // todo this is bad, but would it work?
        // i think this is only needed if you dont directly save the jsons yourself?
    }


    @Override
    public String getName() {
        return "harvest_data";
    }
}
