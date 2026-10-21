package com.robertx22.world_of_exile.tower;

import java.util.List;

/**
 * One boss (or champion) definition.  Everything is data: the entity type is
 * a vanilla id, the multipliers scale the base mob attributes and the ability
 * list drives {@link TowerAbility}.
 */
public record TowerBossDef(
    String id,
    String name,
    String entity,
    int rank,
    double health,
    double damage,
    double speed,
    double armor,
    String weapon,
    List<TowerAbility> abilities
) {

    public static TowerBossDef of(
        String id,
        String name,
        String entity,
        int rank,
        double health,
        double damage,
        TowerAbility... abilities
    ) {
        return new TowerBossDef(id, name, entity, rank, health, damage, 1.0D, 2.0D + rank * 2.0D, "", List.of(abilities));
    }

    public TowerBossDef withWeapon(String weaponId) {
        return new TowerBossDef(id, name, entity, rank, health, damage, speed, armor, weaponId, abilities);
    }

    public TowerBossDef withSpeed(double value) {
        return new TowerBossDef(id, name, entity, rank, health, damage, value, armor, weapon, abilities);
    }

    public TowerBossDef withArmor(double value) {
        return new TowerBossDef(id, name, entity, rank, health, damage, speed, value, weapon, abilities);
    }
}
