package com.robertx22.library_of_exile.registry.loaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.robertx22.library_of_exile.main.LibraryOfExile;
import com.robertx22.library_of_exile.registry.*;
import com.robertx22.library_of_exile.utils.Watch;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public abstract class BaseDataPackLoader<T extends ExileRegistry> extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {

    public String id;
    Function<JsonObject, T> serializer;
    public ExileRegistryType registryType;

    public BaseDataPackLoader(ExileRegistryType registryType, String id, Function<JsonObject, T> serializer) {
        Objects.requireNonNull(registryType);
        this.id = id;
        this.serializer = serializer;
        this.registryType = registryType;
    }

    public abstract ExileDatapackGenerator getDataPackGenerator();

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {

        if (LibraryOfExile.runDevTools()) {
            this.getDataPackGenerator()
                .run();
        }

        FileToIdConverter lister = FileToIdConverter.json(this.id);
        Map<Identifier, JsonElement> result = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : lister.listMatchingResources(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier fileId = lister.fileToId(location);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement elem = JsonParser.parseReader(reader);
                result.putIfAbsent(fileId, elem);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    static String ENABLED = "enabled";

    @Override
    protected void apply(Map<Identifier, JsonElement> mapToLoad, ResourceManager manager, ProfilerFiller profilerIn) {

        try {
            ExileRegistryContainer reg = Database.getRegistry(registryType);

            Watch normal = new Watch();
            normal.min = 50000;
            reg.unregisterAllEntriesFromDatapacks();

            mapToLoad.forEach((key, value) -> {
                try {
                    JsonObject json = value
                        .getAsJsonObject();

                    if (!json.has(ENABLED) || json.get(ENABLED)
                        .getAsBoolean()) {
                        T object = serializer.apply(json);
                        object.registerToExileRegistry();
                    }
                } catch (Exception exception) {
                    System.out.println(id + " is a broken datapack entry.");
                    exception.printStackTrace();
                }
            });

            normal.print("Loading " + registryType.id + " jsons ");

            if (reg
                .isEmpty()) {
                throw new RuntimeException("Exile Registry of type " + registryType.id + " is EMPTY after datapack loading!");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

    }

}
