package com.robertx22.infinite_dungeons.database.db_types.layout;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.robertx22.infinite_dungeons.exile_events.IDExileEvents;
import com.robertx22.infinite_dungeons.exile_events.OnItemGivenEvent;
import com.robertx22.infinite_dungeons.item.RewardCrateItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import com.robertx22.forge_compat.registries.ForgeRegistries;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
public class ShopReward {

    public enum Type {
        ITEM {
            @Override
            public ItemStack getStack(ShopReward reward) {
                ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(reward.value)), reward.count);

                if (!reward.data.isEmpty()) {
                    CompoundTag nbt = null;
                    try {
                        nbt = new TagParser(new StringReader(reward.data)).readStruct();
                    } catch (CommandSyntaxException e) {
                        e.printStackTrace();
                    }
                    StackNBT.set(stack, nbt);

                }
                return stack;
            }

            @Override
            public void giveReward(ShopReward reward, Player player) {
                giveItem(getStack(reward), player);
            }
        };

        public Component getStackNameForTooltip(ShopReward reward) {

            ItemStack stack = getStack(reward);

            return Component.literal(stack.getCount() + "x ").append(stack.getHoverName());

        }

        public abstract ItemStack getStack(ShopReward reward);

        public abstract void giveReward(ShopReward reward, Player player);

    }

    public static ShopReward item(Item item, int count) {
        return new ShopReward(ForgeRegistries.ITEMS.getKey(item)
            .toString(), Type.ITEM, count);
    }

    public static ShopReward nbtItem(ItemStack stack, int count) {
        ShopReward reward = item(stack.getItem(), count);

        // reward.data = MyGSON.GSON.toJson(StackNBT.get(stack));

        reward.data = StackNBT.get(stack)
            .toString();

        return reward;
    }

    public ShopReward(String value, Type type, int count) {
        this.value = value;
        this.type = type;
        this.count = count;
    }

    public String value = "";
    public String data = "";
    public Type type = Type.ITEM;
    public int count = 1;

    public void give(Player player) {
        type.giveReward(this, player);
    }

    public static void giveItem(ItemStack stack, Player player) {

        if (stack.getItem() instanceof RewardCrateItem) {
            StackNBT.getOrCreate(stack)
                .putBoolean(OnItemGivenEvent.IS_PRODUCER_BOOLEAN_TAG, true);
        }

        OnItemGivenEvent event = new OnItemGivenEvent(stack, player);
        IDExileEvents.ON_ITEM_GIVE.callEvents(event);

        if (player.addItem(stack) == false) {
            player.spawnAtLocation(stack, 1F);
        }

        player.getInventory().setChanged();

    }

}
