package com.robertx22.mine_and_slash.mmorpg.registers.common;

import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.mine_and_slash.database.data.spells.entities.*;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SkeletonSummon;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SpiderPet;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.WolfSummon;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.ZombieSummon;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.golems.ColdGolem;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.golems.FireGolem;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.golems.LightningGolem;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.Def;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
    public static RegObj<EntityType<WolfSummon>> SPIRIT_WOLF = mob(WolfSummon::new, 0.6F, 0.85F, "spirit_wolf");
    public static RegObj<EntityType<ZombieSummon>> ZOMBIE = mob(ZombieSummon::new, 0.6F, 1.99F, "zombie");
    public static RegObj<EntityType<SkeletonSummon>> SKELETON = mob(SkeletonSummon::new, 0.6F, 1.99F, "skeleton");
    public static RegObj<EntityType<SpiderPet>> SPIDER = mob(SpiderPet::new, 1.4F, 0.9F, "spider");

    public static RegObj<EntityType<FireGolem>> FIRE_GOLEM = mob(FireGolem::new, 0.6F, 0.85F, "fire_golem");
    public static RegObj<EntityType<ColdGolem>> COLD_GOLEM = mob(ColdGolem::new, 0.6F, 0.85F, "cold_golem");
    public static RegObj<EntityType<LightningGolem>> LIGHTNING_GOLEM = mob(LightningGolem::new, 0.6F, 0.85F, "lightning_golem");

    private static <T extends Entity> RegObj<EntityType<T>> projectile(EntityType.EntityFactory<T> factory, String id) {
        return projectile(factory, id, true);
    }

    private static <T extends Entity> RegObj<EntityType<T>> mob(EntityType.EntityFactory<T> factory, float width, float height, String id) {
        return Def.entity(id, () -> EntityType.Builder.of(factory, MobCategory.MISC)
            .sized(width, height)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, SlashRef.id(id))));
    }

    private static <T extends Entity> RegObj<EntityType<T>> projectile(EntityType.EntityFactory<T> factory, String id, boolean itemRender) {
        return Def.entity(id, () -> EntityType.Builder.of(factory, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .updateInterval(10)
            .clientTrackingRange(4)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, SlashRef.id(id))));
    }

    public static void init() {
    }
}
