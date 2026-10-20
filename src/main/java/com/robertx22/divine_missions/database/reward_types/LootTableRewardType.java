package com.robertx22.divine_missions.database.reward_types;

import com.robertx22.divine_missions.database.RewardTypeIds;
import com.robertx22.divine_missions.database.db_types.RewardType;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.saving.RewardData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public class LootTableRewardType extends RewardType {

    public LootTableRewardType() {
        super(RewardTypeIds.LOOT_TABLE);
    }

    @Override
    public void giveReward(Player player, RewardData data) {
        Identifier loottableId = Identifier.parse(data.getReward().data);
        ServerLevel serverWorld = (ServerLevel) player.level();

        LootParams lootParams = new LootParams.Builder(serverWorld)
            .withParameter(LootContextParams.THIS_ENTITY, player)
            .withParameter(LootContextParams.ORIGIN, player.position())
            .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
            .withParameter(LootContextParams.BLOCK_STATE, Blocks.AIR.defaultBlockState())
            .create(LootContextParamSets.BLOCK);

        LootTable lootTable = serverWorld.getServer()
            .reloadableRegistries()
            .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, loottableId));

        List<ItemStack> drops = lootTable.getRandomItems(lootParams);

        if (drops.isEmpty()) {
            System.out.println(loottableId.toString() + " loot table dropped ZERO items!");
        }

        drops.forEach(x -> player.getInventory().placeItemBackInInventory(x));
    }

    @Override
    public MutableComponent getTranslatable(RewardData data) {
        return Component.literal(data.count + "x ").append(DivineMissions.ofTranslation("loot_table." + data.id))
            .withStyle(ChatFormatting.LIGHT_PURPLE);
    }
}
