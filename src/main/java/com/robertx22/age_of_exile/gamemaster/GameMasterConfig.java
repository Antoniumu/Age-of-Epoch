package com.robertx22.age_of_exile.gamemaster;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Настройки «GameMaster» — систем динамической подстройки опыта и spawn-rate мобов.
 * <p>
 * Файл: {@code config/age_of_epoch/gamemaster.json}
 * <p>
 * Независимые переключатели:
 * <ul>
 *     <li>{@code catch_up} — догоняющий опыт: отстающие от группы игроки получают больше опыта,
 *     ушедшие вперёд — чуть меньше. Цель — развиваться вместе.</li>
 *     <li>{@code pacing} — темп развития: после {@code pacing_start_level} всем выдаётся
 *     прогрессивный штраф к опыту так, чтобы игрок наигрывал около {@code target_hours}
 *     <b>реальных</b> часов до максимального уровня.</li>
 *     <li>{@code spawn_rate_enabled}/{@code spawn_rate_dynamic} — ручной и динамический
 *     множители частоты появления враждебных мобов.</li>
 *     <li>{@code auto_balance} — «режиссёр»: вместо фиксированных {@code target_hours} держит
 *     коридор «минут на уровень» и подстраивает опыт, HP/урон мобов, лут и частоту спавна.
 *     При включении заменяет {@code pacing} (иначе два регулятора гасят друг друга).</li>
 * </ul>
 * Если включены оба режима опыта, их множители перемножаются, а догоняющий режим работает
 * и тогда, когда общий темп ещё не растянут до целевых часов. Spawn-rate на опыт не влияет.
 */
public class GameMasterConfig {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    /** Текущая версия файла конфига. Нужна, чтобы мягко мигрировать старые файлы. */
    public static final int CONFIG_VERSION = 2;

    /**
     * null — файл был создан до появления версии (старый). После первой загрузки
     * выставляется в {@link #CONFIG_VERSION} и сохраняется.
     */
    public Integer config_version = null;

    /** Мастер-выключатель всей системы. */
    public boolean enabled = true;

    // ------------------------------------------------------------------
    // 1. Догоняющий опыт (catch-up)
    // ------------------------------------------------------------------

    /** Первый переключатель: подстраивать опыт игроков под группу. */
    public boolean catch_up = true;

    /** Бонус к опыту за каждый уровень отставания (0.25 = +25% за уровень). */
    public double catch_up_per_level = 0.25;

    /** Максимальный бонус отстающему игроку (2.0 = +200%). */
    public double catch_up_max_bonus = 2.0;

    /** Штрафовать ли игроков, ушедших вперёд от группы. */
    public boolean catch_up_penalize_ahead = true;

    /** Штраф к опыту за каждый уровень опережения (0.05 = -5% за уровень). */
    public double catch_up_penalty_per_level = 0.05;

    /** Нижняя граница множителя для ушедших вперёд (0.5 = не ниже x0.5). */
    public double catch_up_min_multiplier = 0.5;

    /**
     * Ориентир группы: 0 = средний уровень, 1 = уровень самого высокого игрока.
     * 0.35 — «подтягиваемся к чуть выше среднего».
     */
    public double catch_up_reference_blend = 0.35;

    /** Игроки с меньшим временем игры не участвуют в расчёте ориентира. */
    public int catch_up_min_playtime_minutes = 30;

    /** Игроки, не заходившие дольше N дней, не участвуют в расчёте ориентира. */
    public int catch_up_activity_window_days = 30;

    // ------------------------------------------------------------------
    // 2. Темп развития (pacing)
    // ------------------------------------------------------------------

    /** Второй переключатель: растягивать развитие до целевых реальных часов. */
    public boolean pacing = true;

    /** Целевое время игры в реальных часах до максимального уровня. */
    public double target_hours = 100.0;

    /** С какого уровня начинается штраф (уровни ниже не трогаем). */
    public int pacing_start_level = 20;

    /** Максимальный штраф на последнем уровне (0.9 = -90% опыта к максимуму). */
    public double pacing_max_penalty = 0.95;

