package com.robertx22.divine_missions.database.reward_types;

import com.robertx22.divine_missions.database.RewardTypeIds;
import com.robertx22.divine_missions.database.db_types.RewardType;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.saving.RewardData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextParamSets;
import net.minecraft.world.level.storage.loot.LootContextParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public class LootTableRewardType extends RewardType {

    public LootTableRewardType() {
        super(RewardTypeIds.LOOT_TABLE);
    }

    @Override
    public void giveReward(Player player, RewardData data) {
        ResourceLocation loottableId = new ResourceLocation(data.getReward().data);

        LootContext lootContext = new LootContext.Builder((ServerLevel) player.level)
            .withParameter(LootContextParams.THIS_ENTITY, player)
            .withParameter(LootContextParams.ORIGIN, player.position())
            .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
            .withParameter(LootContextParams.BLOCK_STATE, Blocks.AIR.defaultBlockState())
            .create(LootContextParamSets.BLOCK);
        ServerLevel serverWorld = lootContext.getLevel();
        LootTable lootTable = serverWorld.getServer()
            .getLootTables()
            .get(loottableId);

        List<ItemStack> drops = lootTable.getRandomItems(lootContext);

        if (drops.isEmpty()) {
            System.out.println(loottableId.toString() + " loot table dropped ZERO items!");
        }

        drops.forEach(x -> player.getInventory().placeItemBackInInventory(player.level, x));
    }

    @Override
    public MutableComponent getTranslatable(RewardData data) {
        return Component.literal(data.count + "x ").append(DivineMissions.ofTranslation("loot_table." + data.id))
            .withStyle(ChatFormatting.LIGHT_PURPLE);
    }
}

