package com.robertx22.divine_missions.database.task_types;

import com.robertx22.divine_missions.database.TaskTypeIds;
import com.robertx22.divine_missions.database.db_types.TaskType;
import com.robertx22.divine_missions.saving.TaskData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BringItem extends TaskType {

    public BringItem() {
        super(TaskTypeIds.BRING_ITEM);
    }

    @Override
    public boolean isTaskDone(TaskData data, Player player) {

        return super.isTaskDone(data, player);
    }

    @Override
    public void spendItems(Player player, TaskData data) {

        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(data.getTaskEntry().data));

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (data.curr < data.req) {
                int tospend = data.req - data.curr;

                if (stack.getItem() == item) {
                    int spend = Mth.clamp(stack.getCount(), 0, tospend);
                    stack.shrink(spend);
                    data.curr += spend;
                }

            }
        }

    }

    @Override
    public int getCurrentDoneTooltip(Player player, TaskData data) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(data.getTaskEntry().data));
        int inInv = player.getInventory().countItem(item);
        int current = Mth.clamp(inInv + data.curr, 0, data.req);
        return current;
    }

    @Override
    public boolean isTaskDoneTooltip(TaskData data, Player player) {
        return getCurrentDoneTooltip(player, data) >= data.req;
    }

    @Override
    public MutableComponent getTranslatable(Player player, TaskData data) {

        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(data.getTaskEntry().data));

        return Component.literal("").append(Component.translatable(item.getDescriptionId()));
    }

}
