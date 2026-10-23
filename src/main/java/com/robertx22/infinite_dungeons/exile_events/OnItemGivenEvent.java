package com.robertx22.infinite_dungeons.exile_events;

import com.robertx22.library_of_exile.events.base.ExileEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
public class OnItemGivenEvent extends ExileEvent {

    public static String IS_PRODUCER_BOOLEAN_TAG = "is_producer";

    public ItemStack producerStack;
    public ItemStack stack;
    public Player player;
    public LivingEntity mob;
    public BlockPos pos;

    public OnItemGivenEvent(ItemStack stack, Player player) {
        this.stack = stack;
        this.player = player;
        this.pos = player.blockPosition();
    }

    public OnItemGivenEvent setMadeFromStack(ItemStack producer) {
        this.producerStack = producer;
        return this;
    }

    public static boolean isProducer(ItemStack stack) {
        return StackNBT.has(stack) && StackNBT.getOrCreate(stack)
            .getBooleanOr(IS_PRODUCER_BOOLEAN_TAG, false);
    }

    public boolean hasMobSource() {
        return mob != null;
    }

}
