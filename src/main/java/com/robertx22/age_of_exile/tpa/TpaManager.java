package com.robertx22.age_of_exile.tpa;

import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-side request handling and teleport execution for the TPA addon. */
public final class TpaManager {

    public static final long REQUEST_TIMEOUT_MILLIS = 60_000L;

    /** One outgoing request per sender; a sender can still have many incoming requests. */
    private static final Map<UUID, PendingRequest> REQUESTS_BY_SENDER = new HashMap<>();

    private static long nextRequestId = 1L;
    private static int ticksUntilExpiryCheck = 20;

    private TpaManager() {
    }

    public static boolean sendRequest(ServerPlayer sender, ServerPlayer target) {
        MinecraftServer server = serverOf(sender);
        if (server == null) {
            return false;
        }
        if (sender.getUUID().equals(target.getUUID())) {
            send(sender, "Нельзя отправить запрос самому себе.", ChatFormatting.RED);
            return false;
        }

        long now = System.currentTimeMillis();
        purgeExpired(server, now, true);
        if (!hasCharge(sender, now)) {
            return false;
        }

        // Sending to somebody else replaces the old outgoing request.
        removePreviousOutgoing(server, sender, target.getUUID());

        if (data(target).trusts(sender.getUUID())) {
            return performPlayerTeleport(sender, target, true, now);
        }

        PendingRequest request = new PendingRequest(
            nextRequestId++, sender, target, now
        );
        REQUESTS_BY_SENDER.put(sender.getUUID(), request);

        send(sender, "Запрос отправлен игроку " + name(target) + ". Он действует 60 секунд.",
            ChatFormatting.GREEN);
        send(target, name(sender) + " просит телепортироваться к вам.", ChatFormatting.YELLOW);
        send(target, "Принять: /tpaccept " + name(sender) + " — отклонить: /tpdeny " + name(sender),
            ChatFormatting.AQUA);
        return true;
    }

