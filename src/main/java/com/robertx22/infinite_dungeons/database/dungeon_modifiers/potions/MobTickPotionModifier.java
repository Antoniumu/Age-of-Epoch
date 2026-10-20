package com.robertx22.infinite_dungeons.database.dungeon_modifiers.potions;

import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.DungeonModifier;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.ModifierType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class MobTickPotionModifier extends DungeonModifier {

    public MobEffect effect;
    public int amplifier;
    public int rank;

    public MobTickPotionModifier(String id, int rank, MobEffect effect, int amplifier) {
        super(id);
        this.effect = effect;
        this.amplifier = amplifier;
        this.rank = rank;
    }

    @Override
    public ModifierType getModifierType() {
        return ModifierType.mobTickPotion(rank);
    }

    @Override
    public void onControlBlockTick(ControlBlockEntity en) {
        if (en.data.ticks % 20 == 0) {
            en.getAllDungeonMobsAlive()
                .forEach(x -> {
                    x.addEffect(new MobEffectInstance(effect, 20 * 5, amplifier));
                });
        }
    }

    @Override
    public void onMobSpawn(LivingEntity en) {

    }

}
