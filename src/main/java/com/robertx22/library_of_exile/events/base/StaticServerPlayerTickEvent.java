package com.robertx22.library_of_exile.events.base;

import com.robertx22.library_of_exile.utils.EntityUtils;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.forge_compat.eventbus.api.SubscribeEvent;
import com.robertx22.forge_compat.fml.common.Mod;
import com.robertx22.forge_compat.fml.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = "mmorpg")
public class StaticServerPlayerTickEvent {

    public static HashMap<UUID, PlayerTickData> PlayerTickDatas = new HashMap<UUID, PlayerTickData>();

    @SubscribeEvent
    public void onEndTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        for (ServerPlayer player : server.getPlayerList()
            .getPlayers()) {

            try {

                PlayerTickData data = PlayerTickDatas.get(player.getUUID());

                if (data == null) {
                    data = new PlayerTickData();
                }

                if (data.ensureTeleportData == null) {
                    if (data.removeInvlunTicks > 0) {
                        player.setPermanentlyInvulnerable(false);
                    }
                } else {

                    data.tick();

                    if (data.ensureTeleportData.ticks > 3) {
                        data.removeInvlunTicks = 200;
                        data.ensureTeleportData.action.accept(data.ensureTeleportData);
                        if (data.ensureTeleportData.ticksLeft < 1) {
                            data.ensureTeleportData = null;
                        }
                    }
                }

                PlayerTickDatas.put(player.getUUID(), data);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public static Consumer<EnsureTeleportData> MAKE_SURE_TELEPORT = x -> {

        if (x.player.isDeadOrDying()) {
            x.cancel();
            return;
        }

        x.player.setPermanentlyInvulnerable(true);

        if (x.player.blockPosition()
            .distSqr(x.whereShouldTeleport) > 1000) {

            if (x.tries > 3) {
                ServerPlayer.RespawnConfig respawnConfig = x.player.getRespawnConfig();
                BlockPos spawnpos = respawnConfig != null ? respawnConfig.respawnData().pos() : null;
                if (spawnpos != null) {
                    EntityUtils.setLoc(x.player, spawnpos);
                }
                x.cancel(); // pray that works at least
                return;
            } else {

                x.resetTicks();

                x.tries++;

                x.player.sendSystemMessage(Component.literal("Произошла ошибка телепортации, вы были возвращены обратно."));

                TeleportUtils.teleport(x.player, x.whereShouldTeleport);
            }
        }

    };

    public static void makeSureTeleport(ServerPlayer player, BlockPos teleportPos, Identifier dim) {

        if (!PlayerTickDatas.containsKey(player.getUUID())) {
            PlayerTickDatas.put(player.getUUID(), new PlayerTickData());
        }
        player.setPermanentlyInvulnerable(true);
        PlayerTickDatas.get(player.getUUID()).ensureTeleportData =
            new EnsureTeleportData(player, MAKE_SURE_TELEPORT, 50, teleportPos, dim);
    }

    public static class PlayerTickData {
        public EnsureTeleportData ensureTeleportData;
        int removeInvlunTicks = 0;

        public void tick() {
            if (ensureTeleportData != null) {
                ensureTeleportData.ticksLeft--;
                ensureTeleportData.ticks++;
            }
        }
    }

}
