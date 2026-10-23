package com.robertx22.age_of_exile.vanilla_mc.potion_effects;

import com.robertx22.age_of_exile.database.data.exile_effects.ExileEffectInstanceData;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashPotions;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftedConsumableData;
import com.robertx22.age_of_exile.saveclasses.ExactStatData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.saveclasses.unit.stat_ctx.MiscStatCtx;
import com.robertx22.age_of_exile.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.age_of_exile.vanilla_mc.potion_effects.types.ExileStatusEffect;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Storable
public class EntityStatusEffectsData {

    @Store
    public HashMap<String, ExileEffectInstanceData> exileMap = new HashMap<>();

    @Store
    private HashMap<String, CraftedConsumableData> cons = new HashMap<>();

    public ExileEffectInstanceData get(ExileStatusEffect eff) {
        return exileMap.getOrDefault(eff.GUID(), null);
    }

    public void set(ExileStatusEffect eff, ExileEffectInstanceData data) {
        exileMap.put(eff.GUID(), data);
    }

    public void addConsumableEffect(PlayerSkillEnum skill, CraftedConsumableData data) {
        cons.put(skill.GUID(), data);
    }

    /*
    public void expireConsumableEffect(PlayerSkillEnum skill) {
        cons.remove(skill.GUID());
    }


     */
    public StatContext getStats(LivingEntity en) {

        List<ExactStatData> stats = new ArrayList<>();

        cons.entrySet()
            .forEach(x -> {

                try {
                    PlayerSkillEnum prof = ExileDB.PlayerSkills()
                        .get(x.getKey()).type_enum;

                    MobEffect effect = SlashPotions.CRAFTED_CONSUMABLES_MAP.get(prof)
                        .get();

                    if (en.hasEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect))) {
                        stats.addAll(x.getValue().stats);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });

        return new MiscStatCtx(stats);

    }
}
