package com.robertx22.divine_missions.mission_gen;

import com.robertx22.divine_missions.components.PlayerMissionCap;
import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.divine_missions.database.WorthTypeIds;
import com.robertx22.divine_missions.database.db_types.*;
import com.robertx22.divine_missions.item.MissionItem;
import com.robertx22.divine_missions.main.ModItems;
import com.robertx22.divine_missions.saving.MissionItemData;
import com.robertx22.divine_missions.saving.RewardData;
import com.robertx22.divine_missions.saving.TaskData;
import com.robertx22.divine_missions.util.GodWeight;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
public class MissionUtil {

    public static ItemStack createRandomMissionItem(Player player) {

        MissionItemData data = null;
        try {
            data = createRandom(player);
        } catch (Exception e) {
            e.printStackTrace();
            data = createRandom(player);
        }

        ItemStack stack = new ItemStack(ModItems.MISSION_ITEM.get());

        MissionItemData.SAVER.saveTo(stack, data);

        int timelimit = data.tasks.stream()
            .mapToInt(x -> x.getTaskEntry().seconds * x.req)
            .sum() * 20;

        StackNBT.get(stack)
            .putInt(MissionItem.TICKS, timelimit);

        return stack;
    }

    public static MissionItemData createRandom(Player player) {

        MissionItemData data = new MissionItemData();

        PlayerMissionCap reps = PlayerMissionCap.get(player);

        List<GodWeight> weights = new ArrayList<>();
        for (God god : MissionsDB.Gods()
            .getList()) {
            int weight = (int) (god.Weight() * reps.getReputationLevel(god).weight_multi);
            weights.add(new GodWeight(god, weight));
        }
        God god = RandomUtils.weightedRandom(weights).god;
        ReputationLevel rep = reps.getReputationLevel(god);
        MissionRarity rar = RandomUtils.weightedRandom(MissionsDB.MissionRarities()
            .getList());

        if (RandomUtils.roll(rep.chance_for_higher_rar)) {
            int higher = rar.rank + 1;
            Optional<MissionRarity> opt = MissionsDB.MissionRarities()
                .getList()
                .stream()
                .filter(x -> x.rank == higher)
                .findFirst();

            if (opt.isPresent()) {
                rar = opt.get();
            }
        }

        data.rar = rar.id;

        List<Pool> touse = god.getRandomPoolsToUse(player, Pool.PoolType.TASKS, rar, x -> true);

        WorthsData worths = new WorthsData();

        for (Pool x : touse) {

            List<String> alreadyUsed = data.tasks.stream()
                .map(e -> e.id)
                .collect(Collectors.toList());
            List<TaskEntry> possible = x.getTaskEntries()
                .stream()
                .filter(e -> e.getType() != null)
                .filter(e -> !alreadyUsed.contains(e.id))
                .collect(Collectors.toList());

            TaskEntry task = RandomUtils.weightedRandom(possible);
            if (task != null) {

                int count = task.getType()
                    .getRandomTaskCount(task, rar);

                TaskData taskData = TaskData.createNew(task, count);
                data.tasks.add(taskData);

                worths.add(task.worths, taskData.req, rar.reward_multi);
            }
        }

        data.god = god.GUID();

        int tries = 0;

        while (data.rewards.size() < 1) {
            tries++;
            if (tries > 5) {
                worths.add(WorthTypeIds.DEFAULT, 10);
            }
            if (tries > 10) {
                break;
            }
            addRewards(data, worths, player, rar);
        }

        spendRemainingWorth(data, worths, player);

        data.rep = worths.totalReduced / 100;

        return data;
    }

    private static void spendRemainingWorth(MissionItemData data, WorthsData worths, Player player) {

        data.rewards.forEach(x -> {
            if (x.getReward().max > x.count) {

                int canadd = worths.getWorth(x.getReward().worth_type) / x.getSingleWorth();

                if (canadd > 0) {

                    int max = x.getReward().max - x.count;

                    if (canadd > max) {
                        canadd = max;
                    }

                    x.count += canadd;

                    worths.reduce(x.getReward().worth_type, canadd * x.getSingleWorth());
                }
            }

        });

    }

    private static void addRewards(MissionItemData data, WorthsData worths, Player player, MissionRarity rar) {
        // todo add limit with total worth
        List<Pool> rewardPools = data.getGod()
            .getRandomPoolsToUse(player, Pool.PoolType.REWARDS, rar, x -> {
                return x.getRewards()
                    .stream()
                    .anyMatch(e -> {
                        return worths.getWorth(e.worth_type) > e.worth * e.min;
                    });
            });
        for (Pool x : rewardPools) {
            if (data.rewards.size() > 3) {
                continue;
            }

            List<String> alreadyUsed = data.rewards.stream()
                .map(e -> e.id)
                .collect(Collectors.toList());
            List<Reward> possible = x.getRewards()
                .stream()
                .filter(e -> worths.getWorth(e.worth_type) > e.worth * e.min)
                .filter(e -> !alreadyUsed.contains(e.id))
                .collect(Collectors.toList());

            if (possible.isEmpty()) {
                continue;
            }

            Reward reward = RandomUtils.weightedRandom(possible);
            RewardData rewardData = new RewardData();

            int max = worths.getWorth(reward.worth_type) / reward.worth;

            rewardData.id = reward.GUID();
            rewardData.count = (int) (RandomUtils.RandomRange(reward.min, reward.max) * rar.reward_multi);

            if (rewardData.count > max) {
                rewardData.count = max;
            }

            if (rewardData.count > 1000) { // round up a bit
                int hundreds = rewardData.count / 100;
                rewardData.count = hundreds * 100;
            } else if (rewardData.count > 50) { // round up a bit
                int tens = rewardData.count / 10;
                rewardData.count = tens * 10;
            }

            worths.reduce(reward.worth_type, rewardData.count * reward.worth);

            if (rewardData.count > 0) {
                data.rewards.add(rewardData);
            }

        }
    }

    public static List<ItemStack> getCurrentMissions(Player player) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack x = player.getInventory().getItem(i);
            if (x.getItem() == ModItems.MISSION_ITEM.get()) {
                result.add(x);
            }
        }
        return result;
    }

    public static void tryDoMissions(Player player, Consumer<TaskData> attempt) {

        for (ItemStack stack : MissionUtil.getCurrentMissions(player)) {

            MissionItemData data = MissionItemData.SAVER.loadFrom(stack);

            if (data != null) {

                for (TaskData task : data.tasks) {

                    int before = task.curr;
                    if (task.isDone(player)) {
                        continue;
                    }
                    attempt.accept(task);

                    if (before != task.curr) {
                        MissionItemData.SAVER.saveTo(stack, data);
                        return;
                    }
                }
            }
        }

    }

}
