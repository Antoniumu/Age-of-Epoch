package com.robertx22.forge_compat.fml.loading;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public enum FMLPaths implements Supplier<Path> {
    GAMEDIR,
    CONFIGDIR,
    MODSDIR,
    LOGDIR;

    @Override
    public Path get() {
        switch (this) {
            case CONFIGDIR:
                return Paths.get("config");
            case MODSDIR:
                return Paths.get("mods");
            case LOGDIR:
                return Paths.get("logs");
            case GAMEDIR:
            default:
                return Paths.get(".");
        }
    }
}