    /** Максимальное ускорение, если сборка проходится быстрее цели (1.0 = +100%). */
    public double pacing_max_bonus = 1.0;

    /** Кривая роста штрафа: 1 = линейно, 2 = мягкий старт и резкий конец. */
    public double pacing_curve = 2.0;

    /** Автоматически подбирать силу штрафа по реальным замерам времени. */
    public boolean pacing_auto_calibrate = true;

    /** Оценка «базового» времени до максимума, пока нет своих замеров. */
    public double assumed_baseline_hours = 40.0;

    /** Нижняя граница итогового множителя опыта. */
    public double exp_multiplier_min = 0.05;

    /** Верхняя граница итогового множителя опыта. */
    public double exp_multiplier_max = 5.0;

    // ------------------------------------------------------------------
    // 2b. Автобаланс (динамическое управление балансом)
    // ------------------------------------------------------------------

    /**
     * Вместо фиксированной цели «N часов» держать игрока в коридоре темпа и
     * подстраивать под него сложность мобов, лут и награду.
     * Если выключено — работает старый режим {@code pacing} с {@code target_hours}.
     */
    public boolean auto_balance = true;

    /** Целевое время первого уровня, минуты. */
    public double auto_start_minutes = 14.0;

    /** Целевое время последнего уровня, минуты. */
    public double auto_end_minutes = 70.0;

    /** Форма роста цели: 1 — линейно по логарифму, 2 — долго держит низкие уровни. */
    public double auto_curve = 1.0;

    /** «Мёртвая зона» вокруг цели: ±20% не трогаем, чтобы не было качелей. */
    public double auto_dead_zone = 0.2;

    /** Границы множителя опыта от автобаланса. */
    public double auto_exp_min = 0.35;
    public double auto_exp_max = 3.0;

    /** Максимальное изменение множителя опыта за минуту (0.25 = ±25%). */
    public double auto_exp_step_per_minute = 0.25;

    /** Смертей в час, после которых включается «мягкий режим». */
    public double auto_deaths_brake = 2.5;

    /** Смертей в час, ниже которых можно поднять сложность (если игрок идёт быстро). */
    public double auto_challenge_deaths = 0.3;

    /** Во сколько раз дольше цели считается «застреванием». */
    public double auto_rescue_factor = 2.0;

    /** Границы множителей HP и урона мобов от автобаланса. */
    public double auto_mob_health_min = 0.7;
    public double auto_mob_health_max = 1.35;
    public double auto_mob_damage_min = 0.7;
    public double auto_mob_damage_max = 1.3;

    /** Верхняя граница множителя лута от автобаланса (нижняя всегда 1.0). */
    public double auto_loot_max = 1.35;

    /**
     * Границы множителя частоты спавна от автобаланса.
     * Спавн — вероятность на попытку, итог всегда клампится в 0…1, поэтому «поднять»
     * он может только там, где спавн уже урезан (Благосклонность, ручной множитель).
     */
    public double auto_spawn_min = 0.6;
    public double auto_spawn_max = 1.5;

    /** Через сколько минут непрерывной игры включается «усталость сессии». */
    public double auto_session_fatigue_minutes = 150;

    /** Перерыв в часах, после которого возвращение получает лёгкий разгон. 0 — выключено. */
    public double auto_welcome_hours = 96;

    /** Разрешить автобалансу менять HP и урон мобов. */
    public boolean auto_touch_mobs = true;

    /** Разрешить автобалансу менять шанс дропа. */
    public boolean auto_touch_loot = true;

    /** Максимальное изменение серверных множителей мобов за минуту. */
    public double auto_global_step_per_minute = 0.05;

    // ------------------------------------------------------------------
    // 2c. Удержание игрока (см. Retention)
    // ------------------------------------------------------------------

    /** Ежедневные серии, вехи уровней и защита от невезения. */
    public boolean retention_enabled = true;

    /** Благосклонность за первый вход за сутки. */
    public double retention_daily_favor = 40;

    /** Добавка за каждый день серии. */
    public double retention_streak_bonus_favor = 25;

    /** Потолок добавки за серию. */
    public double retention_streak_max_favor = 300;

