package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.item.ItemStack;

@Mixin(Hud.class)
public class StopOffhandRenderMixin {

    @Redirect(
        method = "extractItemHotbar",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"),
        require = 0
    )
    private boolean hideOffhandIfConfigured(ItemStack stack, GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        try {
            var cfg = ClientConfigs.getConfig();
            if (cfg != null && !true) {
                var player = Minecraft.getInstance().player;
                if (player != null && stack == player.getOffhandItem()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return stack.isEmpty();
    }
}
