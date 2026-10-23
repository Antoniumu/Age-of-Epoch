package com.robertx22.age_of_exile.database.data.spell_school;

import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.synergy.Synergy;
import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.PointData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SpellSchool implements JsonExileRegistry<SpellSchool>, IAutoGson<SpellSchool>, IAutoLocName {
    public static SpellSchool SERIALIZER = new SpellSchool();

    public String id = "";
    public transient String locname = "";

    public static int MAX_Y_ROWS = 6;
    public static int MAX_X_ROWS = 10;

    public HashMap<String, PointData> spells = new HashMap<>();
    public HashMap<String, PointData> synergies = new HashMap<>();

    /**
     * Ветка заклинания (spell id -> ветка). Заклинания с разными непустыми ветками в одной школе
     * взаимоисключают друг друга. Заклинания без ветки (общие) конфликтов не создают.
     */
    public HashMap<String, String> branches = new HashMap<>();

    public List<Integer> lvl_reqs = Arrays.asList(1, 5, 10, 20, 30, 40, 50);

    public String branchOf(String spellId) {
        String b = branches.get(spellId);
        return b == null ? "" : b;
    }

    /**
     * true, если уже изученное заклинание этой школы относится к другой ветке.
     * levelOf возвращает уровень изученности заклинания по id.
     */
    public boolean isBranchBlocked(String spellId, java.util.function.ToIntFunction<String> levelOf) {
        String mine = branchOf(spellId);
        if (mine.isEmpty()) {
            return false;
        }
        for (String other : spells.keySet()) {
            String theirs = branchOf(other);
            if (theirs.isEmpty() || theirs.equals(mine) || other.equals(spellId)) {
                continue;
            }
            if (levelOf.applyAsInt(other) > 0) {
                return true;
            }
        }
        return false;
    }

    // public HashMap<String, PointData> spells = new HashMap<>(); todo synergies

    public int getLevelNeededToAllocate(PointData point) {

        int req = lvl_reqs.get(point.y);

        return req;
    }

    public boolean isLevelEnoughForSpell(LivingEntity en, Spell spell) {
        return Load.Unit(en)
            .getLevel() >= getLevelNeededToAllocate(spells.get(spell.GUID()));
    }

    public boolean isLevelEnoughForSynergy(LivingEntity en, Synergy synergy) {
        return Load.Unit(en)
            .getLevel() >= getLevelNeededToAllocate(synergies.get(synergy.GUID()));
    }

    public Identifier getIconLoc() {
        return SlashRef.guiId("spells/schools/" + id);
    }

    public Identifier getBackgroundLoc() {
        return SlashRef.guiId("spells/school_backgrounds/" + id);
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.SPELL_SCHOOL;
    }

    @Override
    public Class<SpellSchool> getClassForSerialization() {
        return SpellSchool.class;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return 1000;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Spells;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".spell_school." + id;
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }
}