    /** Максимальная длина серии (дней). */
    public int retention_streak_max_days = 30;

    /** Перерыв в днях, который ещё не рвёт серию (1 = можно пропустить день). */
    public int retention_streak_grace_days = 1;

    /** Каждый N-й уровень даёт веху с наградой. */
    public int retention_milestone_every = 5;

    /** Базовый размер награды за веху. */
    public double retention_milestone_favor = 150;

    /** Включить защиту от невезения (pity) для уникатов. */
    public boolean retention_pity = true;

    /** Сколько убийств без униката считается «нормой» (до этого pity молчит). */
    public int retention_pity_start_kills = 250;

    /** На этом числе убийств шанс принудительного униката достигает 90%. */
    public int retention_pity_full_kills = 900;

    /**
     * До какого уровня смерть не отнимает опыт: новичок не должен терять прогресс
     * в первые минуты, это самый частый повод бросить игру.
     */
    public int retention_no_exp_loss_level = 5;

    // ------------------------------------------------------------------
    // 3. Спавн мобов вокруг игроков
    // ------------------------------------------------------------------

    /** Применять GameMaster к скорости появления враждебных мобов. */
    public boolean spawn_rate_enabled = true;

    /** Динамически уменьшать спавн по текущей Благосклонности игрока. */
    public boolean spawn_rate_dynamic = true;

    /** Ручной глобальный множитель вероятности спавна (1 = без ручного штрафа). */
    public double spawn_rate_manual_multiplier = 1.0;

    /**
     * Благосклонность, при которой динамический множитель равен 0.5.
     * <p>
     * Должна совпадать со стартовой Благосклонностью ({@code starting_favor}, сейчас 500):
     * иначе старт с 500 favor и базой 100 даёт спавн ×0.167 — втрое меньше прежнего ×0.5,
     * и игрок оказывается в почти пустом мире, а темп прокачки растягивается в разы.
     */
    public double spawn_rate_favor_reference = 500.0;

    /**
     * НИЖНЯЯ ПОЛКА спавна — доля попыток, которая проходит даже при нулевой Благосклонности.
     * <p>
     * Динамика развёрнута: раньше при высокой Благосклонности спавн падал
     * ({@code ref/(ref+favor)}), то есть лучший игрок получал пустой мир. Теперь
     * {@code rate = floor + (1 - floor) × favor/(favor + ref)}: чем выше Благосклонность,
     * тем больше мобов, а полка не даёт миру опустеть при нуле.
     * <p>
     * 0.65 = даже «пустой» игрок сохраняет две трети обычного спавна.
     */
    public double spawn_rate_dynamic_min_multiplier = 0.65;

    /** UUID игрока → ручной множитель-override (0…1); при его наличии заменяет глобальную ручную настройку. */
    public Map<String, Double> spawn_rate_player_overrides = new HashMap<>();

    // ------------------------------------------------------------------
    // 4. Учёт времени
    // ------------------------------------------------------------------

    /** Не считать AFK-время: если игрок не двигался N секунд, секунды не идут в статистику. */
    public boolean afk_exclusion = true;

    /** Порог AFK в секундах. */
    public int afk_threshold_seconds = 300;

    /** Как часто писать точку на графиках (в секундах реального времени игры). */
    public int snapshot_interval_seconds = 300;

    /** Как часто сохранять данные на диск (в секундах). */
    public int save_interval_seconds = 180;

    // ------------------------------------------------------------------
    // 5. Сообщения игрокам
    // ------------------------------------------------------------------

    /** Показывать множитель опыта в сообщении о полученном опыте. */
    public boolean show_multiplier_in_actionbar = true;

    /** Сообщать игроку его множитель при входе. */
    public boolean announce_on_login = true;

    // ------------------------------------------------------------------
    // 6. Веб-панель GameMaster
    // ------------------------------------------------------------------

    /** Поднимать локальную веб-панель с графиками. */
    public boolean web_enabled = true;

    /** Адрес, на котором слушает панель. 127.0.0.1 — только с этой машины. */
    public String web_host = "127.0.0.1";

