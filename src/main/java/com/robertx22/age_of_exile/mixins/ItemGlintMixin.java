package com.robertx22.age_of_exile.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.config.forge.ClientConfigs;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.forge_compat.client.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class ItemGlintMixin {

    private void drawMyGlint(PoseStack matrices, Slot slot, CallbackInfo ci) {

        try {
            if (ClientConfigs.getConfig().RENDER_ITEM_RARITY_BACKGROUND.get()) {
                ItemStack stack = slot.getItem();

                GearRarity rar = null;

                if (Gear.has(stack)) {
                    GearItemData gear = Gear.Load(stack);
                    rar = gear.getRarity();
                }

                if (StackSaving.STAT_SOULS.has(stack)) {
                    try {
                        rar = ExileDB.GearRarities()
                            .get(StackSaving.STAT_SOULS.loadFrom(stack).rar);
                    } catch (Exception e) {

                    }
                }

                if (rar == null) {
                    return;
                }

                RenderSystem.enableBlend();
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, ClientConfigs.getConfig().ITEM_RARITY_OPACITY.get()
                    .floatValue());

                Identifier tex = rar.getGlintTextureFull();

                if (ClientConfigs.getConfig().ITEM_RARITY_BACKGROUND_TYPE.get() == ClientConfigs.GlintType.BORDER) {
                    tex = rar.getGlintTextureBorder();
                }

                AbstractGui.bindTexture(tex);
                AbstractGui.blit(matrices, slot.x, slot.y, 0, 0, 16, 16, 16, 16);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1F);
                RenderSystem.disableBlend();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
