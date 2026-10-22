package com.robertx22.library_of_exile.registry.loaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistry;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.register_info.ExileRegistrationInfo;
import com.robertx22.library_of_exile.registry.register_info.FromDatapackRegistration;
import com.robertx22.library_of_exile.registry.register_info.SeriazableRegistration;
import com.robertx22.library_of_exile.registry.serialization.ISerializable;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BaseDataPackLoader<T extends ExileRegistry> extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    public ExileRegistryType registryType;
    public String id;
    public ISerializable<T> serializer;

    public static int LAST_RELOAD_LOADED_OBJECTS = 0;
    public static int LAST_RELOAD_PARSE_ERRORS = 0;

    public static ExileRegistrationInfo INFO = new SeriazableRegistration("mmorpg");

    public BaseDataPackLoader(ExileRegistryType registryType, String id) {
        this(registryType, id, registryType != null ? registryType.getSerializer() : null);
    }

    @SuppressWarnings("unchecked")
    public BaseDataPackLoader(ExileRegistryType registryType, String id, ISerializable<T> serializer) {
        this.registryType = registryType;
        this.id = id;
        this.serializer = serializer;
    }

    public enum LoaderType {
        REPLACE_FULLY, ADD_TO_EXISTING
    }

    public LoaderType loaderType = LoaderType.REPLACE_FULLY;

    public void reloadSync(ResourceManager manager) {
        this.apply(this.prepare(manager, null), manager, null);
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> result = new LinkedHashMap<>();
        if (manager == null || this.id == null) return result;
        Map<Identifier, Resource> found = manager.listResources(this.id, loc -> loc.getPath().endsWith(".json"));
        String prefix = this.id + "/";
        List<Map.Entry<Identifier, Resource>> bundles = new ArrayList<>();
        List<Map.Entry<Identifier, Resource>> singles = new ArrayList<>();
        for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
            if (entry.getKey().getPath().endsWith("/_bundle.json")) {
                bundles.add(entry);
            } else {
                singles.add(entry);
            }
        }
        // Load base bundles first so any individual datapack .json file cleanly overrides specific entries!
        for (Map.Entry<Identifier, Resource> entry : bundles) {
            Identifier raw = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject bundleObj = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> item : bundleObj.entrySet()) {
                    result.put(Identifier.fromNamespaceAndPath(raw.getNamespace(), item.getKey()), item.getValue());
                }
            } catch (Exception ex) {
                LAST_RELOAD_PARSE_ERRORS++;
                ExileLog.get().warn("Failed parsing datapack bundle " + raw + ": " + ex.getMessage());
            }
        }
        for (Map.Entry<Identifier, Resource> entry : singles) {
            Identifier raw = entry.getKey();
            String path = raw.getPath();
            if (path.startsWith(prefix)) path = path.substring(prefix.length());
            if (path.endsWith(".json")) path = path.substring(0, path.length() - 5);
            Identifier cleanId = Identifier.fromNamespaceAndPath(raw.getNamespace(), path);
            try (Reader reader = entry.getValue().openAsReader()) {
                result.put(cleanId, JsonParser.parseReader(reader));
            } catch (Exception ex) {
                LAST_RELOAD_PARSE_ERRORS++;
                ExileLog.get().warn("Failed parsing datapack JSON " + raw + ": " + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void apply(Map<Identifier, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        try {
            var reg = Database.getRegistry(this.registryType);
            if (this.loaderType == LoaderType.REPLACE_FULLY && reg != null) {
                for (Object rawExisting : new ArrayList<>(reg.getList())) {
                    ExileRegistry existing = (ExileRegistry) rawExisting;
                    if (existing.isFromDatapack()) {
                        reg.unRegister(existing);
                    }
                }
            }
            ISerializable<T> ser = this.serializer != null ? this.serializer : this.registryType.getSerializer();
            for (Map.Entry<Identifier, JsonElement> entry : map.entrySet()) {
                try {
                    var json = entry.getValue().getAsJsonObject();
                    T object = (T) ser.fromJson(json);
                    if (reg != null && reg.isRegistered(object.GUID())) {
                        reg.unRegister(object);
                    }
                    object.registerToExileRegistry(new FromDatapackRegistration(entry.getKey()));
                    LAST_RELOAD_LOADED_OBJECTS++;
                } catch (Exception inner) {
                    LAST_RELOAD_PARSE_ERRORS++;
                    ExileLog.get().warn("Error loading " + entry.getKey() + " into " + this.id + ": " + inner.getMessage());
                }
            }
            if (reg != null) {
                reg.onAllDatapacksLoaded();
            }
        } catch (Exception e) {
            LAST_RELOAD_PARSE_ERRORS++;
            e.printStackTrace();
        }
    }
}