    /** Порт панели. */
    public int web_port = 8787;

    /** Необязательный токен доступа (пусто — без проверки). */
    public String web_token = "";

    public GameMasterConfig() {
    }

    /** Приводит значения в безопасные границы. */
    public GameMasterConfig sanitize() {
        target_hours = clamp(target_hours, 1, 100000);
        pacing_start_level = (int) clamp(pacing_start_level, 1, 1000);
        pacing_max_penalty = clamp(pacing_max_penalty, 0, 0.99);
        pacing_max_bonus = clamp(pacing_max_bonus, 0, 100);
        pacing_curve = clamp(pacing_curve, 0.1, 10);
        assumed_baseline_hours = clamp(assumed_baseline_hours, 0.5, 100000);
        catch_up_per_level = clamp(catch_up_per_level, 0, 100);
        catch_up_max_bonus = clamp(catch_up_max_bonus, 0, 100);
        catch_up_penalty_per_level = clamp(catch_up_penalty_per_level, 0, 10);
        catch_up_min_multiplier = clamp(catch_up_min_multiplier, 0.01, 1);
        catch_up_reference_blend = clamp(catch_up_reference_blend, 0, 1);
        catch_up_min_playtime_minutes = (int) clamp(catch_up_min_playtime_minutes, 0, 100000);
        catch_up_activity_window_days = (int) clamp(catch_up_activity_window_days, 1, 100000);
        spawn_rate_manual_multiplier = clamp(spawn_rate_manual_multiplier, 0, 1);
        spawn_rate_favor_reference = clamp(spawn_rate_favor_reference, 1, 1000000);
        spawn_rate_dynamic_min_multiplier = clamp(spawn_rate_dynamic_min_multiplier, 0, 1);
        if (spawn_rate_player_overrides == null) {
            spawn_rate_player_overrides = new HashMap<>();
        } else {
            Iterator<Map.Entry<String, Double>> overrides = spawn_rate_player_overrides.entrySet().iterator();
            while (overrides.hasNext()) {
                Map.Entry<String, Double> entry = overrides.next();
                try {
                    UUID.fromString(entry.getKey());
                    if (entry.getValue() == null) {
                        overrides.remove();
                    } else {
                        entry.setValue(clamp(entry.getValue(), 0, 1));
                    }
                } catch (Exception invalidUuid) {
                    overrides.remove();
                }
            }
        }
        exp_multiplier_min = clamp(exp_multiplier_min, 0.001, 1000);
        exp_multiplier_max = clamp(exp_multiplier_max, exp_multiplier_min, 1000);
        auto_start_minutes = clamp(auto_start_minutes, 0.5, 100000);
        auto_end_minutes = clamp(auto_end_minutes, auto_start_minutes, 100000);
        auto_curve = clamp(auto_curve, 0.1, 10);
        auto_dead_zone = clamp(auto_dead_zone, 0, 0.9);
        auto_exp_min = clamp(auto_exp_min, 0.01, 1000);
        auto_exp_max = clamp(auto_exp_max, auto_exp_min, 1000);
        auto_exp_step_per_minute = clamp(auto_exp_step_per_minute, 0.001, 100);
        auto_deaths_brake = clamp(auto_deaths_brake, 0.01, 1000);
        auto_challenge_deaths = clamp(auto_challenge_deaths, 0, 1000);
        auto_rescue_factor = clamp(auto_rescue_factor, 1.05, 100);
        auto_mob_health_min = clamp(auto_mob_health_min, 0.05, 100);
        auto_mob_health_max = clamp(auto_mob_health_max, auto_mob_health_min, 100);
        auto_mob_damage_min = clamp(auto_mob_damage_min, 0.05, 100);
        auto_mob_damage_max = clamp(auto_mob_damage_max, auto_mob_damage_min, 100);
        auto_loot_max = clamp(auto_loot_max, 1, 100);
        retention_daily_favor = clamp(retention_daily_favor, 0, 100000);
        retention_streak_bonus_favor = clamp(retention_streak_bonus_favor, 0, 100000);
        retention_streak_max_favor = clamp(retention_streak_max_favor, 0, 1000000);
        retention_streak_max_days = (int) clamp(retention_streak_max_days, 1, 1000);
        retention_streak_grace_days = (int) clamp(retention_streak_grace_days, 1, 365);
        retention_milestone_every = (int) clamp(retention_milestone_every, 1, 50);
        retention_milestone_favor = clamp(retention_milestone_favor, 0, 100000);
        retention_pity_start_kills = (int) clamp(retention_pity_start_kills, 1, 1000000);
        retention_pity_full_kills = (int) clamp(retention_pity_full_kills,
            retention_pity_start_kills + 1, 10000000);
        retention_no_exp_loss_level = (int) clamp(retention_no_exp_loss_level, 0, 50);
        auto_spawn_min = clamp(auto_spawn_min, 0.05, 100);
        auto_spawn_max = clamp(auto_spawn_max, auto_spawn_min, 100);
        if (config_version == null) {
            config_version = CONFIG_VERSION;
        }
        auto_session_fatigue_minutes = clamp(auto_session_fatigue_minutes, 1, 100000);
        auto_welcome_hours = clamp(auto_welcome_hours, 0, 100000);
        auto_global_step_per_minute = clamp(auto_global_step_per_minute, 0.001, 100);
        afk_threshold_seconds = (int) clamp(afk_threshold_seconds, 5, 86400);
        snapshot_interval_seconds = (int) clamp(snapshot_interval_seconds, 10, 86400);
        save_interval_seconds = (int) clamp(save_interval_seconds, 5, 3600);
        if (web_host == null || web_host.isEmpty()) {
            web_host = "127.0.0.1";
        }
        web_port = (int) clamp(web_port, 1, 65535);
        if (web_token == null) {
            web_token = "";
        }
        return this;
    }

