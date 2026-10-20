package com.robertx22.divine_missions.database.task_types;

import com.robertx22.divine_missions.database.TaskTypeIds;
import com.robertx22.divine_missions.database.db_types.TaskType;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.saving.TaskData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import net.minecraft.core.registries.BuiltInRegistries;
public class KillSpecificMob extends TaskType {

    public KillSpecificMob() {
        super(TaskTypeIds.SPECIFIC_MOB_KILL);
    }

    @Override
    public MutableComponent getTranslatable(Player player, TaskData data) {
        EntityType type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(data.getTaskEntry().data));
        return Component.translatable(DivineMissions.MODID + ".task." + this.id).append(" ")
            .append(type.getDescription());

    }
}
