package com.robertx22.age_of_exile.guides;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class GuidesPacket extends MyPacket<GuidesPacket> {

    public String action = "";
    public String guideId = "";

    public GuidesPacket() {}

    public GuidesPacket(String action, String guideId) {
        this.action = action;
        this.guideId = guideId;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath("mmorpg", "guides_action");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        this.action = buf.readUtf();
        this.guideId = buf.readUtf();
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeUtf(this.action == null ? "" : this.action);
        buf.writeUtf(this.guideId == null ? "" : this.guideId);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (ctx.getPlayer() instanceof ServerPlayer sp) {
            if ("select".equals(action)) {
                GuideAdvisor.setActiveGuide(sp, guideId);
                sp.sendSystemMessage(Component.literal("Активный билд-гайд: " + (guideId.isBlank() ? "отключён" : guideId))
                    .withStyle(ChatFormatting.AQUA));
            } else if ("apply".equals(action)) {
                GuideAdvisor.setActiveGuide(sp, guideId);
                String msg = GuideAdvisor.autoApplyRecommended(sp);
                sp.sendSystemMessage(Component.literal("[Гайд] " + msg).withStyle(ChatFormatting.GREEN));
            }
        }
    }

    @Override
    public MyPacket<GuidesPacket> newInstance() {
        return new GuidesPacket();
    }
}
