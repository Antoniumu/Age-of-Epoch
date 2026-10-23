package com.robertx22.age_of_exile.questbook;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Small client-to-server action packet; all quest state is validated server-side. */
public class QuestBookPacket extends MyPacket<QuestBookPacket> {

    public enum Action {
        REFRESH,
        TOGGLE_PLATINUM,
        CLAIM_MANUAL_TASK
    }

    public Action action = Action.REFRESH;
    public String taskId = "";

    public QuestBookPacket() {
    }

    public QuestBookPacket(Action action, String taskId) {
        this.action = action;
        this.taskId = taskId == null ? "" : taskId;
    }

    public static QuestBookPacket refresh() {
        return new QuestBookPacket(Action.REFRESH, "");
    }

    public static QuestBookPacket togglePlatinum() {
        return new QuestBookPacket(Action.TOGGLE_PLATINUM, "");
    }

    public static QuestBookPacket claim(String taskId) {
        return new QuestBookPacket(Action.CLAIM_MANUAL_TASK, taskId);
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "questbook_action");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        action = buf.readEnum(Action.class);
        taskId = buf.readUtf(96);
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUtf(taskId == null ? "" : taskId, 96);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        switch (action) {
            case REFRESH -> QuestBookService.refresh(player);
            case TOGGLE_PLATINUM -> QuestBookService.togglePlatinum(player);
            case CLAIM_MANUAL_TASK -> QuestBookService.claimManualTask(player, taskId);
        }
    }

    @Override
    public MyPacket<QuestBookPacket> newInstance() {
        return new QuestBookPacket();
    }
}
