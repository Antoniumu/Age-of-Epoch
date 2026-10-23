package com.robertx22.age_of_exile.guides;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Клиент -> сервер: выбрать гайд, снять выбор или собрать сборку на Полигоне.
 */
public class GuidesPacket extends MyPacket<GuidesPacket> {

    public enum Action {
        /** выбрать гайд (и получить подсказки) */
        SELECT,
        /** снять выбор гайда */
        CLEAR,
        /** собрать полную сборку по гайду (только Полигон) */
        APPLY_IN_SIMULATION
    }

    public Action action = Action.SELECT;
    public String guideId = "";
    public boolean gear = true;

    public GuidesPacket() {
    }

    public GuidesPacket(Action action, String guideId, boolean gear) {
        this.action = action;
        this.guideId = guideId;
        this.gear = gear;
    }

    public static GuidesPacket select(String guideId) {
        return new GuidesPacket(Action.SELECT, guideId, false);
    }

    public static GuidesPacket clear() {
        return new GuidesPacket(Action.CLEAR, "", false);
    }

    public static GuidesPacket applyInSimulation(String guideId, boolean gear) {
        return new GuidesPacket(Action.APPLY_IN_SIMULATION, guideId, gear);
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "guides_action");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.guideId = buf.readUtf(64);
        this.gear = buf.readBoolean();
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        buf.writeUtf(this.guideId == null ? "" : this.guideId, 64);
        buf.writeBoolean(this.gear);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        switch (this.action) {
            case SELECT -> GuideAdvisor.setActiveGuide(player, this.guideId);
            case CLEAR -> GuideAdvisor.setActiveGuide(player, "");
            case APPLY_IN_SIMULATION -> {
                GuideDefinition guide = GuideData.byId(this.guideId);
                if (guide == null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c[Гайд] Неизвестный гайд: " + this.guideId));
                    return;
                }
                if (!guide.id.equals(GuideAdvisor.activeGuideId(player))) {
                    GuideAdvisor.setActiveGuide(player, guide.id);
                }
                GuideAdvisor.applyInSimulation(player, guide, true, this.gear);
            }
        }
    }

    @Override
    public MyPacket<GuidesPacket> newInstance() {
        return new GuidesPacket();
    }
}
