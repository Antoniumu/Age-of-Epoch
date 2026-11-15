package com.robertx22.age_of_exile.ascension;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.List;

/**
 * Вознесённые классы (Ascended Classes) — ультимативные классы, открывающиеся с 50 уровня.
 *
 * <p>Это НЕ школы магии: у каждого класса своя тема (фон GUI, цвета, частицы),
 * а навыки идут линейной цепочкой — следующий узел скрыт «?», пока не вложены
 * очки в предыдущие (см. {@link AscensionLogic}).
 *
 * <p>Пока класс один: «Танцор Прилива» — боец с кнутом из живой воды
 * (по духу Нила из LoL). Новые классы добавляются новой записью в этом enum
 * + свои заклинания + тема + текстуры.
 */
public enum AscensionClasses {

    TIDE_DANCER(
        "tide_dancer",
        "Танцор Прилива",
        "Кнут из живой воды и несдержанная радость боя. Тот, кто встречает бурю с улыбкой.",
        0xFF37E2FF,   // акцент: яркая аква
        0xFF7DFFE9,   // светлая бирюза (блики)
        0xFFB26CFF,   // призматический фиолет (радость пучины)
        0xFF071F33,   // глубокая синева (фон)
        TideDancerSkillIds.ORDER
    );

    /**
     * Уровень, с которого открывается выбор Вознесённого класса.
     * До этого уровня GUI показывает серый экран с замком.
     */
    public static final int UNLOCK_LEVEL = 50;

    /**
     * Максимальный ранг одного узла линейной цепочки.
     */
    public static final int MAX_RANK = 3;

    public final String id;
    public final String locName;
    public final String lore;

    public final int accentColor;   // основной цвет класса (ARGB)
    public final int lightColor;    // светлый оттенок для бликов
    public final int prismColor;    // дополнительный «праздничный» оттенок
    public final int deepColor;     // глубокий тёмный оттенок для заливок

    /**
     * Навыки ЛИНЕЙНОЙ цепочки: порядок в списке = порядок в древе.
     */
    public final List<String> skillIds;

    AscensionClasses(String id, String locName, String lore, int accentColor, int lightColor, int prismColor, int deepColor, List<String> skillIds) {
        this.id = id;
        this.locName = locName;
        this.lore = lore;
        this.accentColor = accentColor;
        this.lightColor = lightColor;
        this.prismColor = prismColor;
        this.deepColor = deepColor;
        this.skillIds = skillIds;
    }

    public Identifier getBackgroundLoc() {
        return SlashRef.guiId("ascension/" + id + "_bg");
    }

    public Identifier getEmblemLoc() {
        return SlashRef.guiId("ascension/" + id + "_emblem");
    }

    public boolean hasSkill(String spellId) {
        return skillIds.contains(spellId);
    }

    public int indexOfSkill(String spellId) {
        return skillIds.indexOf(spellId);
    }

    public static AscensionClasses byId(String id) {
        if (id == null) {
            return null;
        }
        for (AscensionClasses c : values()) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    public static List<AscensionClasses> listAll() {
        return Arrays.asList(values());
    }

    /**
     * Идентификаторы навыков «Танцора Прилива». Вынесены отдельно, чтобы и
     * класс-описание, и регистратор заклинаний пользовались одними и теми же
     * константами (компилятор их инлайнит — порядок инициализации классов
     * роли не играет).
     */
    public static class TideDancerSkillIds {
        public static final String WHIP = "tidewhip_lash";
        public static final String DASH = "slipstream_surge";
        public static final String VEIL = "jubilant_veil";
        public static final String JOY = "unbound_joy";
        public static final String GRASP = "undertow_grasp";
        public static final String ULT = "maelstrom_waltz";

        public static final List<String> ORDER = Arrays.asList(WHIP, DASH, VEIL, JOY, GRASP, ULT);

    }
}
