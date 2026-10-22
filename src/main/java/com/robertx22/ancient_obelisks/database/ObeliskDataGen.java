package com.robertx22.ancient_obelisks.database;

import com.robertx22.ancient_obelisks.main.ObeliskEntries;
import com.robertx22.ancient_obelisks.main.ObeliskWords;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.localization.ExileLangFile;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.recipe.RecipeGenerator;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import net.minecraft.ChatFormatting;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ObeliskDataGen implements DataProvider {

    public ObeliskDataGen() {

    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        List<ITranslated> tra = new ArrayList<>();
        tra.addAll(Arrays.stream(ObeliskWords.values()).toList());
        for (ITranslated t : tra) {
            t.createTranslationBuilder().build();
        }
        TranslationBuilder.of(ObelisksMain.MODID).name(ExileTranslation.item(ObeliskEntries.OBELISK_MAP_ITEM.get(), ChatFormatting.DARK_PURPLE + "Obelisk Map")).build();
        TranslationBuilder.of(ObelisksMain.MODID).name(ExileTranslation.item(ObeliskEntries.OBELISK_ITEM.get(), "Obelisk")).build();
        TranslationBuilder.of(ObelisksMain.MODID).name(ExileTranslation.item(ObeliskEntries.RELIC.get(), ChatFormatting.LIGHT_PURPLE + "Dungeon Relic")).build();
        TranslationBuilder.of(ObelisksMain.MODID).name(ExileTranslation.block(ObeliskEntries.OBELISK_BLOCK.get(), "Obelisk")).build();
        TranslationBuilder.of(ObelisksMain.MODID).name(ExileTranslation.block(ObeliskEntries.SPAWNER_BLOCK.get(), "Obelisk Spawner")).build();
        TranslationBuilder.of(ObelisksMain.MODID).name(ExileTranslation.block(ObeliskEntries.OBELISK_REWARD_BLOCK.get(), "Obelisk Reward Spawner")).build();

        ExileLangFile.createFile(ObelisksMain.MODID, "");

        // todo  new RecipeGenerator().generateAll(pOutput, Obe.MODID);

        for (ExileRegistryType type : ExileRegistryType.getAllInRegisterOrder()) {
            type.getDatapackGenerator().run(pOutput);
        }

        RecipeGenerator.generateAll(pOutput, ObelisksMain.MODID);

        //DataProvider.saveStable(pOutput, x.serializeRecipe(), target);

        return CompletableFuture.completedFuture(null); // todo this is bad, but would it work?
        // i think this is only needed if you dont directly save the jsons yourself?
    }


    @Override
    public String getName() {
        return "obelisk_data";
    }
}
