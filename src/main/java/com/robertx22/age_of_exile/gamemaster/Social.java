package com.robertx22.age_of_exile.gamemaster;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * Социальный слой удержания для маленького сервера (4–6 игроков).
 *
 * <p>На сервере такого размера удержание решает не контент, а <b>ощущение живого мира</b>
 * и расписание: игрок должен видеть, что он не один, получать выгоду от игры вместе
 * и понимать, когда заходить, чтобы застать остальных.
 *
 * <p>Что здесь есть:
 * <ul>
 *     <li><b>Кооп-бонус</b> — опыт, лут и спавн растут с числом игроков онлайн.
 *     Играть вместе выгоднее, чем по очереди.</li>
 *     <li><b>Прайм-тайм</b> — гистограмма активности по часам (24 корзины) и «лучшее окно»
 *     из нескольких часов: сервер сам подсказывает, когда обычно собирается компания.</li>
 *     <li><b>Общая цель недели</b> — счётчик убийств всех игроков и награда каждому,
 *     кто зашёл после выполнения. Маленький сервер превращается в команду.</li>
 *     <li><b>Масштабирование мобов по числу игроков рядом</b> — вчетвером босс не
 *     разваливается за две секунды, а в одиночку не превращается в стену.</li>
 * </ul>
 *
 * <p>Класс чистый: сервер и мир сюда не передаются, все решения применяет {@link GameMaster}.
 */
public final class Social {

    private Social() {
    }

    // ------------------------------------------------------------------
    // Кооп-бонус
    // ------------------------------------------------------------------

    /** Множитель опыта за игру вместе: 1.0 при одном игроке, дальше +N% за каждого. */
    public static double coopExp(int online, GameMasterConfig config) {
        return coop(online, config == null ? 0.07 : config.social_coop_exp_per_player,
            config == null ? 1.35 : config.social_coop_max);
    }

    /** Множитель лута за игру вместе. */
    public static double coopLoot(int online, GameMasterConfig config) {
        return coop(online, config == null ? 0.05 : config.social_coop_loot_per_player,
            config == null ? 1.35 : config.social_coop_max);
    }

    /**
     * Множитель частоты спавна. В отличие от опыта, у него нет своего потолка:
     * итог всё равно клампится в вероятность 0…1 в {@code spawnRateMultiplier}.
     */
    public static double coopSpawn(int online, GameMasterConfig config) {
        double perPlayer = config == null ? 0.15 : config.social_coop_spawn_per_player;
        return 1.0 + Math.max(0, online - 1) * Math.max(0, perPlayer);
    }

    private static double coop(int online, double perPlayer, double max) {
        double ceiling = Math.max(1, max);
        return Math.min(ceiling, 1.0 + Math.max(0, online - 1) * Math.max(0, perPlayer));
    }

    /**
     * Множитель HP моба по числу игроков рядом с ним.
     *
     * <p>Считается в момент спавна/пересчёта статов. Одиночке моб не становится крепче,
     * а группа из 4 человек получает ×1.75 HP вместо «убили за один удар».
     */
    public static double groupHealthMultiplier(int nearbyPlayers, GameMasterConfig config) {
        if (config == null || !config.social_group_content) {
            return 1;
        }
        double perPlayer = Math.max(0, config.social_group_hp_per_player);
        double max = Math.max(1, config.social_group_hp_max);
        return Math.min(max, 1.0 + Math.max(0, nearbyPlayers - 1) * perPlayer);
    }

    // ------------------------------------------------------------------
    // Время: локальный час и неделя
    // ------------------------------------------------------------------

    /** Локальный час сервера (0–23) — с учётом часового пояса и перехода на летнее время. */
    public static int localHour(long epochSeconds) {
        try {
            int offset = TimeZone.getDefault()
                .getOffset(epochSeconds * 1000L) / 1000;
            long local = epochSeconds + offset;
            return (int) Math.floorMod(local / 3600L, 24L);
        } catch (Exception e) {
            return (int) Math.floorMod(epochSeconds / 3600L, 24L);
        }
    }

    /** Номер недели (UTC-эпоха / 7 дней) — границы недели стабильны и не зависят от пояса. */
    public static long weekIndex(long epochSeconds) {
        return Math.max(0, epochSeconds) / (7L * 86400L);
    }

    /** Индекс лучшего окна из {@code length} часов подряд по накопленной активности. */
    public static int bestWindowStart(long[] activity, int length) {
        if (activity == null || activity.length == 0) {
            return -1;
        }
        int window = Math.max(1, Math.min(24, length));
        long best = -1;
        int bestStart = -1;
        for (int start = 0; start < 24; start++) {
            long sum = 0;
            for (int i = 0; i < window; i++) {
                sum += Math.max(0, activity[(start + i) % 24]);
            }
            if (sum > best) {
                best = sum;
                bestStart = start;
            }
        }
        return bestStart;
    }

    /** Есть ли вообще данные для подсказки о прайм-тайме (иначе не показываем фикцию). */
    public static boolean hasActivity(long[] activity) {
        if (activity == null) {
            return false;
        }
        for (long value : activity) {
            if (value > 0) {
                return true;
            }
        }
        return false;
    }

    /** «19:00–22:00» по началу окна и его длине. */
    public static String formatWindow(int startHour, int length) {
        if (startHour < 0) {
            return "—";
        }
        int end = Math.floorMod(startHour + Math.max(1, length), 24);
        return String.format("%02d:00–%02d:00", startHour, end);
    }

    /** Топ часов по активности — для панели («когда все играют»). */
    public static List<Integer> topHours(long[] activity, int count) {
        List<Integer> result = new ArrayList<>();
        if (activity == null || activity.length < 24) {
            return result;
        }
        List<Integer> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(i);
        }
        hours.sort((a, b) -> Long.compare(activity[b], activity[a]));
        for (int i = 0; i < Math.min(count, hours.size()); i++) {
            if (activity[hours.get(i)] <= 0) {
                break;
            }
            result.add(hours.get(i));
        }
        return result;
    }

    /** Нормализованная активность (0…1) для столбиков на панели. */
    public static double[] normalized(long[] activity) {
        double[] result = new double[24];
        if (activity == null) {
            return result;
        }
        long max = 0;
        for (long value : activity) {
            max = Math.max(max, value);
        }
        if (max <= 0) {
            return result;
        }
        for (int i = 0; i < 24 && i < activity.length; i++) {
            result[i] = activity[i] / (double) max;
        }
        return result;
    }

    /** Человеческое описание цели недели. */
    public static String goalText(int kills, int goal) {
        if (goal <= 0) {
            return "цель выключена";
        }
        int percent = (int) Math.round(100.0 * Math.min(kills, goal) / goal);
        return kills + " / " + goal + " убийств (" + percent + "%)";
    }
}
