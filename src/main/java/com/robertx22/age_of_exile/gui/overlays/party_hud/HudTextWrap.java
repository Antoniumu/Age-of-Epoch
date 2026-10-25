package com.robertx22.age_of_exile.gui.overlays.party_hud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Перенос текста для панели HUD.
 *
 * <p>Раньше строка цели рисовалась одним куском, обрезанным по ширине панели,
 * и превращалась в «Победите монстров · 0/…» — хвост с прогрессом пропадал.
 * Здесь текст переносится по словам, а если в строке есть «·», то в первую
 * очередь ломаем по нему: «Победите монстров» / «0/25».
 *
 * <p>Измерение ширины передаётся снаружи ({@link ToIntFunction}), поэтому
 * вёрстку можно тестировать обычным JUnit-тестом без запуска игры.
 */
public final class HudTextWrap {

    /** Без этого «……» добавится к строке, которая и так уже шире панели. */
    private static final String ELLIPSIS = "…";

    private HudTextWrap() {
    }

    /**
     * @param maxWidth ширина блока в пикселях
     * @param maxLines сколько строк максимум; лишнее схлопывается многоточием
     * @param widthOf измеряет ширину строки в пикселях
     */
    public static List<String> wrap(String text, int maxWidth, int maxLines, ToIntFunction<String> widthOf) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || maxLines <= 0) {
            return lines;
        }
        if (widthOf.applyAsInt(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }

        // Смысловая граница: «Победите монстров · 0/25».
        int dot = text.indexOf('·');
        if (dot > 0) {
            String head = text.substring(0, dot).trim();
            String tail = text.substring(dot + 1).trim();
            lines.addAll(wrapWords(head, maxWidth, maxLines, widthOf));
            if (lines.size() < maxLines && !tail.isEmpty()) {
                lines.addAll(wrapWords(tail, maxWidth, maxLines - lines.size(), widthOf));
            }
            return lines;
        }
        return wrapWords(text, maxWidth, maxLines, widthOf);
    }

    static List<String> wrapWords(String text, int maxWidth, int maxLines, ToIntFunction<String> widthOf) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() == 0 || widthOf.applyAsInt(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        if (lines.size() > maxLines) {
            List<String> limited = new ArrayList<>(lines.subList(0, maxLines));
            limited.set(maxLines - 1, trim(limited.get(maxLines - 1) + ELLIPSIS, maxWidth, widthOf));
            return limited;
        }
        // Слово шире панели (ссылка, длинный ник) — дорезаем по символам.
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (widthOf.applyAsInt(line) <= maxWidth) {
                result.add(line);
            } else {
                result.addAll(cutByChars(line, maxWidth, widthOf));
            }
        }
        return result;
    }

    static List<String> cutByChars(String text, int maxWidth, ToIntFunction<String> widthOf) {
        List<String> parts = new ArrayList<>();
        String rest = text;
        while (!rest.isEmpty()) {
            int take = rest.length();
            while (take > 1 && widthOf.applyAsInt(rest.substring(0, take)) > maxWidth) {
                take--;
            }
            parts.add(rest.substring(0, take));
            rest = rest.substring(take);
        }
        return parts;
    }

    static String trim(String text, int maxWidth, ToIntFunction<String> widthOf) {
        if (text == null) {
            return "";
        }
        if (widthOf.applyAsInt(text) <= maxWidth) {
            return text;
        }
        int end = text.length();
        while (end > 0 && widthOf.applyAsInt(text.substring(0, end) + ELLIPSIS) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ELLIPSIS;
    }
}
