package com.robertx22.addon.divine_missions.types;

import com.robertx22.divine_missions.database.db_types.TaskType;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.saving.TaskData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

public class CompleteDungeonTask extends TaskType {
    public static String ID = "complete_dungeon";

    public CompleteDungeonTask() {
        super(ID);
    }

    @Override
    public MutableComponent getTranslatable(Player playerEntity, TaskData taskData) {
        return DivineMissions.ofTranslation("complete_dungeon")
            .copy().withStyle(ChatFormatting.DARK_PURPLE);
    }
}