    public static boolean teleportToCoordinates(ServerPlayer player, Vec3 coordinates) {
        if (!Double.isFinite(coordinates.x) || !Double.isFinite(coordinates.y)
            || !Double.isFinite(coordinates.z)) {
            send(player, "Координаты должны быть конечными числами.", ChatFormatting.RED);
            return false;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockPos blockPos = BlockPos.containing(coordinates.x, coordinates.y, coordinates.z);
        if (!level.getWorldBorder().isWithinBounds(blockPos)) {
            send(player, "Эти координаты находятся за мировой границей.", ChatFormatting.RED);
            return false;
        }
        if (player.isDeadOrDying()) {
            send(player, "Нельзя телепортироваться во время смерти.", ChatFormatting.RED);
            return false;
        }

        long now = System.currentTimeMillis();
        TpaData data = data(player);
        if (!hasCharge(player, data, now)) {
            return false;
        }

        if (!teleport(player, level, coordinates.x, coordinates.y, coordinates.z)) {
            return false;
        }

        // No request is charged up front: only a completed teleport spends a charge.
        data.consumeCharge(now);
        send(player, "Вы телепортированы на " + formatCoordinates(coordinates)
            + ". " + chargeStatus(data, now), ChatFormatting.GREEN);
        return true;
    }

    /** Accepts the selected request, or the newest incoming request when sender is null. */
    public static boolean accept(ServerPlayer target, ServerPlayer sender) {
        MinecraftServer server = serverOf(target);
        if (server == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        purgeExpired(server, now, true);
        PendingRequest request = findIncoming(target, sender, now);
        if (request == null) {
            send(target, sender == null
                ? "У вас нет активных запросов TPA."
                : "Нет активного запроса от игрока " + name(sender) + ".", ChatFormatting.RED);
            return false;
        }

        ServerPlayer requester = server.getPlayerList().getPlayer(request.requesterId);
        if (requester == null) {
            REQUESTS_BY_SENDER.remove(request.requesterId, request);
            send(target, "Игрок " + request.requesterName + " уже вышел из игры.", ChatFormatting.RED);
            return false;
        }

        if (!performPlayerTeleport(requester, target, false, now)) {
            return false;
        }

        REQUESTS_BY_SENDER.remove(request.requesterId, request);
        return true;
    }

    /** Rejects the selected request, or the newest incoming request when sender is null. */
    public static boolean deny(ServerPlayer target, ServerPlayer sender) {
        MinecraftServer server = serverOf(target);
        if (server == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        purgeExpired(server, now, true);
        PendingRequest request = findIncoming(target, sender, now);
        if (request == null) {
            send(target, sender == null
                ? "У вас нет активных запросов TPA."
                : "Нет активного запроса от игрока " + name(sender) + ".", ChatFormatting.RED);
            return false;
        }

        REQUESTS_BY_SENDER.remove(request.requesterId, request);
        send(target, "Запрос от игрока " + request.requesterName + " отклонён.", ChatFormatting.YELLOW);

        ServerPlayer requester = server.getPlayerList().getPlayer(request.requesterId);
        if (requester != null) {
            send(requester, name(target) + " отклонил ваш запрос TPA.", ChatFormatting.RED);
        }
        return true;
    }

    public static boolean cancel(ServerPlayer sender) {
        MinecraftServer server = serverOf(sender);
        PendingRequest request = REQUESTS_BY_SENDER.remove(sender.getUUID());
        if (request == null) {
            send(sender, "У вас нет исходящего запроса TPA.", ChatFormatting.RED);
            return false;
        }

        send(sender, "Исходящий запрос игроку " + request.targetName + " отменён.", ChatFormatting.YELLOW);
        if (server != null) {
            ServerPlayer target = server.getPlayerList().getPlayer(request.targetId);
            if (target != null) {
                send(target, name(sender) + " отменил свой запрос TPA.", ChatFormatting.YELLOW);
            }
        }
        return true;
    }

    public static boolean trust(ServerPlayer player, ServerPlayer trusted) {
        if (player.getUUID().equals(trusted.getUUID())) {
            send(player, "Не нужно добавлять самого себя в доверенные.", ChatFormatting.RED);
            return false;
        }

        data(player).trust(trusted.getUUID(), name(trusted));
        send(player, name(trusted) + " добавлен в доверенные: его запросы TPA принимаются автоматически.",
            ChatFormatting.GREEN);
        return true;
    }

    public static boolean untrust(ServerPlayer player, ServerPlayer trusted) {
        if (!data(player).untrust(trusted.getUUID())) {
            send(player, name(trusted) + " не был в вашем списке доверенных.", ChatFormatting.RED);
            return false;
        }

        send(player, name(trusted) + " удалён из доверенных. Автопринятие отключено.",
            ChatFormatting.YELLOW);
        return true;
    }

    public static boolean showTrusted(ServerPlayer player) {
        List<Map.Entry<UUID, String>> trusted = new ArrayList<>(data(player).getTrustedPlayers().entrySet());
        if (trusted.isEmpty()) {
            send(player, "Список доверенных пуст. Добавить: /tpa trust <игрок>.", ChatFormatting.GRAY);
            return true;
        }

        trusted.sort(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER));
        send(player, "Доверенные игроки (" + trusted.size() + "):", ChatFormatting.GOLD);
        for (Map.Entry<UUID, String> entry : trusted) {
            send(player, "• " + entry.getValue(), ChatFormatting.AQUA);
        }
        return true;
    }

    public static boolean showCharges(ServerPlayer player) {
        send(player, chargeStatus(data(player), System.currentTimeMillis()), ChatFormatting.AQUA);
        return true;
    }

    public static boolean showHelp(ServerPlayer player) {
        showCharges(player);
        send(player, "/tpa <игрок> — запрос телепортации к игроку", ChatFormatting.GRAY);
        send(player, "/tpa <x> <y> <z> — телепортация на координаты", ChatFormatting.GRAY);
        send(player, "/tpaccept [игрок] и /tpdeny [игрок] — принять или отклонить", ChatFormatting.GRAY);
        send(player, "/tpa trust <игрок> — включить автопринятие; /tpa untrust <игрок> — выключить", ChatFormatting.GRAY);
        send(player, "/tpa trusted — список; /tpacancel — отменить свой запрос", ChatFormatting.GRAY);
        // Без прав оператора ванильный /tp сам ведёт в /tpa — со всеми теми же лимитами.
        send(player, "/tp <игрок> и /tp <x> <y> <z> без прав оператора работают как /tpa", ChatFormatting.GRAY);
        return true;
    }

    public static void onServerTick(MinecraftServer server) {
        if (server == null || --ticksUntilExpiryCheck > 0) {
            return;
        }
        ticksUntilExpiryCheck = 20;
        purgeExpired(server, System.currentTimeMillis(), true);
    }

    public static void onLogout(ServerPlayer player) {
        MinecraftServer server = serverOf(player);
        if (server == null) {
            return;
        }

        Iterator<PendingRequest> iterator = REQUESTS_BY_SENDER.values().iterator();
        while (iterator.hasNext()) {
            PendingRequest request = iterator.next();
            if (!request.requesterId.equals(player.getUUID()) && !request.targetId.equals(player.getUUID())) {
                continue;
            }

            iterator.remove();
            UUID otherId = request.requesterId.equals(player.getUUID()) ? request.targetId : request.requesterId;
            ServerPlayer other = server.getPlayerList().getPlayer(otherId);
            if (other != null && !other.getUUID().equals(player.getUUID())) {
                send(other, "Запрос TPA отменён: второй игрок вышел из игры.", ChatFormatting.YELLOW);
            }
        }
    }

    public static void onServerStarted() {
        REQUESTS_BY_SENDER.clear();
        ticksUntilExpiryCheck = 20;
    }

    public static void onServerStopped() {
        REQUESTS_BY_SENDER.clear();
        ticksUntilExpiryCheck = 20;
    }

    private static boolean performPlayerTeleport(ServerPlayer requester, ServerPlayer target,
                                                  boolean autoAccepted, long now) {
        TpaData data = data(requester);
        if (!hasCharge(requester, data, now)) {
            if (!autoAccepted) {
                send(target, "У игрока " + name(requester) + " не осталось зарядов TPA.", ChatFormatting.RED);
            }
            return false;
        }
        if (requester.isDeadOrDying() || target.isDeadOrDying()) {
            send(requester, "Телепортация невозможна, пока один из игроков мёртв.", ChatFormatting.RED);
            if (!autoAccepted) {
                send(target, "Телепортация невозможна, пока один из игроков мёртв.", ChatFormatting.RED);
            }
            return false;
        }

        ServerLevel destination = (ServerLevel) target.level();
        if (!teleport(requester, destination, target.getX(), target.getY(), target.getZ())) {
            return false;
        }

        data.consumeCharge(now);
        send(requester, "Вы телепортированы к игроку " + name(target)
            + (autoAccepted ? " (автопринятие)." : ".") + " " + chargeStatus(data, now), ChatFormatting.GREEN);
        send(target, autoAccepted
            ? "Запрос от " + name(requester) + " принят автоматически: игрок телепортирован к вам."
            : "Запрос принят: " + name(requester) + " телепортирован к вам.", ChatFormatting.GREEN);
        return true;
    }

    private static boolean teleport(ServerPlayer player, ServerLevel destination,
                                    double x, double y, double z) {
        try {
            player.teleportTo(destination, x, y, z, Set.of(), player.getYRot(), player.getXRot(), false);
            return true;
        } catch (Exception e) {
            send(player, "Не удалось выполнить телепортацию. Заряд не потрачен.", ChatFormatting.RED);
            e.printStackTrace();
            return false;
        }
    }

    private static PendingRequest findIncoming(ServerPlayer target, ServerPlayer requestedSender, long now) {
        if (requestedSender != null) {
            PendingRequest request = REQUESTS_BY_SENDER.get(requestedSender.getUUID());
            return request != null && request.targetId.equals(target.getUUID()) && !request.isExpired(now)
                ? request
                : null;
        }

        PendingRequest newest = null;
        for (PendingRequest request : REQUESTS_BY_SENDER.values()) {
            if (request.targetId.equals(target.getUUID()) && !request.isExpired(now)
                && (newest == null || request.id > newest.id)) {
                newest = request;
            }
        }
        return newest;
    }

    private static void removePreviousOutgoing(MinecraftServer server, ServerPlayer sender, UUID newTargetId) {
        PendingRequest previous = REQUESTS_BY_SENDER.remove(sender.getUUID());
        if (previous == null || previous.targetId.equals(newTargetId)) {
            return;
        }

        ServerPlayer oldTarget = server.getPlayerList().getPlayer(previous.targetId);
        if (oldTarget != null) {
            send(oldTarget, name(sender) + " отменил свой предыдущий запрос TPA.", ChatFormatting.YELLOW);
        }
    }

    private static void purgeExpired(MinecraftServer server, long now, boolean notify) {
        Iterator<PendingRequest> iterator = REQUESTS_BY_SENDER.values().iterator();
        while (iterator.hasNext()) {
            PendingRequest request = iterator.next();
            if (!request.isExpired(now)) {
                continue;
            }
            iterator.remove();

            if (notify) {
                ServerPlayer requester = server.getPlayerList().getPlayer(request.requesterId);
                ServerPlayer target = server.getPlayerList().getPlayer(request.targetId);
                send(requester, "Ваш запрос игроку " + request.targetName + " истёк.", ChatFormatting.GRAY);
                send(target, "Запрос от игрока " + request.requesterName + " истёк.", ChatFormatting.GRAY);
            }
        }
    }

    private static boolean hasCharge(ServerPlayer player, long now) {
        return hasCharge(player, data(player), now);
    }

    private static boolean hasCharge(ServerPlayer player, TpaData data, long now) {
        if (data.getCharges(now) > 0) {
            return true;
        }
        send(player, "Заряды TPA закончились. Следующий через "
            + formatDuration(data.getNextRechargeDelayMillis(now)) + ".", ChatFormatting.RED);
        return false;
    }

    private static String chargeStatus(TpaData data, long now) {
        int charges = data.getCharges(now);
        String status = "Заряды TPA: " + charges + "/" + TpaData.MAX_CHARGES + ".";
        if (charges < TpaData.MAX_CHARGES) {
            status += " Следующий через " + formatDuration(data.getNextRechargeDelayMillis(now)) + ".";
        }
        return status;
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    private static String formatCoordinates(Vec3 coordinates) {
        return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", coordinates.x, coordinates.y, coordinates.z);
    }

    private static TpaData data(ServerPlayer player) {
        return Load.playerRPGData(player).tpa;
    }

    private static MinecraftServer serverOf(ServerPlayer player) {
        return player.level().getServer();
    }

    private static String name(ServerPlayer player) {
        return player.getGameProfile().name();
    }

    /** Сообщение в стиле аддона; используется и мостом ванильного /tp в /tpa. */
    public static void send(ServerPlayer player, String text, ChatFormatting color) {
        if (player == null) {
            return;
        }
        player.sendSystemMessage(Component.literal("[TPA] ")
            .withStyle(ChatFormatting.GOLD)
            .append(Component.literal(text).withStyle(color)));
    }

    private static final class PendingRequest {
        private final long id;
        private final UUID requesterId;
        private final UUID targetId;
        private final String requesterName;
        private final String targetName;
        private final long createdAt;

        private PendingRequest(long id, ServerPlayer requester, ServerPlayer target, long createdAt) {
            this.id = id;
            this.requesterId = requester.getUUID();
            this.targetId = target.getUUID();
            this.requesterName = name(requester);
            this.targetName = name(target);
            this.createdAt = createdAt;
        }

        private boolean isExpired(long now) {
            return now - createdAt >= REQUEST_TIMEOUT_MILLIS;
        }
    }
}
