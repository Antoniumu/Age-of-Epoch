package com.robertx22.age_of_exile.mmorpg;

import com.robertx22.age_of_exile.aoe_data.datapacks.trinkets_tags.GenerateTrinketDataJsons;
import com.robertx22.age_of_exile.aoe_data.datapacks.generators.LootTableGenerator;
import com.robertx22.age_of_exile.aoe_data.datapacks.generators.RecipeGenerator;
import com.robertx22.age_of_exile.aoe_data.datapacks.lang_file.CreateLangFile;
import com.robertx22.age_of_exile.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.age_of_exile.aoe_data.datapacks.modpack_helper_lists.ModpackerHelperLists;

public class DataGeneration {

    public static void generateAll() {

        try {
            new RecipeGenerator().run();
            new LootTableGenerator().run();

            ModpackerHelperLists.generate();

            CreateLangFile.create();
            GenerateTrinketDataJsons.generate();
            ItemModelManager.INSTANCE.generateModels();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
