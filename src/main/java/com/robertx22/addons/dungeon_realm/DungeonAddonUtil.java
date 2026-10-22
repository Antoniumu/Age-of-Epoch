package com.robertx22.addons.dungeon_realm;

import com.robertx22.mine_and_slash.database.data.rarities.MobRarity;
import com.robertx22.mine_and_slash.event_hooks.entity.OnMobSpawn;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Consumer;

public class DungeonAddonUtil {

    public static Consumer<LivingEntity> createMobRarityEdit(MobRarity rarity) {
        return mob -> {
            if (rarity != null) {
                Load.Unit(mob).setRarity(rarity.GUID());
            }
            OnMobSpawn.setupNewMobOnSpawn(mob);
            if (rarity != null) {
                Load.Unit(mob).setRarity(rarity.GUID());
            }
        };
    }
}
