package com.robertx22.age_of_exile.uncommon.testing;

/**
 * Общее состояние аудита заклинаний: пишет сервер ({@link SpellAuditProbe}), читает
 * клиент (оверлей с подписью для видео).
 *
 * <p>В dev-клиенте одиночной игры сервер и клиент живут в одном JVM, поэтому
 * статических полей достаточно — сетевой пакет не нужен. В выделенном сервере
 * оверлей просто ничего не покажет, на сам аудит это не влияет.
 */
public final class SpellAuditState {

    /** Идёт ли прогон (для оверлея: показывать ли подпись). */
    public static volatile boolean active = false;

    /** Класс/школа и позиция заклинания в прогоне. */
    public static volatile String school = "";
    public static volatile int index = 0;
    public static volatile int total = 0;

    /** Подпись: что за умение и что оно делает. */
    public static volatile String name = "";
    public static volatile String description = "";

    /** Текущая фаза: ПОДГОТОВКА / ПРИМЕНЕНИЕ / НАБЛЮДЕНИЕ / ИТОГ. */
    public static volatile String phase = "";

    /** Живые числа: урон, попадания, эффекты, лечение. */
    public static volatile String metrics = "";

    /** Итог: PASS / FAIL и причины. */
    public static volatile String verdict = "";

    /** Когда началась фаза (мс) — для анимаций и для нарезки видео. */
    public static volatile long phaseStartedAt = 0L;

    private SpellAuditState() {
    }

    public static void begin(String schoolName, int position, int spells) {
        active = true;
        school = schoolName == null ? "" : schoolName;
        index = position;
        total = spells;
        name = "";
        description = "";
        phase = "ПОДГОТОВКА";
        metrics = "";
        verdict = "";
        phaseStartedAt = System.currentTimeMillis();
    }

    public static void spell(String spellName, String spellDescription) {
        name = spellName == null ? "" : spellName;
        description = spellDescription == null ? "" : spellDescription;
        phase = "ПОДГОТОВКА";
        metrics = "";
        verdict = "";
        phaseStartedAt = System.currentTimeMillis();
    }

    public static void setPhase(String newPhase) {
        phase = newPhase == null ? "" : newPhase;
        phaseStartedAt = System.currentTimeMillis();
    }

    public static void setMetrics(String text) {
        metrics = text == null ? "" : text;
    }

    public static void setVerdict(String text) {
        verdict = text == null ? "" : text;
    }

    public static void end() {
        active = false;
        phase = "";
        metrics = "";
        verdict = "";
        name = "";
        description = "";
        school = "";
    }
}
