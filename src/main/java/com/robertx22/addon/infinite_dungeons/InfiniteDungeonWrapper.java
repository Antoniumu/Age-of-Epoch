package com.robertx22.addon.infinite_dungeons;

import com.robertx22.infinite_dungeons.components.MobIDCap;
import com.robertx22.infinite_dungeons.components.PlayerIDCap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class InfiniteDungeonWrapper {

    public static boolean isDungeonMob(LivingEntity en) {
        return MobIDCap.get(en)
            .isDungeonMob();
    }

    public static boolean isPlayerInDungeon(Player en) {
        return PlayerIDCap.get(en)
            .isInDungeon();
    }

}
