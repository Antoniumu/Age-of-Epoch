package com.robertx22.age_of_exile.database.data.spells.entities.renders;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public class MySpriteRenderer<T extends Entity & IMyRenderAsItem> extends EntityRenderer<T, EntityRenderState> {
    private final float scale;
    private final boolean lit;

    public MySpriteRenderer(EntityRendererProvider.Context context, Object itemRenderer, float scale, boolean lit) {
        super(context);
        this.scale = scale;
        this.lit = lit;
    }

    public MySpriteRenderer(EntityRendererProvider.Context context, Object itemRenderer) {
        this(context, itemRenderer, 1.0F, false);
    }

    public MySpriteRenderer(EntityRendererProvider.Context context) {
        this(context, null, 1.0F, false);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    protected int getBlockLightLevel(T entity, BlockPos blockPos) {
        return this.lit ? 15 : super.getBlockLightLevel(entity, blockPos);
    }
}
