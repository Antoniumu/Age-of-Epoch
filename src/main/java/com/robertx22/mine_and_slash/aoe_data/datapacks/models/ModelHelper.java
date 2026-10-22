package com.robertx22.mine_and_slash.aoe_data.datapacks.models;

import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.DirUtils;
import net.minecraft.world.item.Item;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModelHelper {

    Item item;
    Type type;
    String tex;

    public String modelPath = "";

    public ModelHelper(Item item, Type type) {
        this.item = item;
        this.type = type;
    }

    public ModelHelper(Item item, Type type, String tex) {
        this.item = item;
        this.type = type;
        this.tex = tex;
    }

    public void generate() {
        String tex = getTextureString();
        if (this.tex != null) {
            tex = this.tex;
        }
        String parent = (type == Type.GENERATED) ? "item/generated" : "item/handheld";
        String filecontent = getBaseJsonString()
            .replace("[PARENT]", parent)
            .replace("[TEXTURE]", tex);

        Path path = DirUtils.generatedResourcesDir().resolve(
            "assets/" + SlashRef.MODID + "/models/item/" + VanillaUTIL.REGISTRY.items().getKey(item).getPath() + ".json"
        );
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            File file = path.toFile();
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(filecontent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getTextureString() {
        String id = VanillaUTIL.REGISTRY.items().getKey(item).toString();
        if (!modelPath.isEmpty()) {
            id = SlashRef.MODID + ":" + modelPath;
        }
        return id.replace(SlashRef.MODID + ":", SlashRef.MODID + ":item/");
    }

    private String getBaseJsonString() {
        return "{\n  \"parent\": \"[PARENT]\",\n  \"textures\": {\n    \"layer0\": \"[TEXTURE]\"\n  }\n}";
    }

    public enum Type {
        GENERATED, HANDHELD
    }
}
