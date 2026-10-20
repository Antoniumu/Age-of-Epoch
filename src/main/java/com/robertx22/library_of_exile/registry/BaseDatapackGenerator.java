package com.robertx22.library_of_exile.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.robertx22.library_of_exile.registry.serialization.ISerializable;
import net.minecraft.data.HashCache;
import com.robertx22.forge_compat.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public abstract class BaseDatapackGenerator<T extends IGUID & ISerializable<T>> {
    public static final Gson GSON = (new GsonBuilder()).setPrettyPrinting()
        .create();
    protected HashCache cache;
    public String category;
    public List<T> list;

    public abstract void run();

    public BaseDatapackGenerator(List<T> list, String category) {

        this.list = list;
        this.category = category;
        try {
            cache = new HashCache(FMLPaths.GAMEDIR.get(), "datagencache");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Path gameDirPath() {
        return FMLPaths.GAMEDIR.get();
    }

    protected Path movePath(Path target) {
        String movedpath = target.toString();
        movedpath = movedpath.replace("run/", "src/generated/resources/");
        movedpath = movedpath.replace("run\\", "src/generated/resources\\");

        return Paths.get(movedpath);
    }
}
