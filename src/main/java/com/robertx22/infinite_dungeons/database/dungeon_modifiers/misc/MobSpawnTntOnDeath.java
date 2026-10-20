package com.robertx22.infinite_dungeons.database.dungeon_modifiers.misc;

import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.DungeonModifier;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.ModifierType;
import com.robertx22.infinite_dungeons.database.ids.DungeonModifierIds;
import com.robertx22.infinite_dungeons.entity.TickingTNTEntity;
import com.robertx22.infinite_dungeons.main.DungeonEntities;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.world.entity.LivingEntity;

public class MobSpawnTntOnDeath extends DungeonModifier {

    public MobSpawnTntOnDeath() {
        super(DungeonModifierIds.MOB_DEATH_TNT);
        this.weight = 250;
    }

    @Override
    public ModifierType getModifierType() {
        return ModifierType.tntDeathMobs(2);
    }

    @Override
    public void onControlBlockTick(ControlBlockEntity en) {

    }

    @Override
    public void onMobSpawn(LivingEntity en) {

    }

    @Override
    public void onMobDeath(ExileEvents.OnMobDeath event) {

        LivingEntity en = event.mob;
        TickingTNTEntity tnt = (TickingTNTEntity) DungeonEntities.TICKING_TNT.get()
            .create(event.mob.level);

        tnt.setPos(en.getX(), en.getY(), en.getZ());

        en.level.addFreshEntity(tnt);

    }

}
