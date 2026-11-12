package com.robertx22.age_of_exile.ascension;

import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;

import java.util.ArrayList;
import java.util.List;

/**
 * Сохраняемые данные Вознесения игрока: выбранный класс и история вложений.
 * Хранится внутри RPGPlayerData (поэтому синхронизируется с клиентом вместе с ним).
 *
 * <p>Ранги здесь — учёт очевидный: каждый элемент списка {@link #allocated} —
 * одно вложенное очко Вознесения. Сам уровень заклинания дополнительно пишется в
 * {@code SpellsData.extra_lvls} (см. {@link AscensionLogic}), чтобы движок каста
 * и хотбара работал нативно.
 */
@Storable
public class AscensionData {

    @Store
    public String chosen_class = "";

    /**
     * Каждый элемент — id заклинания-навыка, в который вложено 1 очко Вознесения.
     * Ранг узла = количество его вхождений в списке.
     */
    @Store
    public List<String> allocated = new ArrayList<>();

    public boolean hasChosenClass() {
        return chosen_class != null && !chosen_class.isEmpty();
    }

    public AscensionClasses getChosenClass() {
        return AscensionClasses.byId(chosen_class);
    }

    public boolean isClass(AscensionClasses cls) {
        return cls != null && cls.id.equals(chosen_class);
    }

    public int rankOf(String spellId) {
        int n = 0;
        for (String s : allocated) {
            if (s.equals(spellId)) {
                n++;
            }
        }
        return n;
    }

    public int spentPoints() {
        return allocated.size();
    }

    public boolean isAnythingAscended(String spellId) {
        for (AscensionClasses c : AscensionClasses.values()) {
            if (c.hasSkill(spellId)) {
                return true;
            }
        }
        return false;
    }

}
