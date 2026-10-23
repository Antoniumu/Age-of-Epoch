package com.robertx22.library_of_exile.components;

import com.robertx22.forge_compat.common.capabilities.CapabilityProvider;

import com.robertx22.library_of_exile.components.forge.ICommonCap;
import com.robertx22.library_of_exile.components.forge.IPlayerCap;
import net.minecraft.world.entity.player.Player;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.event.entity.player.PlayerEvent;

import java.util.HashMap;

public class PlayerCapabilities {
    static HashMap<String, Capability<? extends IPlayerCap>> caps = new HashMap<>();

    public static void register(Capability<? extends IPlayerCap> cap, IPlayerCap obj) {
        caps.put(obj.getCapIdForSyncing(), cap);
    }

    public static void saveAllOnDeath(PlayerEvent.Clone event) {
        caps.values()
            .forEach(x -> {
                Player original = event.getOriginal();
                Player current = event.getPlayer();

                try {
                    ICommonCap data = CapabilityProvider.get(current, x)
                        .orElse(null);
                    data.loadFromNBT(CapabilityProvider.get(original, x)
                        .orElse(null)
                        .saveToNBT());
                    // todo add sync
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

    }

    public static void syncAllToClient(Player player) {

        try {
            caps.values()
                .forEach(x -> {
                    CapabilityProvider.get(player, x)
                        .orElse(null)
                        .syncToClient(player);
                });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static IPlayerCap get(Player player, String id) {
        if (caps.containsKey(id)) {
            Capability<? extends IPlayerCap> cap = caps.get(id);
            return CapabilityProvider.get(player, cap)
                .orElse(null);
        }
        return null;
    }

}
