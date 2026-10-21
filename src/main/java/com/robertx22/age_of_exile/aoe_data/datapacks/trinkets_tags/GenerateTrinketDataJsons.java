package com.robertx22.age_of_exile.aoe_data.datapacks.trinkets_tags;

import com.google.common.base.Joiner;
import com.robertx22.age_of_exile.a_libraries.trinkets.TrinketSlots;
import com.robertx22.age_of_exile.a_libraries.trinkets.interfaces.ITrinketType;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.utilityclasses.DirUtils;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenerateTrinketDataJsons {

    public static void generate() {

        HashMap<String, List<String>> map = new HashMap<>();
        Set<String> trinketSlotIds = new LinkedHashSet<>();

        for (Item item : BuiltInRegistries.ITEM) {

            if (BuiltInRegistries.ITEM.getKey(item)
                .getNamespace()
                .equals(SlashRef.MODID) && item instanceof ITrinketType type) {

                String slot = type.trinketTypeName();
                String trinketId = TrinketSlots.trinketId(slot);

                if (trinketId != null) {
                    trinketSlotIds.add(trinketId);
                }

                List<String> list = new ArrayList<>();

                if (map.containsKey(slot)) {
                    list.addAll(map.get(slot));
                }

                list.add("\"" + BuiltInRegistries.ITEM.getKey(item)
                    .toString() + "\"");

                map.put(slot, list);

            }
        }
        // loop end

        // data/trinkets/entities/mmorpg.json - tells Trinkets which slots players have
        Path entitiesDir = Paths.get(DirUtils.trinketEntitiesPath());
        try {
            if (Files.exists(entitiesDir) == false) {
                Files.createDirectories(entitiesDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> sortedSlots = new ArrayList<>(trinketSlotIds);
        String slotsJson = Joiner.on(",").join(sortedSlots);

        String entityJson = "{\n  \"replace\": false,\n  \"entities\": [\"player\"],\n  \"slots\": ["
            + slotsJson + "]\n}";

        writeFile(Paths.get(entitiesDir.toAbsolutePath() + "/mmorpg.json"), entityJson);

        // data/trinkets/tags/item/<group>/<slot>.json - marks our jewelry as valid trinkets
        Path tagsDir = Paths.get(DirUtils.trinketItemTagsPath());
        try {
            if (Files.exists(tagsDir) == false) {
                Files.createDirectories(tagsDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Map.Entry<String, List<String>> entry : map.entrySet()) {

            String trinketId = TrinketSlots.trinketId(entry.getKey());

            if (trinketId == null) {
                continue;
            }

            String json = "{\n  \"replace\": false,\n  \"values\": ["
                + Joiner.on(",").join(entry.getValue()) + "]\n}";

            String[] parts = trinketId.split("/", 2);
            Path path = Paths.get(tagsDir.toAbsolutePath() + "/" + parts[0] + "/" + parts[1] + ".json");

            try {
                Files.write(path, json.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private static void writeFile(Path path, String json) {
        byte[] strToBytes = json.getBytes();

        try {
            Files.write(path, strToBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
