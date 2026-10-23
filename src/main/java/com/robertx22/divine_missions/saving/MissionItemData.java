package com.robertx22.divine_missions.saving;

import com.robertx22.divine_missions.components.PlayerMissionCap;
import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.divine_missions.database.db_types.God;
import com.robertx22.divine_missions.database.db_types.MissionRarity;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.library_of_exile.utils.ItemstackDataSaver;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

@Storable
public class MissionItemData {

    public static ItemstackDataSaver<MissionItemData> SAVER = new ItemstackDataSaver<>("missions", MissionItemData.class, () -> new MissionItemData());

    @Store
    public List<TaskData> tasks = new ArrayList<>();
    @Store
    public List<RewardData> rewards = new ArrayList<>();
    @Store
    public String god = "";
    @Store
    public String rar = "";
    @Store
    public int rep = 0;

    public God getGod() {
        return MissionsDB.Gods()
            .get(god);
    }

    public MissionRarity getRarity() {
        return MissionsDB.MissionRarities()
            .get(rar);
    }

    public void spendItems(Player player) {
        this.tasks.forEach(x -> x.getTaskType()
            .spendItems(player, x));
    }

    public void giveRewards(Player player) {

        int repreward = rep * 2;

        PlayerMissionCap.get(player)
            .addReputation(repreward, getGod());

        try {
            this.rewards.forEach(x -> x.give(player));
        } catch (Exception e) {
            e.printStackTrace();
        }

        player.sendSystemMessage(Component.literal("+" + repreward + " ").append(getGod().getTranslatable())
            .append(" ")
            .append(Component.translatable(DivineMissions.MODID + ".aspect"))
            .append(" ")
            .append(Component.translatable(DivineMissions.MODID + ".reputation"))
            .withStyle(ChatFormatting.GREEN));

    }

    public boolean canFinishTooltip(Player player) {
        return tasks.stream()
            .allMatch(x -> {
                return x.isDoneTooltip(player);
            });
    }

    public boolean canFinish(Player player) {
        return tasks.stream()
            .allMatch(x -> x.isDone(player));
    }

}
