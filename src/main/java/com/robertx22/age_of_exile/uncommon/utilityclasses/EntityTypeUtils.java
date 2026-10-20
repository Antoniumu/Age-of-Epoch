package com.robertx22.age_of_exile.uncommon.utilityclasses;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.entity.MobCategory;
public class EntityTypeUtils {

    public enum MobCategory {
        MOB("mob"),
        PLAYER("player"),
        ANIMAL("animal"),
        NPC("npc"),
        OTHER("other");

        MobCategory(String id) {
            this.id = id;
        }

        public String id;

    }

    public static MobCategory getType(LivingEntity entity) {

        if (isMob(entity)) {
            return MobCategory.MOB;
        } else if (isAnimal(entity)) {
            return MobCategory.ANIMAL;
        } else if (isNPC(entity)) {
            return MobCategory.NPC;
        } else if (entity instanceof Player) {
            return MobCategory.PLAYER;
        } else {
            return MobCategory.OTHER;
        }

    }

    public static boolean isMob(Entity en) {
        if (en instanceof Enemy) {
            return true;
        }
        if (en instanceof IAngerable) {
            return true;
        }
        if (!en.getType()
            .getCategory()
            .isFriendly()) {
            return true;
        }
        if (EntityType.getKey(en.getType())
            .getNamespace()
            .equals(SlashRef.WORLD_OF_EXILE_ID)) {
            return true; // all my mobs are supposed to be rewarding
        }

        return false;
    }

    public static boolean isAnimal(Entity en) {
        return en instanceof Animal;
    }

    /**
     * has to be checked first because inpc extends ianimals
     *
     * @param en
     * @return
     */
    public static boolean isNPC(Entity en) {

        return en instanceof INPC;

    }
}
