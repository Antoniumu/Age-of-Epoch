package com.robertx22.library_of_exile.components;

import com.robertx22.library_of_exile.main.ExileLog;
import net.minecraft.world.entity.player.Player;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.event.entity.player.PlayerEvent;

import java.util.HashMap;

public class PlayerCapabilities {
    static HashMap<String, Capability<? extends ICap>> caps = new HashMap<>();

    public static void register(Capability<? extends ICap> cap, ICap obj) {
        caps.put(obj.getCapIdForSyncing(), cap);
    }

    public static void register(Capability<? extends ICap> cap, String syncid) {
        caps.put(syncid, cap);
    }

    public static void saveAllOnDeath(PlayerEvent.Clone event) {

        Player original = event.getOriginal();
        Player current = event.getEntity();

        

        caps.values()
                .forEach(x -> {

                    try {
                        ICap data = com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(current, x)
                                .orElse(null);

                        ICap origcap = com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(original, x)
                                .orElse(null);
                        if (origcap != null) {
                            data.deserializeNBT(origcap.serializeNBT());
                            data.syncToClient(current);
                        } else {
                            ExileLog.get().log("couldn't get original player's " + x.getName() + " capability");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        

    }

    public static void syncAllToClient(Player player) {

        try {
            caps.values()
                    .forEach(x -> {
                        com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(player, x)
                                .orElse(null)
                                .syncToClient(player);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static ICap get(Player player, String id) {
        if (caps.containsKey(id)) {
            Capability cap = caps.get(id);
            return (ICap) com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(player, cap)
                    .orElse(null);
        }
        return null;
    }

}
