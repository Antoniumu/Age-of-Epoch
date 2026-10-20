package com.robertx22.age_of_exile.database.data.spells.entities.renders;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.Projectile;

public class ModTridentRenderer extends EntityRenderer<Projectile, EntityRenderState> {
    public static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/trident.png");

    public ModTridentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    public Identifier getTextureLocation(Projectile tridentEntity) {
        return TEXTURE;
    }
}