    public static double clamp(double v, double min, double max) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return min;
        }
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Применяет к конфигу поля из JSON (используется веб-панелью).
     *
     * @return список реально изменённых полей
     */
    public List<String> applyJson(JsonObject json) {
        List<String> changed = new ArrayList<>();
        if (json == null) {
            return changed;
        }
        for (String key : json.keySet()) {
            JsonElement el = json.get(key);
            if (el == null || el.isJsonNull()) {
                continue;
            }
            try {
                switch (key) {
                    case "enabled" -> this.enabled = el.getAsBoolean();
                    case "catch_up" -> this.catch_up = el.getAsBoolean();
                    case "catch_up_per_level" -> this.catch_up_per_level = el.getAsDouble();
                    case "catch_up_max_bonus" -> this.catch_up_max_bonus = el.getAsDouble();
                    case "catch_up_penalize_ahead" -> this.catch_up_penalize_ahead = el.getAsBoolean();
                    case "catch_up_penalty_per_level" -> this.catch_up_penalty_per_level = el.getAsDouble();
                    case "catch_up_min_multiplier" -> this.catch_up_min_multiplier = el.getAsDouble();
                    case "catch_up_reference_blend" -> this.catch_up_reference_blend = el.getAsDouble();
                    case "pacing" -> this.pacing = el.getAsBoolean();
                    case "target_hours" -> this.target_hours = el.getAsDouble();
                    case "pacing_start_level" -> this.pacing_start_level = el.getAsInt();
                    case "pacing_max_penalty" -> this.pacing_max_penalty = el.getAsDouble();
                    case "pacing_curve" -> this.pacing_curve = el.getAsDouble();
                    case "pacing_auto_calibrate" -> this.pacing_auto_calibrate = el.getAsBoolean();
                    case "assumed_baseline_hours" -> this.assumed_baseline_hours = el.getAsDouble();
                    case "auto_balance" -> this.auto_balance = el.getAsBoolean();
                    case "auto_start_minutes" -> this.auto_start_minutes = el.getAsDouble();
                    case "auto_end_minutes" -> this.auto_end_minutes = el.getAsDouble();
                    case "auto_curve" -> this.auto_curve = el.getAsDouble();
                    case "auto_dead_zone" -> this.auto_dead_zone = el.getAsDouble();
                    case "auto_exp_min" -> this.auto_exp_min = el.getAsDouble();
                    case "auto_exp_max" -> this.auto_exp_max = el.getAsDouble();
                    case "auto_exp_step_per_minute" -> this.auto_exp_step_per_minute = el.getAsDouble();
                    case "auto_deaths_brake" -> this.auto_deaths_brake = el.getAsDouble();
                    case "auto_challenge_deaths" -> this.auto_challenge_deaths = el.getAsDouble();
                    case "auto_rescue_factor" -> this.auto_rescue_factor = el.getAsDouble();
                    case "auto_mob_health_min" -> this.auto_mob_health_min = el.getAsDouble();
                    case "auto_mob_health_max" -> this.auto_mob_health_max = el.getAsDouble();
                    case "auto_mob_damage_min" -> this.auto_mob_damage_min = el.getAsDouble();
                    case "auto_mob_damage_max" -> this.auto_mob_damage_max = el.getAsDouble();
                    case "auto_loot_max" -> this.auto_loot_max = el.getAsDouble();
                    case "auto_spawn_min" -> this.auto_spawn_min = el.getAsDouble();
                    case "auto_spawn_max" -> this.auto_spawn_max = el.getAsDouble();
                    case "retention_enabled" -> this.retention_enabled = el.getAsBoolean();
                    case "retention_daily_favor" -> this.retention_daily_favor = el.getAsDouble();
                    case "retention_streak_bonus_favor" -> this.retention_streak_bonus_favor = el.getAsDouble();
                    case "retention_streak_max_favor" -> this.retention_streak_max_favor = el.getAsDouble();
                    case "retention_streak_max_days" -> this.retention_streak_max_days = el.getAsInt();
                    case "retention_streak_grace_days" -> this.retention_streak_grace_days = el.getAsInt();
                    case "retention_milestone_every" -> this.retention_milestone_every = el.getAsInt();
                    case "retention_milestone_favor" -> this.retention_milestone_favor = el.getAsDouble();
                    case "retention_pity" -> this.retention_pity = el.getAsBoolean();
                    case "retention_pity_start_kills" -> this.retention_pity_start_kills = el.getAsInt();
                    case "retention_pity_full_kills" -> this.retention_pity_full_kills = el.getAsInt();
                    case "retention_no_exp_loss_level" -> this.retention_no_exp_loss_level = el.getAsInt();
                    case "auto_session_fatigue_minutes" -> this.auto_session_fatigue_minutes = el.getAsDouble();
                    case "auto_welcome_hours" -> this.auto_welcome_hours = el.getAsDouble();
                    case "auto_touch_mobs" -> this.auto_touch_mobs = el.getAsBoolean();
                    case "auto_touch_loot" -> this.auto_touch_loot = el.getAsBoolean();
                    case "auto_global_step_per_minute" -> this.auto_global_step_per_minute = el.getAsDouble();
                    case "spawn_rate_enabled" -> this.spawn_rate_enabled = el.getAsBoolean();
                    case "spawn_rate_dynamic" -> this.spawn_rate_dynamic = el.getAsBoolean();
                    case "spawn_rate_manual_multiplier" -> this.spawn_rate_manual_multiplier = el.getAsDouble();
                    case "spawn_rate_favor_reference" -> this.spawn_rate_favor_reference = el.getAsDouble();
                    case "spawn_rate_dynamic_min_multiplier" -> this.spawn_rate_dynamic_min_multiplier = el.getAsDouble();
                    case "show_multiplier_in_actionbar" -> this.show_multiplier_in_actionbar = el.getAsBoolean();
                    case "announce_on_login" -> this.announce_on_login = el.getAsBoolean();
                    case "web_enabled" -> this.web_enabled = el.getAsBoolean();
                    case "web_port" -> this.web_port = el.getAsInt();
                    case "web_token" -> this.web_token = el.getAsString();
                    default -> {
                        continue;
                    }
                }
                changed.add(key);
            } catch (Exception e) {
                // игнорируем кривые значения из ручного JSON
            }
        }
        sanitize();
        return changed;
    }
}
