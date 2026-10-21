#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Добавляет ключи локализации древа «Божественный» в ru_ru.json и en_us.json.

Ключи вставляются внутрь своих групп (mmorpg.talent.*, mmorpg.stat.*, mmorpg.stat_desc.*,
mmorpg.one_of_a_kind.*, mmorpg.branch.*), а не в конец файла, чтобы не ломать группировку.

Запуск:  python3 tools/talent_gen/add_divine_lang.py [--check]
  --check — только проверить, что все нужные ключи на месте.
"""

import json
import os
import sys
from collections import OrderedDict

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
LANG_DIR = os.path.join(REPO, "src", "main", "resources", "assets", "mmorpg", "lang")

RU = {
    # --- таланты ---
    "mmorpg.talent.divine_necromancer": "Путь некроманта",
    "mmorpg.talent.div_necro_raise": "Восстание мёртвых",
    "mmorpg.talent.div_necro_legion": "Легион",
    "mmorpg.talent.div_necro_fury": "Холодная ярость",
    "mmorpg.talent.div_necro_flesh": "Гнилая плоть",
    "mmorpg.talent.div_necro_lord": "Властелин мертвецов",
    "mmorpg.talent.div_necro_corpse": "Взрыв трупа",
    "mmorpg.talent.div_necro_chain_death": "Цепная смерть",

    "mmorpg.talent.divine_summoner": "Путь призывателя",
    "mmorpg.talent.div_summ_wolves": "Стая волков",
    "mmorpg.talent.div_summ_bear": "Медведь-хранитель",
    "mmorpg.talent.div_summ_mammoth": "Боевой мамонт",
    "mmorpg.talent.div_summ_fangs": "Звериный оскал",
    "mmorpg.talent.div_summ_paws": "Быстрые лапы",
    "mmorpg.talent.div_summ_hide": "Густая шерсть",
    "mmorpg.talent.div_summ_alpha": "Вожак стаи",

    "mmorpg.talent.div_shared_damage": "Узы власти",
    "mmorpg.talent.div_shared_health": "Духовная связь",
    "mmorpg.talent.div_shared_duration": "Долгий срок",
    "mmorpg.talent.div_shared_armor": "Костяной панцирь",
    "mmorpg.talent.div_shared_speed": "Резвость слуг",
    "mmorpg.talent.div_shared_damage_big": "Воля хозяина",
    "mmorpg.talent.div_shared_health_big": "Плоть и кость",
    "mmorpg.talent.divine_command": "Власть над слугами",

    # --- характеристики ---
    "mmorpg.stat.summon_damage": "Урон прислужников",
    "mmorpg.stat.summon_health": "Здоровье прислужников",
    "mmorpg.stat.summon_duration": "Время жизни прислужников",
    "mmorpg.stat.summon_speed": "Скорость прислужников",
    "mmorpg.stat.summon_armor": "Броня прислужников",
    "mmorpg.stat.max_undead_minions": "Предел нежити",
    "mmorpg.stat.undead_minion_damage": "Урон нежити",
    "mmorpg.stat.max_beast_minions": "Предел зверей",
    "mmorpg.stat.beast_minion_damage": "Урон зверей",
    "mmorpg.stat.beast_pack_wolves": "Призыв: волки",
    "mmorpg.stat.beast_pack_bear": "Призыв: медведь",
    "mmorpg.stat.beast_pack_mammoth": "Призыв: мамонт",
    "mmorpg.stat.raise_dead_chance": "§7§a[VAL1]§7% шанс поднять убитого врага в виде нежити",
    "mmorpg.stat.corpse_explosion": "§7Убитые вами враги взрываются, нанося §a[VAL1]§7% своего здоровья уроном вокруг",

    "mmorpg.stat_desc.summon_damage": "Урон всех ваших прислужников.",
    "mmorpg.stat_desc.summon_health": "Здоровье всех ваших прислужников.",
    "mmorpg.stat_desc.summon_duration": "Как долго живут ваши прислужники, прежде чем рассеяться.",
    "mmorpg.stat_desc.summon_speed": "Скорость передвижения всех ваших прислужников.",
    "mmorpg.stat_desc.summon_armor": "Броня всех ваших прислужников.",
    "mmorpg.stat_desc.max_undead_minions": "Сколько поднятых мертвецов может следовать за вами одновременно.",
    "mmorpg.stat_desc.undead_minion_damage": "Урон поднятых вами мертвецов.",
    "mmorpg.stat_desc.max_beast_minions": "Сколько призванных зверей держится рядом с вами.",
    "mmorpg.stat_desc.beast_minion_damage": "Урон призванных вами зверей.",
    "mmorpg.stat_desc.beast_pack_wolves": "Вы призываете волков. Одновременно можно держать только один вид зверей.",
    "mmorpg.stat_desc.beast_pack_bear": "Вы призываете медведя. Одновременно можно держать только один вид зверей.",
    "mmorpg.stat_desc.beast_pack_mammoth": "Вы призываете боевого мамонта. Одновременно можно держать только один вид зверей.",
    "mmorpg.stat_desc.raise_dead_chance": "Поднятый мертвец сражается на вашей стороне, пока не рассеется.",
    "mmorpg.stat_desc.corpse_explosion": "Урон считается от здоровья трупа и проходит через обычные правила урона.",

    # --- группы выбора ---
    "mmorpg.one_of_a_kind.divine_beast": "Вид призванного зверя",
    "mmorpg.branch.necromancer": "Некромант",
    "mmorpg.branch.summoner": "Призыватель",
}

EN = {
    "mmorpg.talent.divine_necromancer": "Path of the Necromancer",
    "mmorpg.talent.div_necro_raise": "Raise the Dead",
    "mmorpg.talent.div_necro_legion": "Legion",
    "mmorpg.talent.div_necro_fury": "Cold Fury",
    "mmorpg.talent.div_necro_flesh": "Rotten Flesh",
    "mmorpg.talent.div_necro_lord": "Lord of the Dead",
    "mmorpg.talent.div_necro_corpse": "Corpse Explosion",
    "mmorpg.talent.div_necro_chain_death": "Chain of Death",

    "mmorpg.talent.divine_summoner": "Path of the Summoner",
    "mmorpg.talent.div_summ_wolves": "Wolf Pack",
    "mmorpg.talent.div_summ_bear": "Guardian Bear",
    "mmorpg.talent.div_summ_mammoth": "War Mammoth",
    "mmorpg.talent.div_summ_fangs": "Beast Fangs",
    "mmorpg.talent.div_summ_paws": "Swift Paws",
    "mmorpg.talent.div_summ_hide": "Thick Hide",
    "mmorpg.talent.div_summ_alpha": "Alpha of the Pack",

    "mmorpg.talent.div_shared_damage": "Bonds of Command",
    "mmorpg.talent.div_shared_health": "Spirit Link",
    "mmorpg.talent.div_shared_duration": "Long Service",
    "mmorpg.talent.div_shared_armor": "Bone Plating",
    "mmorpg.talent.div_shared_speed": "Servant Swiftness",
    "mmorpg.talent.div_shared_damage_big": "Master's Will",
    "mmorpg.talent.div_shared_health_big": "Flesh and Bone",
    "mmorpg.talent.divine_command": "Dominion Over Servants",

    "mmorpg.stat.summon_damage": "Minion Damage",
    "mmorpg.stat.summon_health": "Minion Health",
    "mmorpg.stat.summon_duration": "Minion Duration",
    "mmorpg.stat.summon_speed": "Minion Speed",
    "mmorpg.stat.summon_armor": "Minion Armor",
    "mmorpg.stat.max_undead_minions": "Max Undead Minions",
    "mmorpg.stat.undead_minion_damage": "Undead Minion Damage",
    "mmorpg.stat.max_beast_minions": "Max Beast Minions",
    "mmorpg.stat.beast_minion_damage": "Beast Minion Damage",
    "mmorpg.stat.beast_pack_wolves": "Summon: Wolves",
    "mmorpg.stat.beast_pack_bear": "Summon: Bear",
    "mmorpg.stat.beast_pack_mammoth": "Summon: Mammoth",
    "mmorpg.stat.raise_dead_chance": "§7§a[VAL1]§7% chance to raise an enemy you slay as undead",
    "mmorpg.stat.corpse_explosion": "§7Enemies you slay explode, dealing §a[VAL1]§7% of their health as damage around them",

    "mmorpg.stat_desc.summon_damage": "Damage of all your minions.",
    "mmorpg.stat_desc.summon_health": "Health of all your minions.",
    "mmorpg.stat_desc.summon_duration": "How long your minions live before fading away.",
    "mmorpg.stat_desc.summon_speed": "Movement speed of all your minions.",
    "mmorpg.stat_desc.summon_armor": "Armor of all your minions.",
    "mmorpg.stat_desc.max_undead_minions": "How many raised dead can follow you at once.",
    "mmorpg.stat_desc.undead_minion_damage": "Damage of the dead you raise.",
    "mmorpg.stat_desc.max_beast_minions": "How many summoned beasts stay by your side.",
    "mmorpg.stat_desc.beast_minion_damage": "Damage of your summoned beasts.",
    "mmorpg.stat_desc.beast_pack_wolves": "You summon wolves. Only one beast type can be kept at a time.",
    "mmorpg.stat_desc.beast_pack_bear": "You summon a bear. Only one beast type can be kept at a time.",
    "mmorpg.stat_desc.beast_pack_mammoth": "You summon a war mammoth. Only one beast type can be kept at a time.",
    "mmorpg.stat_desc.raise_dead_chance": "The raised dead fights for you until it fades.",
    "mmorpg.stat_desc.corpse_explosion": "Damage is based on the corpse's health and goes through the normal damage rules.",

    "mmorpg.one_of_a_kind.divine_beast": "Summoned Beast",
    "mmorpg.branch.necromancer": "Necromancer",
    "mmorpg.branch.summoner": "Summoner",
}

# после ключа с таким префиксом вставляем новую группу (ищем последний ключ префикса)
GROUP_PREFIXES = ["mmorpg.talent.", "mmorpg.stat.", "mmorpg.stat_desc.", "mmorpg.one_of_a_kind.", "mmorpg.branch."]


def merge(path, new_keys, check_only):
    with open(path, encoding="utf-8") as f:
        data = json.load(f, object_pairs_hook=OrderedDict)

    missing = [k for k in new_keys if k not in data]

    if check_only:
        return missing

    if not missing:
        return missing

    result = OrderedDict()

    for key, value in data.items():
        result[key] = value

        # вставляем недостающие ключи группы сразу за последним ключом этой группы
        for prefix in GROUP_PREFIXES:
            if not key.startswith(prefix):
                continue
            to_add = [k for k in missing if k.startswith(prefix)]
            if not to_add:
                continue
            # добавляем только если следующий ключ файла уже из другой группы
            keys = list(data.keys())
            idx = keys.index(key)
            nxt = keys[idx + 1] if idx + 1 < len(keys) else None
            if nxt is None or not nxt.startswith(prefix):
                for k in sorted(to_add):
                    result[k] = new_keys[k]

    # если какая-то группа в файле вообще отсутствовала — дописываем в конец
    for k in sorted(missing):
        if k not in result:
            result[k] = new_keys[k]

    with open(path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
        f.write("\n")

    return missing


def main():
    check_only = "--check" in sys.argv

    for name, table in (("ru_ru", RU), ("en_us", EN)):
        path = os.path.join(LANG_DIR, name + ".json")
        missing = merge(path, table, check_only)

        if check_only:
            if missing:
                print("%s: не хватает %d ключей (например, %s)" % (name, len(missing), missing[0]))
            else:
                print("%s: все %d ключей на месте" % (name, len(table)))
        else:
            print("%s: добавлено %d ключей" % (name, len(missing)))


if __name__ == "__main__":
    main()
