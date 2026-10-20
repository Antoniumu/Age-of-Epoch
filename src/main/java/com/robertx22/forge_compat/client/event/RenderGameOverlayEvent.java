package com.robertx22.forge_compat.client.event;

import com.robertx22.forge_compat.eventbus.api.Event;
import com.mojang.blaze3d.vertex.PoseStack;

public class RenderGameOverlayEvent extends Event {

    public enum ElementType { ALL, TEXT, HOTBAR, EXPERIENCE, HEALTH, ARMOR, FOOD, AIR }

    private final ElementType type;
    private final PoseStack matrixStack;
    private final float partialTicks;

    public RenderGameOverlayEvent(ElementType type, PoseStack matrixStack, float partialTicks) {
        this.type = type;
        this.matrixStack = matrixStack;
        this.partialTicks = partialTicks;
    }

    public ElementType getType() { return type; }

    public PoseStack getMatrixStack() { return matrixStack; }

    public PoseStack getPoseStack() { return matrixStack; }

    public float getPartialTicks() { return partialTicks; }
}
