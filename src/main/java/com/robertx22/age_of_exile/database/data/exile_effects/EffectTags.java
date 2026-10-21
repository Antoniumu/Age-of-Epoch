package com.robertx22.age_of_exile.database.data.exile_effects;

public enum EffectTags {
    immobilizing("Обездвиживание"),
    offensive("Атакующий"),
    curse("Проклятие"),
    food("Еда"),
    defensive("Защитный"),
    positive("Положительный"),
    song("Песнь"),
    negative("Отрицательный"),
    heal_over_time("Периодическое исцеление");

    String name;

    EffectTags(String name) {
        this.name = name;
    }

    public String getLocName() {
        return name;
    }
}
