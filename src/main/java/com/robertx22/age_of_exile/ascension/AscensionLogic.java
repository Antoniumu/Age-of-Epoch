package com.robertx22.age_of_exile.ascension;

import com.robertx22.age_of_exile.capability.player.EntitySpellCap;
import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.saveclasses.spells.SpellsData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.world.entity.player.Player;

/**
 * Все правила системы Вознесения живут здесь — единая точка истины для
 * сервера (пакеты) и клиента (GUI рисует по тем же формулам, чтобы не расходиться).
 *
 * <p><b>Очки Вознесения:</b> не тратят обычные очки навыков. Зарабатываются
 * уровнями после открытия: {@code max(0, уровень - 49)}, т.е. 1 очко на 50-м,
 * дальше +1 за уровень.
 *
 * <p><b>Линейная цепочка:</b> узел N «приоткрыт» (иконка видна, можно качать),
 * если в предыдущие узлы вложено не меньше N очков. Всё, что дальше — «?».
 */
public class AscensionLogic {

    /**
     * Точек за уровень (начиная с 50-го).
     */
    public static int earnedPoints(Player p) {
        int lvl = Load.Unit(p)
            .getLevel();
        return Math.max(0, lvl - (AscensionClasses.UNLOCK_LEVEL - 1));
    }

    public static int freePoints(Player p) {
        return earnedPoints(p) - data(p).spentPoints();
    }

    public static AscensionData data(Player p) {
        RPGPlayerData d = Load.playerRPGData(p);
        return d == null ? new AscensionData() : d.ascension;
    }

    public static boolean canPickClass(Player p) {
        return Load.Unit(p)
            .getLevel() >= AscensionClasses.UNLOCK_LEVEL;
    }

    /**
     * Ранги, вложенные в узлы строго ПЕРЕД указанным индексом.
     */
    public static int ranksBeforeNode(AscensionData data, AscensionClasses cls, int index) {
        int n = 0;
        for (int i = 0; i < index && i < cls.skillIds.size(); i++) {
            n += data.rankOf(cls.skillIds.get(i));
        }
        return n;
    }

    /**
     * «Приоткрыт» ли узел (иконка видна, можно качать): N-я ступень требует
     * N очков, вложенных во все предыдущие ступени в сумме.
     */
    public static boolean isNodeRevealed(AscensionData data, AscensionClasses cls, int index) {
        return ranksBeforeNode(data, cls, index) >= index;
    }

    /**
     * Индекс первого узла, который можно качать/докачивать прямо сейчас
     * (для подсветки «следующий шаг» в GUI); -1 если такого нет.
     */
    public static int firstActableNode(AscensionData data, AscensionClasses cls) {
        for (int i = 0; i < cls.skillIds.size(); i++) {
            if (isNodeRevealed(data, cls, i) && data.rankOf(cls.skillIds.get(i)) < AscensionClasses.MAX_RANK) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Почему нельзя вложить очко (пустая строка = можно).
     */
    public static String canAllocate(Player p, String spellId) {
        AscensionData data = data(p);
        if (!canPickClass(p)) {
            return "low_level";
        }
        AscensionClasses cls = data.getChosenClass();
        if (cls == null) {
            return "no_class";
        }
        int idx = cls.indexOfSkill(spellId);
        if (idx < 0) {
            return "wrong_class";
        }
        if (data.rankOf(spellId) >= AscensionClasses.MAX_RANK) {
            return "max_rank";
        }
        if (!isNodeRevealed(data, cls, idx)) {
            return "chain_locked";
        }
        if (freePoints(p) < 1) {
            return "no_points";
        }
        return "";
    }

    /**
     * Сервер: вложить очко Вознесения в навык. Пишет и в учёт Вознесения,
     * и в бонус-уровни заклинания (extra_lvls — очки школ не расходуются).
     */
    public static boolean allocate(Player p, String spellId) {
        if (!canAllocate(p, spellId).isEmpty()) {
            return false;
        }
        AscensionData data = data(p);
        data.allocated.add(spellId);

        EntitySpellCap.ISpellsCap spells = Load.spells(p);
        if (spells != null) {
            spells.getSpellsData()
                .addToLevelsFromStat(spellId, 1);
            spells.syncToClient(p);
        }
        syncRpg(p);
        return true;
    }

    /**
     * Сервер: выбрать (или сменить) Вознесённый класс. При смене ранги старого
     * класса возвращаются, уровни его умений убираются, хотбар чистится.
     */
    public static boolean pickClass(Player p, String classId) {
        AscensionClasses cls = AscensionClasses.byId(classId);
        if (cls == null || !canPickClass(p)) {
            return false;
        }
        AscensionData data = data(p);
        AscensionClasses old = data.getChosenClass();
        if (old == cls) {
            return true;
        }

        if (old != null) {
            EntitySpellCap.ISpellsCap spells = Load.spells(p);
            if (spells != null) {
                SpellsData sd = spells.getSpellsData();
                for (String id : old.skillIds) {
                    int ranks = data.rankOf(id);
                    if (ranks > 0) {
                        sd.addToLevelsFromStat(id, -ranks);
                    }
                    if (sd.extra_lvls.getOrDefault(id, 0) <= 0) {
                        sd.extra_lvls.remove(id);
                    }
                    sd.allocated_lvls.remove(id);
                }
                sd.hotbars.entrySet()
                    .removeIf(e -> old.hasSkill(e.getValue()));
                spells.syncToClient(p);
            }
            data.allocated.clear();
        }

        data.chosen_class = cls.id;
        syncRpg(p);
        return true;
    }

    /**
     * Сервер: назначить ИЗУЧЕННЫЙ навык Вознесённого класса в ячейку хотбара.
     * Отдельный путь от обычных школ — валидация идёт по данным Вознесения.
     */
    public static boolean assignHotbar(Player p, String spellId, int slot) {
        if (slot < 0 || slot > 7) {
            return false;
        }
        AscensionData data = data(p);
        AscensionClasses cls = data.getChosenClass();
        if (cls == null || !cls.hasSkill(spellId)) {
            return false;
        }
        if (data.rankOf(spellId) < 1) {
            return false;
        }
        EntitySpellCap.ISpellsCap spells = Load.spells(p);
        if (spells == null || spells.getLevelOf(spellId) < 1) {
            return false;
        }
        SpellsData sd = spells.getSpellsData();
        sd.hotbars.entrySet()
            .removeIf(e -> e.getValue()
                .equals(spellId));
        sd.hotbars.put(slot, spellId);
        spells.syncToClient(p);
        return true;
    }

    /**
     * Первая свободная ячейка хотбара (0..7) или ячейка, где уже стоит это умение.
     * Клиент и сервер считают одинаково.
     */
    public static int firstFreeHotbarSlot(Player p, String spellId) {
        EntitySpellCap.ISpellsCap spells = Load.spells(p);
        if (spells == null) {
            return 0;
        }
        SpellsData sd = spells.getSpellsData();
        for (int i = 0; i < 8; i++) {
            String cur = sd.hotbars.get(i);
            if (cur != null && cur.equals(spellId)) {
                return i;
            }
        }
        for (int i = 0; i < 8; i++) {
            String cur = sd.hotbars.get(i);
            if (cur == null || cur.isEmpty()) {
                return i;
            }
        }
        return 0;
    }

    private static void syncRpg(Player p) {
        RPGPlayerData d = Load.playerRPGData(p);
        if (d != null) {
            d.syncToClient(p);
        }
    }

}
