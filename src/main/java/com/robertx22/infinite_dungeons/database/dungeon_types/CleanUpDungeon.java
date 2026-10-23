package com.robertx22.infinite_dungeons.database.dungeon_types;

import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import com.robertx22.infinite_dungeons.database.CompletitionScore;
import com.robertx22.infinite_dungeons.database.db_types.DungeonType;
import com.robertx22.infinite_dungeons.database.db_types.layout.config.CleanUpDungeonConfig;
import com.robertx22.infinite_dungeons.database.ids.DungeonTypeIds;
import com.robertx22.forge_compat.network.ClientboundTitleCompatPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class CleanUpDungeon extends DungeonType {

    public CleanUpDungeon() {
        super(DungeonTypeIds.CLEAN_UP);
    }

    @Override
    public void onControlBlockTick(ControlBlockEntity en) {

        CleanUpDungeonConfig config = en.data.getDungeonLayout().clean_up_config;

        if (en.data.mobs_spawned < config.kill_count_to_finish) {

            if (en.data.ticks % (config.modifier_every_x_seconds * 20) == 0) {
                en.data.tryAddRandomModifier(en);
            }

            if (en.data.ticks % 20 == 0) {
                if (en.getAllDungeonMobsAlive()
                    .size() < config.mobs_spawn_when_less_than_x_mobs_alive) {
                    int tospawn = config.mobs_per_spawn;
                    for (int i = 0; i < tospawn; i++) {
                        en.spawnMob();
                    }
                }
            }
        }

        if (en.data.ticks % 50 == 0) {
            en.sendTitleMessage(Component.literal(ChatFormatting.GREEN + "" + en.data.mobs_spawned + "/" + config.kill_count_to_finish), ClientboundTitleCompatPacket.Type.ACTIONBAR);

            if (en.data.mobs_spawned >= config.kill_count_to_finish && en.getAllDungeonMobsAlive()
                .isEmpty()) {
                CompletitionScore score = en.data.getDungeonLayout()
                    .getScore(en);
                en.spawnRewardsAndFinish(score);
            }

            if (en.data.ticks > (config.fail_if_goes_longer_than_x_seconds * 20)) {
                en.spawnRewardsAndFinish(CompletitionScore.FAIL);
            }
        }

    }

}


