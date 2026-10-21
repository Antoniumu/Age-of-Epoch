package com.robertx22.age_of_exile.guides;

import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Первые три гайда Age of Epoch.
 *
 * Планы талантов сгенерированы скриптом tools/guides/build_guides.py:
 * он воспроизводит граф древа (TalentGrid.loadIntoTree), парсит статы перков
 * и строит ВАЛИДНЫЙ путь — каждый следующий узел соединён с предыдущим,
 * поэтому его можно взять по правилам TalentsData.canAllocate.
 *
 * Формат плана: "x:y:perkId;x:y:perkId;..." — координаты в сетке talents.json.
 * Индекс узла i требует (i / 2) уровня: очков талантов на уровне L = 1 + 2 * L.
 */
public class GuideData {

    private static final List<GuideDefinition> ALL = new ArrayList<>();

    public static List<GuideDefinition> all() {
        return ALL;
    }

    public static GuideDefinition byId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (GuideDefinition g : ALL) {
            if (g.id.equals(id)) {
                return g;
            }
        }
        return null;
    }

    public static GuideDefinition first() {
        return ALL.isEmpty() ? null : ALL.get(0);
    }

    static {
        // ------------------------------------------------------------------ 1. Воин
        GuideDefinition warrior = new GuideDefinition(
            "steel_guard", "Стальной страж",
            "Сила и живучесть: меч/топор, тяжёлая броня, школа Божественности",
            ChatFormatting.GOLD, "textures/gui/spells/schools/divine.png", "divine", 0)
            .stat("strength", 0.45F)
            .stat("vitality", 0.35F)
            .stat("dexterity", 0.20F)
            .spell("gong_strike", 16)
            .spell("whirlwind", 16)
            .spell("charge", 10)
            .spell("undying_will", 16)
            .spell("pull", 8)
            .spell("taunt", 10)
            .spell("banish", 10)
            .spell("shooting_star", 8)
            .tip("СИЛ: 1 очко = 10.8 брони и 1.08 физ. урона на 50 ур. Самый выгодный стат: защита и урон сразу.")
            .tip("ЖИВ: 1 очко = 21.6 HP на 50 ур. Броня не спасает от стихий — HP спасает.")
            .tip("ЛОВ: 1 очко = 10.8 уклонения и 1.08 урона. 20 ЛОВ = 216 уклонения = 37.6%.")
            .tip("Броня: 40% ≈ 324, 60% ≈ 972, 70% ≈ 2268. Потолок любого защитного стата — 80%.")
            .tip("Энергия: удар мечом стоит 4 × NORMAL(ур.) = 43 на 50 ур. При энергии < 10% вы получаете Замедление II.")
            .tip("Броня не работает против заклинаний: стихийный урон режут только сопротивления (их даёт ИНТ) и ЖИВ.")
            .milestone("Ур. 1-5: start_str → heavy_strike → физ. урон → cleave.")
            .milestone("Ур. 5-10: big_health, health, str, ground_slam, урон топора/меча.")
            .milestone("Ур. 10-20: big_armor, скорость атаки, узлы armor_dodge (броня + уклонение).")
            .milestone("Ур. 20-30: сопротивление природе, big_health_armor, big_armor_dodge.")
            .milestone("Ур. 30-40: пробивание брони, big_attack_speed, добор СИЛ/ЛОВ.")
            .milestone("Ур. 40-50: добор уклонения и HP, big_cdr, финальные узлы плана.")
            .talentPlan("47:41:start_str;45:43:heavy_strike;43:42:physical_weapon_damage;40:42:physical_weapon_damage;40:40:cleave;40:45:big_health;42:47:health;38:49:str;38:51:ground_slam;34:51:attack_speed;36:53:big_dex_gated;34:55:attack_speed_notable;38:55:armor;42:51:str;43:54:nature_balm;45:54:big_health_regen;46:48:armor_dodge;48:46:armor_dodge;42:58:str;43:60:health;43:62:big_health_armor;36:57:nature_resist;34:59:nature_resist;36:61:thorn_armor;36:63:big_armor;38:64:big_armor;40:64:big_armor;40:62:health;38:62:big_health_armor;43:64:armor;45:65:health_armor;47:66:health_armor;49:66:big_armor_dodge;51:66:health_dodge;53:65:health_dodge;55:64:dodge;55:62:big_health_dodge;45:58:str;48:58:str;49:60:armor_dodge;51:61:armor_dodge;51:63:armor_dodge;49:64:big_armor_dodge;47:63:armor_dodge;47:61:armor_dodge;49:62:big_health;32:50:str;29:51:str;29:53:health_armor;27:53:health_armor;27:55:big_health_armor;38:46:axe_damage;36:47:axe_damage;34:48:big_axe_damage;34:46:physical_weapon_damage;31:47:big_armor;34:44:big_sword_damage;32:61:nature_resist;31:64:nature_penetration;27:63:big_dex_gated;24:61:attack_speed;22:60:str;22:62:health_armor;20:62:health;20:64:big_health_armor;49:44:poison_ball;20:59:str;22:58:attack_speed;22:56:big_attack_speed;50:58:dex;53:58:dex;53:60:big_attack_speed;56:58:dex;60:55:dodge;62:53:big_str_gated;64:51:health;60:49:big_dodge;66:50:dex;68:47:multi_shot;66:46:big_dodge;64:55:armor_penetration_notable;70:48:big_health;61:47:health_dodge;63:46:big_health_dodge;66:53:dodge;68:55:dodge;66:57:recoil_shot;70:57:dodge;72:59:dodge;74:57:health_dodge;74:61:health;71:63:big_str_gated;77:70:iron_reflexes;76:57:health;76:55:big_health_dodge;76:60:dex;76:62:cdr;78:63:trickery;56:47:dodge;54:45:health;53:43:arrow_barrage");

        // ------------------------------------------------------------------ 2. Маг
        GuideDefinition mage = new GuideDefinition(
            "frost_mage", "Ледяной маг",
            "Интеллект и мудрость: лёд, контроль и магический щит",
            ChatFormatting.AQUA, "textures/gui/spells/schools/water.png", "water", 2)
            .stat("intelligence", 0.55F)
            .stat("wisdom", 0.25F)
            .stat("vitality", 0.20F)
            .spell("frostball", 16)
            .spell("frostball_chill", 8)
            .spell("frost_nova", 16)
            .spell("chilling_field", 12)
            .spell("ice_comet", 16)
            .spell("chill_eruption", 12)
            .spell("mage_circle", 8)
            .spell("frost_armor", 8)
            .tip("ИНТ: 1 очко = 1.08 силы заклинаний и 10.8 к трём стихийным сопротивлениям. 55 ИНТ = 59 силы заклинаний и 594 сопротивления (51.8%).")
            .tip("Урон заклинания = база × сила заклинаний (коэффициент растёт с уровнем заклинания до 16).")
            .tip("МУД: 1 очко = 21.6 маны. Мáгический щит тратит ману вместо HP — МУД это ваша «броня».")
            .tip("Заклинания: 2 очка за уровень, всего 100 на 50 ур. Максимум 16 уровней — не распыляйте очки.")
            .tip("Школу можно взять вторую, но очков хватит только на одну полноценную.")
            .tip("Магический щит не защищает от того, что бьёт по HP напрямую (яды, кровотечение).")
            .milestone("Ур. 1-5: start_int → frostball → magic_shield → быстрый каст.")
            .milestone("Ур. 5-10: mana, heart_of_ice, magic_shield/реген щита, int-узлы.")
            .milestone("Ур. 10-20: урон заклинаний, крит заклинаний, int-узлы, faster_cast_rate_notable.")
            .milestone("Ур. 20-30: cdr, spell_damage, big_spell_damage, int и mana.")
            .milestone("Ур. 30-40: elemental spell damage, крит, площадь, добор ИНТ.")
            .milestone("Ур. 40-50: big_crit_hit, big_health, mystic-узлы, добор ИНТ до 55 и МУД до 25.")
            .talentPlan("49:38:start_int;49:36:frostball;50:34:magic_shield;52:32:magic_shield;56:29:mana;54:27:heart_of_ice;60:25:magic_shield;54:25:magic_shield_regen;52:27:magic_shield_regen;62:27:big_dex_gated;64:29:magic_shield;60:31:int;56:33:magic_shield;56:25:heart_of_magic;60:28:faster_cast_rate_notable;52:25:heart_of_chill;51:30:faster_cast_rate;49:30:flower_of_ice;49:28:ice_flower_restore;49:32:ice_flower_dmg;50:25:big_health;54:35:critical_hit;53:37:lightning_totem;62:23:cdr;64:21:cdr;62:19:whirlpool;61:33:spell_damage;60:35:big_spell_damage;56:22:int;56:20:mana;58:19:fast_frostball;66:30:int;66:32:spell_thunder_damage;68:32:thunder_spear;68:34:spell_thunder_damage;66:27:int;68:25:spell_thunder_damage;66:23:health_mana;68:21:mana;70:19:big_health_mana;70:23:spell_thunder_damage;72:21:spell_thunder_damage;74:19:faster_cast_rate;71:17:big_dex_gated;77:10:ghost_reaver;67:16:faster_cast_rate;67:14:cdr;69:13:cdr;70:11:wizardry;54:19:water_weapon_damage;54:17:frostball_chill;58:35:big_crit_hit;53:22:int;50:22:int;48:22:int;49:19:health_mana;47:17:mana;47:15:mana;49:14:big_health_mana;45:22:int;44:25:water_resist;42:25:health_mana;46:25:chance_of_chill;48:25:big_chance_of_chill;42:22:int;38:25:spell_damage;36:27:big_str_gated;34:29:spell_damage;38:31:int;36:32:magic_shield;34:32:magic_shield;34:34:very_big_health_magic_shield;36:34:magic_shield;32:32:magic_shield;38:28:cdr_notable;42:33:faster_cast_rate;40:34:big_spell_damage;42:20:spell_water_damage;40:18:spell_water_damage;40:16:big_spell_water_damage;38:17:big_chance_of_chill;44:18:faster_cast_rate;44:35:faster_cast_rate;44:16:faster_cast_rate;36:23:critical_hit;34:21:critical_hit;32:23:tidal_wave;30:21:burning_currents;34:25:crashing_rocks;38:34:big_increase_healing;45:37:purifying_fires;32:19:critical_hit;31:16:spell_damage;27:17:big_str_gated;21:10:arcane_devotion;30:14:health;31:12:health_magic_shield;29:12:health_mana;43:38:armor;64:25:health;63:17:mana_regen");

        // ------------------------------------------------------------------ 3. Охотник
        GuideDefinition hunter = new GuideDefinition(
            "poison_hunter", "Охотник-отравитель",
            "Ловкость и крит: лук, скорость атаки, уклонение и яды",
            ChatFormatting.GREEN, "textures/gui/spells/schools/hunting.png", "hunting", 1)
            .stat("dexterity", 0.55F)
            .stat("vitality", 0.30F)
            .stat("strength", 0.15F)
            .spell("arrow_storm", 16)
            .spell("poison_arrow", 16)
            .spell("charged_bolt", 16)
            .spell("make_arrows", 1)
            .spell("dash", 8)
            .spell("backflip", 8)
            .spell("poison_trap", 8)
            .spell("fire_trap", 8)
            .spell("explosive_arrow", 16)
            .tip("ЛОВ: 1 очко = 10.8 уклонения и 1.08 физ. урона. 55 ЛОВ = 594 уклонения = 56.8%.")
            .tip("Уклонение — единственная защита, которая полностью обнуляет удар (но не работает против заклинаний).")
            .tip("Крит: средний урон = урон × (1 + шанс_крита × урон_крита). База 1% / 50%.")
            .tip("Скорость атаки множит весь урон лука: 10% скорости ≈ 10% ДПС, пока хватает энергии.")
            .tip("Удар из лука стоит 8 × NORMAL(ур.) = 86 энергии на 50 ур. — ЛВК (энергия) не пустой стат для охотника.")
            .tip("Яды (ELE_DOT) — отдельный множитель поверх физического урона, а не замена.")
            .milestone("Ур. 1-5: start_dex → arrow_barrage → health → dodge.")
            .milestone("Ур. 5-10: big_dodge, health_dodge, удар лука, big_bow_damage.")
            .milestone("Ур. 10-20: крит, multi_shot, big_health, уклонение.")
            .milestone("Ур. 20-30: пробивание брони, big_str_gated, iron_reflexes, poisoned_weapons.")
            .milestone("Ур. 30-40: скорость атаки, armor_dodge, уклонение и HP.")
            .milestone("Ур. 40-50: большие узлы уклонения/брони, poison-узлы, добор ЛОВ до 55.")
            .talentPlan("51:41:start_dex;53:43:arrow_barrage;54:45:health;56:47:dodge;60:49:big_dodge;61:47:health_dodge;63:46:big_health_dodge;64:44:physical_weapon_damage;64:42:big_bow_damage;64:51:health;66:50:dex;68:47:multi_shot;66:46:big_dodge;70:48:big_health;66:53:dodge;68:55:dodge;66:57:recoil_shot;62:53:big_str_gated;64:55:armor_penetration_notable;70:57:dodge;72:59:dodge;74:57:health_dodge;74:61:health;71:63:big_str_gated;77:70:iron_reflexes;76:57:health;76:55:big_health_dodge;76:60:dex;76:62:cdr;78:63:trickery;60:55:dodge;56:51:dex;55:54:poisoned_weapons;57:55:big_chance_of_poison;55:56:big_chance_of_poison;56:58:dex;55:60:health;55:62:big_health_dodge;53:54:big_attack_speed;55:64:dodge;53:65:health_dodge;51:66:health_dodge;49:66:big_armor_dodge;47:66:health_armor;45:65:health_armor;43:64:armor;43:62:big_health_armor;53:58:dex;50:58:dex;49:60:armor_dodge;51:61:armor_dodge;51:63:armor_dodge;49:64:big_armor_dodge;47:63:armor_dodge;47:61:armor_dodge;49:62:big_health;49:56:big_nature_resist;47:56:health_regen;45:54:big_health_regen;43:54:nature_balm;53:60:big_attack_speed;42:51:str;46:48:armor_dodge;48:46:armor_dodge;38:55:armor;36:53:big_dex_gated;34:55:attack_speed_notable;49:44:poison_ball;34:51:attack_speed;38:49:str;38:51:ground_slam;42:47:health;44:45:health;45:43:heavy_strike;40:45:big_health;40:42:physical_weapon_damage;40:40:cleave;36:57:nature_resist;34:59:nature_resist;36:61:thorn_armor;36:63:big_armor;38:64:big_armor;40:64:big_armor;40:62:health;38:62:big_health_armor;32:61:nature_resist;31:64:nature_penetration;27:63:big_dex_gated;67:64:spell_nature_damage;66:61:spell_nature_damage;64:59:spell_nature_damage;62:61:thorn_bush;62:63:nature_penetration;60:64:mod_slow_bush;60:60:big_spell_nature_damage;58:60:mod_poison_bush;58:62:spell_nature_damage;60:62:big_spell_nature_damage;49:54:big_nature_penetration;47:53:chance_of_poison;51:53:chance_of_poison");

        ALL.add(warrior);
        ALL.add(mage);
        ALL.add(hunter);
    }
}
