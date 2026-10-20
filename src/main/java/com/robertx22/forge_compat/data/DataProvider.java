package com.robertx22.forge_compat.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataProvider {
    public static void save(Gson gson, HashCache cache, JsonElement json, Path target) throws IOException {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.writeString(target, gson.toJson(json), StandardCharsets.UTF_8);
    }
}
