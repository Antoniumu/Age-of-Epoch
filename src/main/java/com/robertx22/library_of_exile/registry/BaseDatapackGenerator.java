package com.robertx22.library_of_exile.registry;

import com.robertx22.forge_compat.fml.loading.FMLPaths;
import com.robertx22.library_of_exile.registry.serialization.ISerializable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public abstract class BaseDatapackGenerator<T extends IGUID & ISerializable<T>> {

    public String category;
    public List<T> list;

    public BaseDatapackGenerator(List<T> list, String category) {
        this.list = list;
        this.category = category;
    }

    protected Path gameDirPath() {
        return FMLPaths.GAMEDIR.get();
    }

    protected Path movePath(Path target) {
        String movedpath = target.toString();
        movedpath = movedpath.replace("run/", "src/main/resources/");
        movedpath = movedpath.replace("run\\", "src/main/resources\\");
        return Paths.get(movedpath);
    }
}
