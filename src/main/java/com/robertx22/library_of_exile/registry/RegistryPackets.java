package com.robertx22.library_of_exile.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.robertx22.library_of_exile.registry.serialization.IByteBuf;
import com.robertx22.library_of_exile.utils.Watch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RegistryPackets {

    private static HashMap<ExileRegistryType, List<JsonObject>> map = new HashMap<>();

    public static List<JsonObject> get(ExileRegistryType type) {

        if (!map.containsKey(type)) {
            map.put(type, new ArrayList<>());
        }

        return map.get(type);
    }

    public static void registerAll(SyncTime sync) {
        if (net.minecraft.client.Minecraft.getInstance().hasSingleplayerServer()) {
            Database.getAllRegistries().forEach(ExileRegistryContainer::registerSerializables);
            map.clear();
            return;
        }

        Watch watch = new Watch();
        watch.min = 50000;

        ExileRegistryType.getInRegisterOrder(sync)
            .forEach(type -> {
                ExileRegistryContainer reg = Database.getRegistry(type);

                if (reg.isDatapack() && type.getLoader() != null && type.ser instanceof IByteBuf == false) {

                    List<JsonObject> list = map.get(type);

                    if (list == null || list.isEmpty()) {
                        reg.registerSerializables();
                        return;
                    }

                    reg.unregisterAllEntriesFromDatapacks();

                    list.forEach(x -> {

                        try {
                            JsonExileRegistry entry = (JsonExileRegistry) type.getSerializer()
                                .fromJson(x);
                            reg.unRegister(entry);
                            entry.registerToExileRegistry();

                        } catch (Exception e) {
                            System.out.println("Failed to parse Age of Exile registry Json!!!");
                            e.printStackTrace();
                        }

                    });

                    reg.registerSerializables();

                    if (reg.isEmpty()) {
                        System.out.println("Age of Exile Registry of type " + reg.getType() + " is EMPTY after datapack loading!");
                    }
                }
            });

        watch.print("Registering registry from gson packets on client");

        map.clear();
    }

}
