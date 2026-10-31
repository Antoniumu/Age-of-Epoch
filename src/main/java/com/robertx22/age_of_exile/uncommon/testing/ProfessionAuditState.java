package com.robertx22.age_of_exile.uncommon.testing;

/**
 * Общее состояние аудита профессий: пишет сервер ({@link ProfessionAuditProbe}),
 * читает клиент (оверлей с подписью для видео).
 *
 * <p>В dev-клиенте одиночной игры сервер и клиент живут в одном JVM, поэтому
 * статических полей достаточно — сетевой пакет не нужен. В выделенном сервере
 * оверлей просто ничего не покажет, на сам аудит это не влияет.
 */
public final class ProfessionAuditState {

    /** Идёт ли прогон (для оверлея: показывать ли подпись). */
    public static volatile boolean active = false;

    /** Профессия и позиция в прогоне. */
    public static volatile String profession = "";
    public static volatile int index = 0;
    public static volatile int total = 0;

    /** Подпись: что сейчас делается (кладём ингредиент, ждём результат...). */
    public static volatile String name = "";
    public static volatile String description = "";

    /** Текущая фаза: ПОДГОТОВКА / СЕТКА / РЕЗУЛЬТАТ / ПРОВЕРКА / ИТОГ. */
    public static volatile String phase = "";

    /** Живые числа: сколько слотов занято, шанс успеха, что в результате. */
    public static volatile String metrics = "";

    /** Итог: PASS / FAIL и причины. */
    public static volatile String verdict = "";

    private ProfessionAuditState() {
    }

    public static void begin(String professionName, int position, int professions) {
        active = true;
        profession = professionName == null ? "" : professionName;
        index = position;
        total = professions;
        name = "";
        description = "";
        phase = "ПОДГОТОВКА";
        metrics = "";
        verdict = "";
    }

    public static void step(String stepName, String stepDescription) {
        name = stepName == null ? "" : stepName;
        description = stepDescription == null ? "" : stepDescription;
        phase = "ПОДГОТОВКА";
        metrics = "";
        verdict = "";
    }

    public static void setPhase(String newPhase) {
        phase = newPhase == null ? "" : newPhase;
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
        profession = "";
    }
}
