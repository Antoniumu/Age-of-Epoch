package com.robertx22.ancient_obelisks.main;

import com.robertx22.ancient_obelisks.configs.ObeliskConfig;
import com.robertx22.ancient_obelisks.database.holders.ObeliskRelicStats;
import com.robertx22.ancient_obelisks.structure.ObeliskMapData;
import com.robertx22.library_of_exile.components.LibMapCap;
import com.robertx22.library_of_exile.main.ApiForgeEvents;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ObeliskRewardLogic {

    public static int whileRoll(float chance) {
        int amount = 0;

        while (chance > 0) {

            float maxChance = 100;

            float currentChance = chance;

            if (currentChance > maxChance) {
                currentChance = maxChance;
            }

            chance -= currentChance;

            if (RandomUtils.roll(currentChance)) {
                amount++;
            }

        }
        return amount;

    }

    public static void spawnChests(ObeliskMapData d, Level world, BlockPos pos) {

        for (Player p : ObelisksMain.OBELISK_MAP_STRUCTURE.getAllPlayersInMap(world, pos)) {
            p.sendSystemMessage(ObeliskWords.OBELISK_END.get().withStyle(ChatFormatting.DARK_PURPLE));
        }

        int chests = whileRoll(d.getTotalRewardChance());

        if (chests > ObeliskConfig.get().MAX_CHEST_REWARDS.get()) {
            chests = ObeliskConfig.get().MAX_CHEST_REWARDS.get();
        }
        if (chests < 1) {
            chests = 1; // compensation prize lol
        }

        var data = LibMapCap.getData(world, pos);
        if (data != null) {
            if (RandomUtils.roll(data.relicStats.get(ObeliskRelicStats.INSTANCE.TRIPLE_CHEST_REWARD_CHANCE))) {
                chests *= 3;
            }
        }

        for (int i = 0; i < chests; i++) {
            var cpos = findNearbyFreeChestPos(world, pos);
            if (cpos != null) {
                spawnChest(world, cpos, ObeliskLootTables.LOOT);
            }
        }
    }

    public static void spawnChest(Level world, BlockPos pos, Identifier loottable) {

        world.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);

        if (world.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity ce) {
            ce.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, loottable), world.random.nextLong());
        }

    }

    public static BlockPos findNearbyFreeChestPos(Level world, BlockPos pos) {
        return findNearbyFreeChestPos(world, pos, state -> !state.isAir() && !state.is(Blocks.CHEST) && !state.is(ObeliskEntries.OBELISK_REWARD_BLOCK.get()), 1);
    }

    public static BlockPos findNearbyFreeChestPos(Level world, BlockPos pos, Predicate<BlockState> groundCheck, int freeBlocks) {
        BlockPos nearest = null;
        int rad = 5;
        for (int x = -rad; x < rad; x++) {
            for (int y = -2; y < 2; y++) {
                for (int z = -rad; z < rad; z++) {

                    var check = pos.offset(x, y, z);
                    var state = world.getBlockState(check);

                    if (groundCheck.test(state)) {
                        List<BlockPos> airChecks = new ArrayList<>();
                        for (int i = 0; i < freeBlocks; i++) {
                            airChecks.add(check.above());
                        }
                        if (airChecks.stream().allMatch(e -> world.getBlockState(e).isAir())) {
                            if (nearest == null || nearest.distSqr(pos) > check.above().distSqr(pos)) {
                                nearest = check.above();
                            }
                        }
                    }
                }
            }
        }
        return nearest;
    }

    public static void init() {

        ApiForgeEvents.registerForgeEvent(LivingDeathEvent.class, x -> {
            LivingEntity en = x.getEntity();
            var world = en.level();
            if (!world.isClientSide()) {
                ObelisksMain.ifMapData(world, en.blockPosition()).ifPresent(d -> {
                    d.mob_kills++;
                });
            }
        });

    }
}
