package com.robertx22.infinite_dungeons.database.db_types.layout;

import com.robertx22.infinite_dungeons.main.DungeonItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.robertx22.forge_compat.registries.ForgeRegistries;

public class ShopCost {

    public String item_id = "";
    public int count = 1;

    public ShopCost(String item_id, int count) {
        this.item_id = item_id;
        this.count = count;
    }

    public static ShopCost skeletalCoins(int count) {
        return new ShopCost(ForgeRegistries.ITEMS.getKey(DungeonItems.SKELETAL_COIN.get())
            .toString(), count);
    }

    public static ShopCost netherCoins(int count) {
        return new ShopCost(ForgeRegistries.ITEMS.getKey(DungeonItems.NETHER_COIN.get())
            .toString(), count);
    }

    public static ShopCost voidCoins(int count) {
        return new ShopCost(ForgeRegistries.ITEMS.getKey(DungeonItems.VOID_COIN.get())
            .toString(), count);
    }

    public static ShopCost of(Item item, int count) {
        return new ShopCost(ForgeRegistries.ITEMS.getKey(item)
            .toString(), count);
    }

    public boolean isItem(Item item) {
        return item == getItem();
    }

    public boolean canPay(Player player) {
        Item item = getItem();
        return player.getInventory().countItem(item) >= count;
    }

    public void pay(Player player) {
        Item item = getItem();

        int costToPay = this.count;

        for (int j = 0; j < player.getInventory().getContainerSize(); ++j) {
            if (costToPay < 1) {
                break;
            }

            ItemStack stack = player.getInventory().getItem(j);
            if (stack.getItem()
                .equals(item)) {
                int paid = Mth.clamp(stack.getCount(), 0, costToPay);
                costToPay -= paid;
                stack.shrink(paid);
            }
        }

    }

    public ItemStack getShowStack() {
        return new ItemStack(getItem(), count);
    }

    public Item getItem() {
        return ForgeRegistries.ITEMS.getValue(Identifier.parse(item_id));
    }
}
