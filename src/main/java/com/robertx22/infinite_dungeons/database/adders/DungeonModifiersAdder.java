package com.robertx22.infinite_dungeons.database.adders;

import com.robertx22.infinite_dungeons.database.dungeon_modifiers.geared_mobs.*;
import com.robertx22.infinite_dungeons.database.dungeon_modifiers.misc.DoubleFasterMobSpawning;
import com.robertx22.infinite_dungeons.database.dungeon_modifiers.misc.EnchantedMobGear;
import com.robertx22.infinite_dungeons.database.dungeon_modifiers.misc.MobSpawnTntOnDeath;
import com.robertx22.infinite_dungeons.database.dungeon_modifiers.potions.MobTickPotionModifier;
import com.robertx22.infinite_dungeons.database.ids.DungeonModifierIds;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.world.effect.MobEffects;

public class DungeonModifiersAdder implements ExileRegistryInit {

    @Override
    public void registerAll() {

        new DoubleFasterMobSpawning().registerToExileRegistry();
        new MeleeSkeletonChance(DungeonModifierIds.MELEE_SKELETONS_CHANCE, 50).registerToExileRegistry();
        new MobSpawnTntOnDeath().registerToExileRegistry();
        new LeatherMobs().registerToExileRegistry();
        new IronMobs().registerToExileRegistry();
        new DiamondMobs().registerToExileRegistry();
        new NetheriteMobs().registerToExileRegistry();

        new MobTickPotionModifier("speed_mobs", 0, MobEffects.MOVEMENT_SPEED, 0).registerToExileRegistry();
        new MobTickPotionModifier("strong_mobs", 1, MobEffects.DAMAGE_BOOST, 1).registerToExileRegistry();

        new EnchantedMobGear(DungeonModifierIds.LVL_10_ENCHANT_GEAR_MOBS, 10).registerToExileRegistry();
        new EnchantedMobGear(DungeonModifierIds.LVL_20_ENCHANT_GEAR_MOBS, 20).registerToExileRegistry();
        new EnchantedMobGear(DungeonModifierIds.LVL_30_ENCHANT_GEAR_MOBS, 30).registerToExileRegistry();

    }
}
