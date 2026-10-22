package com.robertx22.library_of_exile.dimension;

import com.robertx22.forge_compat.fml.loading.FMLPaths;
import com.robertx22.library_of_exile.main.ExileLog;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.stream.Stream;

public class WipeDimensionFeature {

    public static void OnStartResetMap(MapDimensionInfo map, Identifier mapid) {
        Path root = FMLPaths.GAMEDIR.get().toAbsolutePath();
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isDirectory).toList().forEach(dir -> {
                try {
                    if (dirMatches(dir.toFile(), mapid)) {
                        deleteDirectoryRecursion(dir);
                        map.markDataForClear = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean dirMatches(File file, Identifier mapid) {
        if (file.isDirectory()) {
            String str = file.toPath().toString();
            String check = "dimensions" + File.separator + mapid.getNamespace() + File.separator + mapid.getPath();
            if (str.endsWith(check)) {
                ExileLog.get().log("Wiping dimension folder: " + file);
                return true;
            }
        }
        return false;
    }

    static void deleteDirectoryRecursion(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
