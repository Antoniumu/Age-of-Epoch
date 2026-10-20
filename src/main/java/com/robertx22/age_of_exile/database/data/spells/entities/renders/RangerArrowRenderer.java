package com.robertx22.age_of_exile.database.data.spells.entities.renders;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

public class RangerArrowRenderer<T extends AbstractArrow> extends EntityRenderer<T, EntityRenderState> {
    public static final Identifier RES_ARROW = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public RangerArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    public Identifier getTextureLocation(T en) {
        return RES_ARROW;
    }
}
