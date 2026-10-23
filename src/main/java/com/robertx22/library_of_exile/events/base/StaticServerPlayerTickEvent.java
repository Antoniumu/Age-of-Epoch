package com.robertx22.library_of_exile.events.base;

import com.robertx22.library_of_exile.utils.EntityUtils;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import com.robertx22.forge_compat.event.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Watchdog для «безопасных» телепортов.
 *
 * <p>Раньше класс был помечен {@code @Mod.EventBusSubscriber} и подписывался на тик
 * экземплярным методом. Авто-регистрация подписчиков в этом порте ищет только
 * статические методы с одним аргументом-событием, поэтому обработчик не вызывался
 * вообще. А {@code TeleportUtils.makeSureTeleport} при каждом телепорте ставит игроку
 * {@code setPermanentlyInvulnerable(true)} и пишет этот флаг в NBT игрока. Так как
 * обработчик не тикал, флаг никогда не снимался — игрок навсегда оставался
 * неуязвимым и невидимым для ИИ ({@code LivingEntity#canBeSeenAsEnemy()} проверяет
 * {@code isInvulnerable()}).</p>
 *
 * <p>Теперь обработчик подписывается явно из {@code ForgeEvents.register()}, а
 * {@code removeInvlunTicks} действительно уменьшается. Дополнительно при первом тике
 * игрока снимается «залипший» флаг из старых сохранений.</p>
 */
public class StaticServerPlayerTickEvent {

    /**
     * Сколько тиков игрок остаётся неуязвимым после завершения телепорта.
     * Раньше здесь было 200 тиков (10 с), но флаг всё равно не снимался из-за
     * отсутствия декремента — теперь защита короткая и гарантированно истекает.
     */
    private static final int POST_TELEPORT_GRACE_TICKS = 60;

    public static final Map<UUID, PlayerTickData> PlayerTickDatas = new HashMap<>();

    /**
     * Единственная точка входа. Вызывается из {@code ForgeEvents.register()} на
     * {@link TickEvent.ServerTickEvent} (фаза END), поэтому событие приходит один раз
     * за серверный тик.
     */
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.server;
        if (server == null || server.getPlayerList() == null) {
            return;
        }

        Set<UUID> online = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());

            PlayerTickData data = PlayerTickDatas.get(player.getUUID());
            if (data == null) {
                data = new PlayerTickData();
                PlayerTickDatas.put(player.getUUID(), data);
                cleanLingeringInvulnerability(player);
            }

            try {
                data.tick(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // не держим в памяти вышедших игроков
        PlayerTickDatas.keySet().removeIf(uuid -> !online.contains(uuid));
    }

    /**
     * Лечит сохранения, в которых остался флаг {@code Invulnerable} после старого бага.
     * Ванильные мобы/кристаллы этот флаг используют, но у игрока его ставит только
     * наш телепорт, поэтому для игрока снятие безопасно.
     */
    private static void cleanLingeringInvulnerability(ServerPlayer player) {
        try {
            if (player.isPermanentlyInvulnerable() && !player.isCreative() && !player.isSpectator()) {
                player.setPermanentlyInvulnerable(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Consumer<EnsureTeleportData> MAKE_SURE_TELEPORT = x -> {

        if (x.player.isDeadOrDying()) {
            x.player.setPermanentlyInvulnerable(false);
            x.cancel();
            return;
        }

        x.player.setPermanentlyInvulnerable(true);

        if (x.player.blockPosition()
            .distSqr(x.whereShouldTeleport) > 1000) {

            if (x.tries > 3) {
                // Все попытки провалились — возвращаем игрока к точке возрождения
                // (и в то измерение, где она находится), чтобы он не застрял.
                ServerPlayer.RespawnConfig respawnConfig = x.player.getRespawnConfig();
                if (respawnConfig != null) {
                    net.minecraft.world.level.storage.LevelData.RespawnData respawnData = respawnConfig.respawnData();
                    TeleportUtils.teleport(x.player, respawnData.pos(), respawnData.dimension().identifier(), false);
                } else {
                    EntityUtils.setLoc(x.player, x.player.blockPosition());
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

        PlayerTickData data = PlayerTickDatas.get(player.getUUID());
        if (data == null) {
            data = new PlayerTickData();
            PlayerTickDatas.put(player.getUUID(), data);
        }
        player.setPermanentlyInvulnerable(true);

        EnsureTeleportData previous = data.ensureTeleportData;
        EnsureTeleportData next = new EnsureTeleportData(player, MAKE_SURE_TELEPORT, 50, teleportPos, dim);

        // Повторная попытка вотчдога создаёт новый EnsureTeleportData; чтобы
        // ограничение «3 попытки» вообще работало, переносим счётчик.
        if (previous != null && previous.tries > 0 && previous.whereShouldTeleport.equals(teleportPos)) {
            next.tries = previous.tries;
        }

        data.ensureTeleportData = next;
    }

    public static class PlayerTickData {
        public EnsureTeleportData ensureTeleportData;
        private int removeInvlunTicks = 0;

        public void tick(ServerPlayer player) {
            if (ensureTeleportData != null) {
                // пока телепорт в процессе, флагом управляет watchdog, а не таймер
                removeInvlunTicks = 0;
                ensureTeleportData.ticksLeft--;
                ensureTeleportData.ticks++;

                if (ensureTeleportData.ticks >= 3) {
                    ensureTeleportData.action.accept(ensureTeleportData);

                    if (ensureTeleportData.ticksLeft < 1) {
                        // cancel() выставляет ticksLeft = -1: телепорт не удался
                        // (смерть или слишком много попыток) — снимаем защиту сразу
                        boolean cancelled = ensureTeleportData.ticksLeft < 0;
                        ensureTeleportData = null;

                        if (cancelled || player.isDeadOrDying()) {
                            removeInvlunTicks = 0;
                            player.setPermanentlyInvulnerable(false);
                        } else {
                            // короткая защита после прибытия, гарантированно истекает
                            removeInvlunTicks = Math.max(removeInvlunTicks, POST_TELEPORT_GRACE_TICKS);
                        }
                    }
                }
            }

            if (removeInvlunTicks > 0) {
                removeInvlunTicks--;
                if (removeInvlunTicks == 0) {
                    player.setPermanentlyInvulnerable(false);
                }
            }
        }
    }

}
