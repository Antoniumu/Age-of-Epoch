package com.robertx22.age_of_exile.uncommon.testing;

/**
 * Общее состояние прогона прокачки: пишет сервер ({@link ProgressionProbe}),
 * читает клиент (оверлей с подписью для видео).
 *
 * <p>В dev-клиенте одиночной игры сервер и клиент живут в одном JVM, поэтому
 * статических полей достаточно — как и у {@link SpellAuditState}.</p>
 */
public final class ProgressionState {

    /** Идёт ли прогон (для оверлея: показывать ли панель). */
    public static volatile boolean active = false;

    /** Название этапа по-русски: «Прокачка 10 → 25», «Выбор пути Вознесения». */
    public static volatile String stage = "";

    /** Деталь этапа: «Уровень 37 / 56», «Выбран путь: Танцор Прилива». */
    public static volatile String detail = "";

    /** Итог этапа/прогона: «OK», «FAIL: ...». */
    public static volatile String verdict = "";

    /** Текущий уровень и цель — для счётчика в панели. */
    public static volatile int level = 0;
    public static volatile int targetLevel = 0;

    private ProgressionState() {
    }

    public static void begin(int target) {
        active = true;
        stage = "Подготовка";
        detail = "";
        verdict = "";
        level = 0;
        targetLevel = target;
    }

    public static void stage(String name, String info) {
        stage = name == null ? "" : name;
        detail = info == null ? "" : info;
    }

    public static void setLevel(int lvl, int target) {
        level = lvl;
        targetLevel = target;
    }

    public static void setVerdict(String text) {
        verdict = text == null ? "" : text;
    }

    public static void end() {
        active = false;
        stage = "";
        detail = "";
        verdict = "";
    }
}
