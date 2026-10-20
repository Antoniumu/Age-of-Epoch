package com.robertx22.age_of_exile.capability.player.data;

import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.NumberUtils;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Mob;
@Storable
public class ScalingPlayerDiffData {

    @Store
    private float diff = 0;
    @Store
    private int dcd = 0; // death cooldown

    public List<Component> getTooltip() {

        List<Component> list = new ArrayList<>();

        list.add(Component.literal("Difficulty: " + (int) diff));
        list.add(Component.literal("Extra Mob Levels: " + getBonusLevels()));
        list.add(Component.literal("Mob Damage at max LVL: " + NumberUtils.singleDigitFloat(getDMGMulti(getMobDifficultyAdder(GameBalanceConfig.get().MAX_LEVEL))) + "x"));
        list.add(Component.literal("Mob Health at max LVL: " + NumberUtils.singleDigitFloat(getDMGMulti(getMobDifficultyAdder(GameBalanceConfig.get().MAX_LEVEL))) + "x"));

        list.add(Component.literal(""));

        list.add(Component.literal("Difficulty increases by:"));
        list.add(Component.literal("- time passing"));
        list.add(Component.literal("- hostile mob kills"));

        return list;
    }

    public static float getHPMulti(float mobScalingDiff) {
        return (float) (1 + mobScalingDiff * ServerContainer.get().MOB_HP_MULTI_PER_DIFFICULTY.get());
    }

    public static float getDMGMulti(float mobScalingDiff) {
        return (float) (1 + mobScalingDiff * ServerContainer.get().MOB_DMG_MULTI_PER_DIFFICULTY.get());
    }

    public static float getLootMulti(float mobScalingDiff) {
        return (float) (1 + mobScalingDiff * ServerContainer.get().MOB_LOOT_MULTI_PER_DIFFICULTY.get());
    }

    public float getMobDifficultyAdder(int originalMobLevel) {
        return Mth.clamp(diff - (ServerContainer.get().MAX_DIFFICULTY.get()
            .floatValue() * (1F - LevelUtils.getMaxLevelMultiplier(originalMobLevel))), 0, Integer.MAX_VALUE);
    }

    public boolean canAddDeathDifficulty() {
        return dcd < 1;
    }

    public void tickDeathCooldown(int ticks) {
        if (dcd > 0) {
            dcd -= ticks;
        }
    }

    public int getBonusLevels() {
        int bonus = (int) (GameBalanceConfig.get().MAX_LEVEL * (diff / ServerContainer.get().MAX_DIFFICULTY.get()));

        return bonus;
    }

    public float getDifficulty() {
        return diff;
    }

    public void add(float d) {
        diff += d;
        diff = Mth.clamp(diff, 0, ServerContainer.get().MAX_DIFFICULTY.get()
            .floatValue());

    }

}
