package com.robertx22.age_of_exile.gamemaster;

/**
 * «Режиссёр» баланса: вместо фиксированной цели «N часов до максимума» держит игрока
 * в коридоре темпа и подстраивает под него сложность, лут и награду.
 *
 * <p>Идея в трёх правилах:
 * <ol>
 *     <li><b>Темп уровня.</b> Для каждого уровня есть целевое время (от {@code auto_start_minutes}
 *     на первом до {@code auto_end_minutes} на максимальном). Если игрок проходит уровень
 *     медленнее — опыт растёт, быстрее — падает. Внутри «мёртвой зоны» (±20%) ничего не
 *     меняется, чтобы не было качелей.</li>
 *     <li><b>Сложность по смертям.</b> Много смертей — мобы становятся мягче (по HP и урону),
 *     опыт чуть выше. Ни одной смерти и очень быстрый темп — мобы крепче: игра не должна
 *     превращаться в прогулку, это скучнее, чем вызов.</li>
 *     <li><b>Анти-фрустрация.</b> Если игрок «застрял» (нет нового уровня дольше двух целевых
 *     времён) или играет очень длинную сессию без прогресса — включается режим спасения:
 *     больше опыта, больше лута, мобы немного мягче.</li>
 * </ol>
 *
 * <p>Всё это не цель «продержать ровно 100 часов», а коридор: суммарное время до максимума
 * получается как сумма целевых времён уровней и само подстраивается под то, как игрок
 * реально играет. Класс чистый: никакого доступа к серверу, только числа.
 */
public final class AutoBalance {

    private AutoBalance() {
    }

    /** Готовые решения для одного игрока и для сервера в целом. */
    public static final class Directive {
        /** Множитель опыта. */
        public double exp = 1.0;
        /** Множитель HP мобов. */
        public double mob_health = 1.0;
        /** Множитель урона мобов. */
        public double mob_damage = 1.0;
        /** Множитель шанса дропа. */
        public double loot = 1.0;
        /** Множитель частоты спавна. */
        public double spawn = 1.0;
        /** 0…1: насколько игрок близок к целевому темпу (1 — идеально). */
        public double flow = 0.5;
        /** Целевое время текущего уровня, минуты. */
        public double target_minutes = 0;
        /** Фактическое время текущего уровня, минуты. */
        public double observed_minutes = 0;
        /** Короткое состояние для панели: stable / rescue / brake / push / welcome. */
        public String state = "stable";

        public Directive copy() {
            Directive d = new Directive();
            d.exp = exp;
            d.mob_health = mob_health;
            d.mob_damage = mob_damage;
            d.loot = loot;
            d.spawn = spawn;
            d.flow = flow;
            d.target_minutes = target_minutes;
            d.observed_minutes = observed_minutes;
            d.state = state;
            return d;
        }
    }

    public static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Целевое время уровня в минутах: геометрическая интерполяция между
     * {@code auto_start_minutes} и {@code auto_end_minutes}.
     * Геометрическая, потому что восприятие времени логарифмическое: переход
     * с 14 на 20 минут заметнее, чем с 64 на 70.
     */
    public static double targetMinutes(int level, int maxLevel, GameMasterConfig config) {
        double start = Math.max(0.5, config.auto_start_minutes);
        double end = Math.max(start, config.auto_end_minutes);
        int max = Math.max(2, maxLevel);
        double t = clamp((level - 1) / (double) (max - 1), 0, 1);
        double curve = Math.max(0.1, config.auto_curve);
        t = Math.pow(t, curve);
        if (start <= 0) {
            return end;
        }
        return start * Math.pow(end / start, t);
    }

    /** Суммарное целевое время 1 → максимум, часы. Полезно для панели. */
    public static double totalTargetHours(int maxLevel, GameMasterConfig config) {
        double sum = 0;
        for (int level = 1; level < Math.max(2, maxLevel); level++) {
            sum += targetMinutes(level, maxLevel, config);
        }
        return sum / 60.0;
    }

    /**
     * Ограничивает изменение значения: не быстрее, чем {@code stepPerMinute} в минуту.
     * Без этого множитель скачет от каждого замера и игрок видит «x3.0» вместо «x1.4».
     */
    public static double rateLimit(double previous, double desired, double stepPerMinute, double minutes) {
        if (previous <= 0) {
            return desired;
        }
        double maxStep = Math.max(0, stepPerMinute) * Math.max(0, minutes);
        if (maxStep <= 0) {
            return previous;
        }
        return clamp(desired, previous - maxStep, previous + maxStep);
    }

