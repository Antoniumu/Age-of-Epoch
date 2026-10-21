package com.robertx22.age_of_exile.saveclasses.spells;

import com.robertx22.age_of_exile.database.data.spell_school.SpellSchool;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.synergy.Synergy;
import com.robertx22.age_of_exile.database.data.value_calc.MaxLevelProvider;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Storable
public class SpellsData {

    @Store
    public HashMap<String, Integer> extra_lvls = new HashMap<>();
    @Store
    public HashMap<String, Integer> allocated_lvls = new HashMap<>();
    @Store
    public HashMap<Integer, String> hotbars = new HashMap<>();
    @Store
    public Set<String> schools = new HashSet<>();
    @Store
    public int bonus_spell_points = 0;

    public void addToLevelsFromStat(String id, int num) {
        this.extra_lvls.put(id, extra_lvls.getOrDefault(id, 0) + num);
    }

    public void learnSpell(Spell spell, SpellSchool school) {

        schools.add(school.GUID());

        int current = allocated_lvls.getOrDefault(spell.GUID(), 0);
        allocated_lvls.put(spell.GUID(), current + 1);

    }

    public void learnSynergy(Synergy synergy, SpellSchool school) {
        schools.add(school.GUID());
        int current = allocated_lvls.getOrDefault(synergy.GUID(), 0);
        allocated_lvls.put(synergy.GUID(), current + 1);
    }

    public boolean unlearnNode(String id, SpellSchool school) {
        int current = allocated_lvls.getOrDefault(id, 0);
        if (current <= 0) {
            return false;
        }
        if (current == 1) {
            allocated_lvls.remove(id);
            hotbars.entrySet().removeIf(e -> id.equals(e.getValue()));
        } else {
            allocated_lvls.put(id, current - 1);
        }
        if (school != null && getAllocatedPointsInSchool(school) <= 0) {
            schools.remove(school.GUID());
        }
        return true;
    }

    public int getLevelOf(String id) {

        int level = allocated_lvls.getOrDefault(id, 0) + extra_lvls.getOrDefault(id, 0);

        MaxLevelProvider provider = MaxLevelProvider.get(id);

        if (provider != null) {
            if (level > provider.getMaxLevelWithBonuses()) {
                level = provider.getMaxLevelWithBonuses();
            }
        }

        return level;
    }

    public int getAllocatedPointsInSchool(SpellSchool school) {
        if (school == null) {
            return 0;
        }
        int total = 0;
        if (school.spells != null) {
            for (String spellId : school.spells.keySet()) {
                total += allocated_lvls.getOrDefault(spellId, 0);
            }
        }
        if (school.synergies != null) {
            for (String synId : school.synergies.keySet()) {
                total += allocated_lvls.getOrDefault(synId, 0);
            }
        }
        return total;
    }

}
