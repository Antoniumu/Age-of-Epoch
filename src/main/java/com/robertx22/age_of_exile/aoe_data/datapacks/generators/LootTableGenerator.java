package com.robertx22.age_of_exile.aoe_data.datapacks.generators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.forge_compat.data.HashCache;
import com.robertx22.forge_compat.fml.loading.FMLPaths;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class LootTableGenerator {

    protected HashCache cache;

    public LootTableGenerator() {
        cache = new HashCache(FMLPaths.GAMEDIR.get(), "datagencache");
    }

    protected Path getBasePath() {
        return FMLPaths.GAMEDIR.get();
    }

    protected Path movePath(Path target) {
        String movedpath = target.toString();
        movedpath = movedpath.replace("run/", "src/generated/resources/");
        movedpath = movedpath.replace("run\\", "src/generated/resources/");
        return Paths.get(movedpath);
    }

    private Path resolve(Path path, String id) {
        return path.resolve("data/" + SlashRef.MODID + "/loot_tables/" + id + ".json");
    }

    public void run() {
        generateAll(cache);
    }

    static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected void generateAll(HashCache cache) {
    }

    public static Identifier RUNE_SALVAGE_RECIPE = SlashRef.id("runes_salvage_recipe");
    public static Identifier GEM_SALVAGE_RECIPE = SlashRef.id("gems_salvage_recipe");
    public static Identifier CURRENCIES_SALVAGE_RECIPE = SlashRef.id("currencies_salvage_recipe");

    private HashMap<Identifier, LootTable> getLootTables() {
        return new HashMap<>();
    }

    private void addFarming(Block block, Item item, Item seed, int age, HashMap<Identifier, LootTable> map) {
    }

}
