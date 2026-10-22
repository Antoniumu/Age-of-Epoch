package com.robertx22.library_of_exile.dimension.teleport;

import com.robertx22.library_of_exile.dimension.MapDimensions;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class SavedPlayerMapTeleports {


    // the dimension the player came from, the dim must be not one of the 'map' dimensions
    public SavedTeleportPos home = new SavedTeleportPos();

    // the last tp 
    public List<SavedTeleportPos> last = new ArrayList<>();


    SavedTeleportPos getLast() {
        return last.get(last.size() - 1);
    }

    void deleteLast() {
        if (!last.isEmpty()) {
            last.remove(last.size() - 1);
        }
    }

    // teleports to maps
    public void entranceTeleportLogic(Player p, Identifier to, BlockPos topos) {
        Identifier from = p.level().dimension().identifier();
        teleportToMap(p, from, to, topos);
    }

    // if in map, teleports to last map teleport point, or if theres no more points, tps back home
    // points are deleted when used or when you teleport from home again
    public void exitTeleportLogic(Player p) {

        Identifier from = p.level().dimension().identifier();

        boolean fromMap = MapDimensions.isMap(from);

        if (fromMap) {
            if (this.last.isEmpty()) {
                teleportHome(p);
            } else {
                teleportToLast(p);
            }
        }
    }


    public void teleportHome(Player p) {
        var dim = home.getDimensionId();
        if (p.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim)) == null) {
            dim = p.level().getServer().overworld().dimension().identifier();
        }
        teleport(p, dim, home.getPos());
    }

    public void teleportToLast(Player p) {
        teleport(p, getLast().getDimensionId(), getLast().getPos());
        deleteLast();
    }

    public void teleportToMap(Player p, Identifier from, Identifier to, BlockPos topos) {
        boolean fromMap = MapDimensions.isMap(from);

        if (!fromMap) {
            home.setFrom(p);
            last = new ArrayList<>();
        } else {
            var data = new SavedTeleportPos();
            data.setFrom(p);
            last.add(data);
        }
        teleport(p, to, topos);
    }

    private void teleport(Player p, Identifier to, BlockPos pos) {
        TeleportUtils.teleport((ServerPlayer) p, pos, to);
    }

}