    /**
     * Основной расчёт для одного игрока.
     *
     * @param progress сохранённый прогресс (время, смерти, замеры)
     * @param config   настройки GameMaster
     * @param maxLevel максимальный уровень мода
     * @param now      текущее время (epoch seconds)
     */
    public static Directive evaluate(PlayerProgress progress, GameMasterConfig config, int maxLevel, long now) {
        Directive d = new Directive();
        if (progress == null) {
            return d;
        }

        int level = Math.max(1, Math.min(maxLevel, progress.level));
        double target = targetMinutes(level, maxLevel, config);
        double observed = progress.ema_minutes_per_level;

        d.target_minutes = target;
        d.observed_minutes = observed;

        double ratio = observed > 0.05 ? observed / target : 1.0;
        double deadZone = Math.max(0, Math.min(0.9, config.auto_dead_zone));

        // --- 1. Темп уровня -------------------------------------------------
        double exp = 1.0;
        if (observed > 0.05) {
            if (ratio > 1 + deadZone || ratio < 1 - deadZone) {
                exp = Math.pow(ratio, 0.8);
            }
        }
        d.flow = 1.0 - clamp(Math.abs(ratio - 1.0), 0, 1);

        // --- 2. Застревание ------------------------------------------------
        double minutesAtLevel = progress.level_started_playtime > 0
            ? Math.max(0, (progress.playtime_seconds - progress.level_started_playtime) / 60.0)
            : observed;
        boolean stuck = minutesAtLevel > target * Math.max(1.5, config.auto_rescue_factor);
        if (stuck) {
            exp *= 1.25;
            d.loot *= 1.25;
            d.state = "rescue";
        }

        // --- 3. Смертность и сложность -------------------------------------
        double deathsPerHour = deathsPerHour(progress);
        if (deathsPerHour > config.auto_deaths_brake) {
            double over = clamp((deathsPerHour - config.auto_deaths_brake) / Math.max(0.1, config.auto_deaths_brake), 0, 1);
            d.mob_health = 1.0 - 0.15 * over;
            d.mob_damage = 1.0 - 0.25 * over;
            exp *= 1.0 + 0.10 * over;
            d.state = "brake";
        } else if (deathsPerHour < config.auto_challenge_deaths && observed > 0.05
            && ratio < 1 - deadZone && !stuck) {
            // Игрок идёт быстрее цели и почти не умирает — можно добавить вызов.
            double under = clamp(1.0 - ratio, 0, 1);
            d.mob_health = 1.0 + 0.20 * under;
            d.mob_damage = 1.0 + 0.15 * under;
            if (!"rescue".equals(d.state)) {
                d.state = "push";
            }
        }

        // --- 4. Усталость длинной сессии -----------------------------------
        double fatigue = Math.max(30, config.auto_session_fatigue_minutes) * 60.0;
        if (progress.session_seconds > fatigue && minutesAtLevel > target * 0.8) {
            exp *= 1.15;
            d.mob_damage *= 0.93;
            if (!"brake".equals(d.state)) {
                d.state = "rescue";
            }
        }

        // --- 5. Возвращение после перерыва ---------------------------------
        // welcome_until выставляет GameMaster.onLogin(), когда перерыв был дольше
        // auto_welcome_hours. Считать по last_seen нельзя: он обновляется каждый тик.
        boolean welcome = progress.welcome_until > now;
        if (welcome) {
            // Первые минуты после долгого перерыва — лёгкий разгон и щедрый лут,
            // чтобы возвращение не начиналось с «я всё забыл и меня бьют».
            exp *= 1.15;
            d.loot *= 1.2;
            d.mob_damage *= 0.95;
            d.state = "welcome";
        }

        // --- 6. Спавн -------------------------------------------------------
        // Итоговый rate всегда ограничен 0…1 (это вероятность на попытку спавна),
        // поэтому автобаланс реально «возвращает» спавн только там, где он урезан
        // Благосклонностью или ручным множителем.
        switch (d.state) {
            case "rescue" -> d.spawn = 1.35;
            case "brake" -> d.spawn = 0.75;
            case "welcome" -> d.spawn = 1.15;
            default -> d.spawn = 1.0;
        }
        d.spawn = clamp(d.spawn, config.auto_spawn_min, config.auto_spawn_max);

        d.exp = clamp(exp, config.auto_exp_min, config.auto_exp_max);
        d.mob_health = clamp(d.mob_health, config.auto_mob_health_min, config.auto_mob_health_max);
        d.mob_damage = clamp(d.mob_damage, config.auto_mob_damage_min, config.auto_mob_damage_max);
        d.loot = clamp(d.loot, 1.0, config.auto_loot_max);

        // Плавность: за один пересчёт множитель не должен прыгать.
        double minutes = progress.auto_updated_epoch > 0
            ? Math.max(0.1, (now - progress.auto_updated_epoch) / 60.0)
            : 60.0;
        d.exp = rateLimit(progress.auto_exp_multiplier, d.exp, config.auto_exp_step_per_minute, minutes);
        d.exp = clamp(d.exp, config.auto_exp_min, config.auto_exp_max);

        return d;
    }

    /** Смертей в час на текущем уровне (по реальному времени уровня). */
    public static double deathsPerHour(PlayerProgress progress) {
        double minutes = Math.max(1.0,
            progress.level_started_playtime > 0 && progress.playtime_seconds > progress.level_started_playtime
                ? (progress.playtime_seconds - progress.level_started_playtime) / 60.0
                : progress.playtime_seconds / 60.0);
        return progress.deaths_since_level / (minutes / 60.0);
    }

    /** Обновляет замер «минут на уровень» после перехода на новый уровень. */
    public static void recordLevel(PlayerProgress progress, double minutes) {
        if (progress == null || minutes <= 0.05) {
            return;
        }
        if (progress.ema_minutes_per_level <= 0) {
            progress.ema_minutes_per_level = minutes;
        } else {
            double alpha = 0.35;
            progress.ema_minutes_per_level = progress.ema_minutes_per_level * (1 - alpha) + minutes * alpha;
        }
    }
}
