package com.robertx22.mine_and_slash.mmorpg.registers.common;

import net.minecraft.world.entity.EntityTypes;
import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.mine_and_slash.database.data.spells.entities.*;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SkeletonSummon;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SpiderPet;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.WolfSummon;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.ZombieSummon;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.golems.ColdGolem;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.golems.FireGolem;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.golems.LightningGolem;
import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.Def;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class SlashEntities {


    public static RegObj<EntityType<SimpleProjectileEntity>> SIMPLE_PROJECTILE = projectile(SimpleProjectileEntity::new, "spell_projectile");
    public static RegObj<EntityType<SimpleArrowEntity>> SIMPLE_ARROW = projectile(SimpleArrowEntity::new, "spell_arrow");
    public static RegObj<EntityType<StationaryFallingBlockEntity>> SIMPLE_BLOCK_ENTITY = projectile(StationaryFallingBlockEntity::new, "spell_block_entity", false);
    public static RegObj<EntityType<SimpleTridentEntity>> SIMPLE_TRIDENT = projectile(SimpleTridentEntity::new, "spell_trident", false);
    public static RegObj<EntityType<AutoAimingProj>> AUTO_AIMING_SKELETON_SKULL = projectile(AutoAimingProj::new, "auto_aim_skull", false);


    // summons
    public static RegObj<EntityType<WolfSummon>> SPIRIT_WOLF = mob(WolfSummon::new, EntityTypes.WOLF, "spirit_wolf");
    public static RegObj<EntityType<ZombieSummon>> ZOMBIE = mob(ZombieSummon::new, EntityTypes.SKELETON, "zombie");
    public static RegObj<EntityType<SkeletonSummon>> SKELETON = mob(SkeletonSummon::new, EntityTypes.SKELETON, "skeleton");
    public static RegObj<EntityType<SpiderPet>> SPIDER = mob(SpiderPet::new, EntityTypes.CAVE_SPIDER, "spider");

    public static RegObj<EntityType<FireGolem>> FIRE_GOLEM = mob(FireGolem::new, EntityTypes.WOLF, "fire_golem");
    public static RegObj<EntityType<ColdGolem>> COLD_GOLEM = mob(ColdGolem::new, EntityTypes.WOLF, "cold_golem");
    public static RegObj<EntityType<LightningGolem>> LIGHTNING_GOLEM = mob(LightningGolem::new, EntityTypes.WOLF, "lightning_golem");


    private static <T extends Entity> RegObj<EntityType<T>> projectile(EntityType.EntityFactory<T> factory, String id) {
        return projectile(factory, id, true);

    }

    private static <T extends Entity> RegObj<EntityType<T>> mob(EntityType.EntityFactory<T> factory, EntityType like, String id) {

        RegObj<EntityType<T>> def = Def.entity(id, () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(like.getDimensions().width(), like.getDimensions().height())
                .clientTrackingRange(10)
                .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, com.robertx22.mine_and_slash.mmorpg.SlashRef.id(id))));


        return def;
    }

    private static <T extends Entity> RegObj<EntityType<T>> projectile(EntityType.EntityFactory<T> factory,
                                                                       String id, boolean itemRender) {

        RegObj<EntityType<T>> def = Def.entity(id, () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .updateInterval(10)
                .clientTrackingRange(4)
                .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, com.robertx22.mine_and_slash.mmorpg.SlashRef.id(id))));

        return def;
    }

    public static void init() {

    }

}


